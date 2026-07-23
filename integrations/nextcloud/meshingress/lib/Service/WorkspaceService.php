<?php

declare(strict_types=1);

namespace OCA\Meshingress\Service;

use OCA\Meshingress\AppInfo\Application;
use OCA\Meshingress\BackgroundJob\WorkspaceImportJob;
use OCP\BackgroundJob\IJobList;
use OCP\Files\File;
use OCP\Files\Folder;
use OCP\Files\IRootFolder;
use OCP\Files\NotFoundException;
use OCP\Http\Client\IClientService;
use OCP\IDBConnection;
use OCP\IURLGenerator;
use Psr\Log\LoggerInterface;

/**
 * Owns one Nextcloud-side lifecycle:
 * OPEN -> SEALED -> QUEUED -> RUNNING -> COMPLETED|FAILED.
 * Native files and delegated URL records may only be added while OPEN.
 */
final class WorkspaceService {
    private const WORKSPACES = 'meshingress_workspaces';
    private const SOURCES = 'meshingress_workspace_sources';
    private const WORKSPACE_PARENT = 'Workspace';
    private const WORKSPACE_NAME = 'Meshingress';
    private const MAX_FILE_BYTES = 1073741824;
    private const MAX_SOURCES = 10000;
    private const MAX_ATTEMPTS = 3;
    private const LEASE_SECONDS = 900;
    private const MANIFEST_NAME = 'manifest.json';

    public function __construct(
        private readonly IDBConnection $database,
        private readonly IJobList $jobs,
        private readonly IRootFolder $rootFolder,
        private readonly IClientService $http,
        private readonly IURLGenerator $urlGenerator,
        private readonly LoggerInterface $logger,
    ) {
    }

    /** @return array<string, mixed> */
    public function reserve(string $uid, string $toolId, string $requestId, string $sessionId): array {
        $toolId = $this->toolId($toolId);
        $requestId = $this->identifier($requestId, 'requestId', true);
        $sessionId = $this->identifier($sessionId, 'sessionId');

        $existing = $this->workspaceByRequest($uid, $requestId);
        if ($existing !== null) {
            if ((string)$existing['tool_id'] !== $toolId) {
                throw new \RuntimeException('requestId is already owned by a different Meshingress tool.');
            }
            return $this->response($existing);
        }

        $this->rootStorage($uid, $toolId, $requestId);
        $now = time();
        try {
            $query = $this->database->getQueryBuilder();
            $query->insert(self::WORKSPACES)->values([
                'workspace_id' => $query->createNamedParameter($requestId),
                'uid' => $query->createNamedParameter($uid),
                'tool_id' => $query->createNamedParameter($toolId),
                'session_id' => $query->createNamedParameter($sessionId === '' ? null : $sessionId),
                'request_id' => $query->createNamedParameter($requestId),
                'root_path' => $query->createNamedParameter($requestId),
                'state' => $query->createNamedParameter('OPEN'),
                'attempts' => $query->createNamedParameter(0),
                'created_at' => $query->createNamedParameter($now),
                'updated_at' => $query->createNamedParameter($now),
            ])->executeStatement();
        } catch (\Throwable $exception) {
            // Reservation is idempotent across an interrupted client retry.
            $existing = $this->workspaceByRequest($uid, $requestId);
            if ($existing !== null && (string)$existing['tool_id'] === $toolId) {
                return $this->response($existing);
            }
            throw new \RuntimeException('The delegated workspace could not be reserved.');
        }
        return $this->response($this->requireWorkspace($uid, $toolId, $requestId));
    }

    /** @return array<string, mixed> */
    public function uploadNativeFile(string $uid, string $toolId, string $workspaceId, string $path, string $content, string $contentType): array {
        $workspace = $this->openWorkspace($uid, $toolId, $workspaceId);
        $path = $this->filePath($path, 'path');
        $this->assertWritablePath($workspace, $path);
        if (strlen($content) > self::MAX_FILE_BYTES) {
            throw new \InvalidArgumentException('The native file exceeds the one-gigabyte limit.');
        }

        $root = $this->rootStorage($uid, (string)$workspace['tool_id'], (string)$workspace['root_path']);
        $folder = $this->parentFolder($root, $path);
        $name = basename($path);
        if ($folder->nodeExists($name)) {
            throw new \RuntimeException('The requested destination already exists: ' . $path);
        }
        $file = $folder->newFile($name);
        $file->putContent($content);
        return [
            'path' => $path,
            'byteSize' => strlen($content),
            'checksumSha256' => hash('sha256', $content),
            'mimeType' => $this->mimeType($contentType),
        ];
    }

    /** @param mixed $sources @return array<string, mixed> */
    public function appendSources(string $uid, string $toolId, string $workspaceId, mixed $sources): array {
        $workspace = $this->openWorkspace($uid, $toolId, $workspaceId);
        if (is_string($sources)) {
            $sources = json_decode($sources, true, 512, JSON_THROW_ON_ERROR);
        }
        if (!is_array($sources) || $sources === []) {
            throw new \InvalidArgumentException('sources must be a non-empty JSON array.');
        }
        if ($this->sourceCount((string)$workspace['workspace_id']) + count($sources) > self::MAX_SOURCES) {
            throw new \InvalidArgumentException('sources exceed the maximum of ' . self::MAX_SOURCES . '.');
        }

        $root = $this->rootStorage($uid, (string)$workspace['tool_id'], (string)$workspace['root_path']);
        $position = $this->sourceCount((string)$workspace['workspace_id']);
        $prepared = [];
        foreach ($sources as $index => $source) {
            if (!is_array($source)) {
                throw new \InvalidArgumentException('sources[' . $index . '] must be an object.');
            }
            $path = $this->filePath((string)($source['path'] ?? ''), 'sources[' . $index . '].path');
            $this->assertWritablePath($workspace, $path);
            if (isset($prepared[$path]) || $this->sourceExists((string)$workspace['workspace_id'], $path) || $this->fileAt($root, $path) !== null) {
                throw new \RuntimeException('The requested destination already exists: ' . $path);
            }
            $prepared[$path] = [
                'source_id' => 'src_' . bin2hex(random_bytes(16)),
                'url' => $this->sourceUrl((string)($source['url'] ?? '')),
                'content_type' => $this->optionalMimeType($source['contentType'] ?? null, 'sources[' . $index . '].contentType'),
                'expected_byte_size' => $this->expectedByteSize($source['expectedByteSize'] ?? null, 'sources[' . $index . '].expectedByteSize'),
                'expected_sha256' => $this->expectedSha256($source['expectedSha256'] ?? null, 'sources[' . $index . '].expectedSha256'),
                'position' => $position++,
            ];
        }

        $this->database->beginTransaction();
        try {
            foreach ($prepared as $path => $source) {
                $query = $this->database->getQueryBuilder();
                $query->insert(self::SOURCES)->values([
                    'workspace_id' => $query->createNamedParameter($workspace['workspace_id']),
                    'source_id' => $query->createNamedParameter($source['source_id']),
                    'source_url' => $query->createNamedParameter($source['url']),
                    'relative_path' => $query->createNamedParameter($path),
                    'content_type' => $query->createNamedParameter($source['content_type']),
                    'expected_byte_size' => $query->createNamedParameter($source['expected_byte_size']),
                    'expected_sha256' => $query->createNamedParameter($source['expected_sha256']),
                    'state' => $query->createNamedParameter('PENDING'),
                    'attempts' => $query->createNamedParameter(0),
                    'position' => $query->createNamedParameter($source['position']),
                ])->executeStatement();
            }
            $this->database->commit();
        } catch (\Throwable $exception) {
            $this->database->rollBack();
            throw new \RuntimeException('The delegated source batch could not be recorded.');
        }
        return [
            'workspaceId' => $workspace['workspace_id'],
            'state' => 'OPEN',
            'sourceCount' => $position,
            'sources' => array_map(
                static fn(string $path, array $source): array => [
                    'sourceId' => $source['source_id'],
                    'path' => $path,
                    'state' => 'PENDING',
                ],
                array_keys($prepared),
                array_values($prepared),
            ),
        ];
    }

    /** @return array<string, mixed> */
    public function seal(string $uid, string $toolId, string $workspaceId): array {
        $workspace = $this->openWorkspace($uid, $toolId, $workspaceId);
        if ($this->sourceCount((string)$workspace['workspace_id']) === 0) {
            throw new \InvalidArgumentException('At least one delegated source is required before sealing.');
        }

        $jobId = 'dws_' . bin2hex(random_bytes(16));
        $query = $this->database->getQueryBuilder();
        $changed = $query->update(self::WORKSPACES)
            ->set('state', $query->createNamedParameter('SEALED'))
            ->set('job_id', $query->createNamedParameter($jobId))
            ->set('updated_at', $query->createNamedParameter(time()))
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspace['workspace_id'])))
            ->andWhere($query->expr()->eq('state', $query->createNamedParameter('OPEN')))
            ->executeStatement();
        if ($changed !== 1) {
            throw new \RuntimeException('The delegated workspace is sealed and cannot be modified.');
        }

        // SEALED is durable before a wake-up is attempted. The dedicated worker
        // scans SEALED records too, so a scheduler failure cannot orphan it.
        $this->schedule((string)$workspace['workspace_id']);
        $this->updateWorkspace((string)$workspace['workspace_id'], ['state' => 'QUEUED', 'error_message' => null]);
        return $this->status($uid, $toolId, $workspaceId);
    }

    /** @return array<string, mixed> */
    public function status(string $uid, string $toolId, string $workspaceId): array {
        return $this->response($this->requireWorkspace($uid, $this->toolId($toolId), $workspaceId));
    }

    /** @return array<string, mixed> */
    public function statusByRequest(string $uid, string $requestId): array {
        $workspace = $this->workspaceByRequest($uid, $this->identifier($requestId, 'requestId', true));
        if ($workspace === null) {
            throw new \RuntimeException('The delegated workspace was not found.');
        }
        return $this->response($workspace);
    }

    /** Process a specific durable workspace. Safe when both a queued job and the dedicated worker wake simultaneously. */
    public function runWorkspace(string $workspaceId): void {
        $workspace = $this->workspaceById($workspaceId);
        if ($workspace === null || in_array((string)$workspace['state'], ['COMPLETED', 'FAILED'], true) || !$this->claim($workspaceId)) {
            return;
        }
        $workspace = $this->workspaceById($workspaceId);
        if ($workspace === null) {
            return;
        }
        try {
            $root = $this->rootStorage((string)$workspace['uid'], (string)$workspace['tool_id'], (string)$workspace['root_path']);
            foreach ($this->processableSources($workspaceId) as $source) {
                $this->runSource($workspace, $root, $source);
            }
            $sources = $this->sources($workspaceId);
            if ($this->hasSourceState($sources, 'FAILED')) {
                $this->finishFailed($workspaceId, $this->firstSourceError($sources));
                return;
            }
            if ($this->hasIncompleteSources($sources)) {
                $this->updateWorkspace($workspaceId, ['state' => 'QUEUED', 'lease_expires_at' => null]);
                $this->schedule($workspaceId);
                return;
            }

            $completedAt = time();
            $manifest = $this->buildManifest($workspace, $root, $completedAt);
            $encoded = json_encode($manifest, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT) . "\n";
            if ($root->nodeExists(self::MANIFEST_NAME)) {
                throw new \RuntimeException('The delegated workspace manifest already exists.');
            }
            $root->newFile(self::MANIFEST_NAME)->putContent($encoded);
            $this->updateWorkspace($workspaceId, [
                'state' => 'COMPLETED',
                'lease_expires_at' => null,
                'completed_at' => $completedAt,
                'manifest_json' => rtrim($encoded),
                'error_message' => null,
            ]);
        } catch (\Throwable $exception) {
            $this->handleWorkspaceFailure($workspaceId, $exception);
        }
    }

    /** Runs the oldest sealed/queued workspace, including a lease that was left behind by a stopped worker. */
    public function runNextWorkspace(): bool {
        $now = time();
        $query = $this->database->getQueryBuilder();
        $result = $query->select('workspace_id')->from(self::WORKSPACES)
            ->where($query->expr()->orX(
                $query->expr()->eq('state', $query->createNamedParameter('SEALED')),
                $query->expr()->eq('state', $query->createNamedParameter('QUEUED')),
                $query->expr()->andX(
                    $query->expr()->eq('state', $query->createNamedParameter('RUNNING')),
                    $query->expr()->lt('lease_expires_at', $query->createNamedParameter($now)),
                ),
            ))
            ->orderBy('created_at', 'ASC')
            ->setMaxResults(1)
            ->executeQuery();
        $workspaceId = $result->fetchOne();
        $result->closeCursor();
        if (!is_string($workspaceId) || $workspaceId === '') {
            return false;
        }
        $this->runWorkspace($workspaceId);
        return true;
    }

    /** @param array<string, mixed> $workspace @param array<string, mixed> $source */
    private function runSource(array $workspace, Folder $root, array $source): void {
        $attempt = ((int)$source['attempts']) + 1;
        $this->updateSource((string)$source['source_id'], ['state' => 'RUNNING', 'attempts' => $attempt, 'error_message' => null]);
        try {
            $existing = $this->fileAt($root, (string)$source['relative_path']);
            $metadata = $existing === null
                ? $this->downloadSource($root, $source)
                : $this->metadataFromFile($existing, $source);
            $this->validateExpected($source, $metadata);
            $this->updateSource((string)$source['source_id'], [
                'state' => 'COMPLETED',
                'file_id' => $metadata['fileId'],
                'file_size' => $metadata['byteSize'],
                'mime_type' => $metadata['mimeType'],
                'checksum_sha256' => $metadata['checksumSha256'],
                'completed_at' => time(),
                'error_message' => null,
            ]);
        } catch (\Throwable $exception) {
            $retry = $attempt < self::MAX_ATTEMPTS;
            $this->updateSource((string)$source['source_id'], [
                'state' => $retry ? 'QUEUED' : 'FAILED',
                'error_message' => $this->safeError($exception),
            ]);
            $this->logger->warning('Meshingress delegated source failed.', [
                'app' => Application::APP_ID,
                'workspaceId' => $workspace['workspace_id'],
                'sourceId' => $source['source_id'],
                'attempt' => $attempt,
                'retry' => $retry,
                'exception' => $exception,
            ]);
        }
    }

    /** @param array<string, mixed> $source @return array{fileId:int,byteSize:int,mimeType:string,checksumSha256:string} */
    private function downloadSource(Folder $root, array $source): array {
        $response = $this->http->newClient()->get((string)$source['source_url'], [
            'timeout' => 300,
            'connect_timeout' => 20,
            'allow_redirects' => ['max' => 3],
        ]);
        if ($response->getStatusCode() < 200 || $response->getStatusCode() >= 300) {
            throw new \RuntimeException('The source server rejected the download.');
        }
        $folder = $this->parentFolder($root, (string)$source['relative_path']);
        $name = basename((string)$source['relative_path']);
        $file = $folder->newFile($name);
        try {
            $metadata = $this->writeResponse($response, $file, $source['content_type'] === null ? null : (string)$source['content_type']);
            return ['fileId' => $file->getId()] + $metadata;
        } catch (\Throwable $exception) {
            try {
                $file->delete();
            } catch (\Throwable) {
            }
            throw $exception;
        }
    }

    /** @param array<string, mixed> $source @return array{fileId:int,byteSize:int,mimeType:string,checksumSha256:string} */
    private function metadataFromFile(File $file, array $source): array {
        return [
            'fileId' => $file->getId(),
            'byteSize' => $file->getSize(),
            'mimeType' => $source['content_type'] === null ? $file->getMimeType() : (string)$source['content_type'],
            'checksumSha256' => $this->fileSha256($file),
        ];
    }

    /** @return array{byteSize:int,mimeType:string,checksumSha256:string} */
    private function writeResponse(mixed $response, File $file, ?string $declaredMimeType): array {
        // Stage before putContent: File::fopen('w') can leave Nextcloud FileInfo at 0 bytes.
        $output = tmpfile();
        if ($output === false) {
            throw new \RuntimeException('Nextcloud could not create temporary storage for the imported file.');
        }
        $input = $response->getBody();
        $hash = hash_init('sha256');
        $size = 0;
        $complete = false;
        $write = static function (string $chunk) use ($output, $hash, &$size): void {
            if ($chunk === '') {
                return;
            }
            $size += strlen($chunk);
            if ($size > self::MAX_FILE_BYTES) {
                throw new \RuntimeException('The source exceeds the configured one-gigabyte import limit.');
            }
            if (fwrite($output, $chunk) === false) {
                throw new \RuntimeException('Nextcloud could not write the imported file.');
            }
            hash_update($hash, $chunk);
        };
        try {
            if (is_string($input)) {
                for ($offset = 0, $length = strlen($input); $offset < $length; $offset += 8192) {
                    $write(substr($input, $offset, 8192));
                }
            } else {
                while (!$input->eof()) {
                    $write($input->read(8192));
                }
            }
            $complete = true;
        } finally {
            if ($complete) {
                rewind($output);
                $file->putContent($output);
            }
            if (is_resource($output)) {
                fclose($output);
            }
        }
        return [
            'byteSize' => $size,
            'mimeType' => $declaredMimeType ?? $this->mimeType((string)$response->getHeader('Content-Type')),
            'checksumSha256' => hash_final($hash),
        ];
    }

    /** @param array<string, mixed> $source @param array{fileId:int,byteSize:int,mimeType:string,checksumSha256:string} $metadata */
    private function validateExpected(array $source, array $metadata): void {
        if ($source['expected_byte_size'] !== null && (int)$source['expected_byte_size'] !== $metadata['byteSize']) {
            throw new \RuntimeException('The downloaded source does not match expectedByteSize.');
        }
        if ($source['expected_sha256'] !== null && !hash_equals((string)$source['expected_sha256'], $metadata['checksumSha256'])) {
            throw new \RuntimeException('The downloaded source does not match expectedSha256.');
        }
    }

    /** @param array<string, mixed> $workspace @return array<string, mixed> */
    private function buildManifest(array $workspace, Folder $root, int $completedAt): array {
        $files = $this->collectFiles($root, '', (string)$workspace['uid'], $this->managedRootPath((string)$workspace['tool_id'], (string)$workspace['root_path']));
        usort($files, static fn(array $left, array $right): int => strcmp((string)$left['path'], (string)$right['path']));
        return [
            'type' => 'meshingress.tool-storage-manifest',
            'state' => 'COMPLETED',
            'workspaceId' => $workspace['workspace_id'],
            'jobId' => $workspace['job_id'],
            'sessionId' => $workspace['session_id'],
            'requestId' => $workspace['request_id'],
            'toolId' => $workspace['tool_id'],
            'filesUri' => $this->davUri((string)$workspace['uid'], $this->managedRootPath((string)$workspace['tool_id'], (string)$workspace['root_path']), true),
            'manifestUri' => $this->davUri((string)$workspace['uid'], $this->managedRootPath((string)$workspace['tool_id'], (string)$workspace['root_path']) . '/' . self::MANIFEST_NAME),
            'createdAt' => gmdate(DATE_ATOM, (int)$workspace['created_at']),
            'completedAt' => gmdate(DATE_ATOM, $completedAt),
            'files' => $files,
        ];
    }

    /** @return list<array<string, mixed>> */
    private function collectFiles(Folder $folder, string $prefix, string $uid, string $managedRoot): array {
        $files = [];
        foreach ($folder->getDirectoryListing() as $node) {
            $relative = $prefix === '' ? $node->getName() : $prefix . '/' . $node->getName();
            if ($node instanceof Folder) {
                array_push($files, ...$this->collectFiles($node, $relative, $uid, $managedRoot));
            } elseif ($node instanceof File && $relative !== self::MANIFEST_NAME) {
                $files[] = [
                    'path' => $relative,
                    'mimeType' => $node->getMimeType(),
                    'byteSize' => $node->getSize(),
                    'checksumSha256' => $this->fileSha256($node),
                    'uri' => $this->davUri($uid, $managedRoot . '/' . $relative),
                ];
            }
        }
        return $files;
    }

    private function handleWorkspaceFailure(string $workspaceId, \Throwable $exception): void {
        $workspace = $this->workspaceById($workspaceId);
        if ($workspace === null) {
            return;
        }
        $retry = (int)$workspace['attempts'] < self::MAX_ATTEMPTS;
        $this->updateWorkspace($workspaceId, [
            'state' => $retry ? 'QUEUED' : 'FAILED',
            'lease_expires_at' => null,
            'error_message' => $this->safeError($exception),
        ]);
        if ($retry) {
            $this->schedule($workspaceId);
        }
        $this->logger->warning('Meshingress delegated workspace failed.', [
            'app' => Application::APP_ID,
            'workspaceId' => $workspaceId,
            'attempt' => $workspace['attempts'],
            'retry' => $retry,
            'exception' => $exception,
        ]);
    }

    private function finishFailed(string $workspaceId, ?string $error): void {
        $this->updateWorkspace($workspaceId, [
            'state' => 'FAILED',
            'lease_expires_at' => null,
            'error_message' => $error ?? 'A delegated source exhausted its retries.',
        ]);
    }

    private function schedule(string $workspaceId): void {
        try {
            $this->jobs->add(WorkspaceImportJob::class, ['workspaceId' => $workspaceId]);
        } catch (\Throwable $exception) {
            $this->logger->warning('Meshingress workspace wake-up could not be queued; the dedicated worker will recover it.', [
                'app' => Application::APP_ID,
                'workspaceId' => $workspaceId,
                'exception' => $exception,
            ]);
        }
    }

    private function claim(string $workspaceId): bool {
        $now = time();
        $query = $this->database->getQueryBuilder();
        $changed = $query->update(self::WORKSPACES)
            ->set('state', $query->createNamedParameter('RUNNING'))
            ->set('attempts', $query->createFunction('attempts + 1'))
            ->set('lease_expires_at', $query->createNamedParameter($now + self::LEASE_SECONDS))
            ->set('updated_at', $query->createNamedParameter($now))
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->andWhere($query->expr()->orX(
                $query->expr()->eq('state', $query->createNamedParameter('SEALED')),
                $query->expr()->eq('state', $query->createNamedParameter('QUEUED')),
                $query->expr()->andX(
                    $query->expr()->eq('state', $query->createNamedParameter('RUNNING')),
                    $query->expr()->lt('lease_expires_at', $query->createNamedParameter($now)),
                ),
            ))
            ->executeStatement();
        return $changed === 1;
    }

    /** @param array<string, mixed> $values */
    private function updateWorkspace(string $workspaceId, array $values): void {
        $query = $this->database->getQueryBuilder();
        $query->update(self::WORKSPACES);
        foreach ($values as $column => $value) {
            $query->set($column, $value === null ? $query->createFunction('NULL') : $query->createNamedParameter($value));
        }
        $query->set('updated_at', $query->createNamedParameter(time()))
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeStatement();
    }

    /** @param array<string, mixed> $values */
    private function updateSource(string $sourceId, array $values): void {
        $query = $this->database->getQueryBuilder();
        $query->update(self::SOURCES);
        foreach ($values as $column => $value) {
            $query->set($column, $value === null ? $query->createFunction('NULL') : $query->createNamedParameter($value));
        }
        $query->where($query->expr()->eq('source_id', $query->createNamedParameter($sourceId)))->executeStatement();
    }

    /** @return array<string, mixed> */
    private function openWorkspace(string $uid, string $toolId, string $workspaceId): array {
        $workspace = $this->requireWorkspace($uid, $this->toolId($toolId), $workspaceId);
        if ((string)$workspace['state'] !== 'OPEN') {
            throw new \RuntimeException('The delegated workspace is sealed and cannot be modified.');
        }
        return $workspace;
    }

    /** @return array<string, mixed> */
    private function requireWorkspace(string $uid, string $toolId, string $workspaceId): array {
        $workspaceId = $this->identifier($workspaceId, 'workspaceId', true);
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::WORKSPACES)
            ->where($query->expr()->eq('uid', $query->createNamedParameter($uid)))
            ->andWhere($query->expr()->eq('tool_id', $query->createNamedParameter($toolId)))
            ->andWhere($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeQuery();
        $workspace = $result->fetchAssociative();
        $result->closeCursor();
        if ($workspace === false) {
            throw new \RuntimeException('The delegated workspace was not found.');
        }
        return $workspace;
    }

    /** @return array<string, mixed>|null */
    private function workspaceByRequest(string $uid, string $requestId): ?array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::WORKSPACES)
            ->where($query->expr()->eq('uid', $query->createNamedParameter($uid)))
            ->andWhere($query->expr()->eq('request_id', $query->createNamedParameter($requestId)))
            ->executeQuery();
        $workspace = $result->fetchAssociative();
        $result->closeCursor();
        return $workspace === false ? null : $workspace;
    }

    /** @return array<string, mixed>|null */
    private function workspaceById(string $workspaceId): ?array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::WORKSPACES)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeQuery();
        $workspace = $result->fetchAssociative();
        $result->closeCursor();
        return $workspace === false ? null : $workspace;
    }

    /** @return list<array<string, mixed>> */
    private function processableSources(string $workspaceId): array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::SOURCES)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->andWhere($query->expr()->in('state', $query->createNamedParameter(['PENDING', 'QUEUED', 'RUNNING'], \Doctrine\DBAL\ArrayParameterType::STRING)))
            ->orderBy('position', 'ASC')
            ->executeQuery();
        $sources = [];
        while (($source = $result->fetchAssociative()) !== false) {
            $sources[] = $source;
        }
        $result->closeCursor();
        return $sources;
    }

    /** @return list<array<string, mixed>> */
    private function sources(string $workspaceId): array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::SOURCES)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->orderBy('position', 'ASC')
            ->executeQuery();
        $sources = [];
        while (($source = $result->fetchAssociative()) !== false) {
            $sources[] = $source;
        }
        $result->closeCursor();
        return $sources;
    }

    private function sourceCount(string $workspaceId): int {
        $query = $this->database->getQueryBuilder();
        $result = $query->select($query->createFunction('COUNT(*)'))->from(self::SOURCES)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeQuery();
        $count = (int)$result->fetchOne();
        $result->closeCursor();
        return $count;
    }

    private function sourceExists(string $workspaceId, string $path): bool {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('id')->from(self::SOURCES)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->andWhere($query->expr()->eq('relative_path', $query->createNamedParameter($path)))
            ->setMaxResults(1)
            ->executeQuery();
        $exists = $result->fetchOne() !== false;
        $result->closeCursor();
        return $exists;
    }

    /** @param list<array<string, mixed>> $sources */
    private function hasSourceState(array $sources, string $state): bool {
        foreach ($sources as $source) {
            if ((string)$source['state'] === $state) {
                return true;
            }
        }
        return false;
    }

    /** @param list<array<string, mixed>> $sources */
    private function hasIncompleteSources(array $sources): bool {
        foreach ($sources as $source) {
            if ((string)$source['state'] !== 'COMPLETED') {
                return true;
            }
        }
        return false;
    }

    /** @param list<array<string, mixed>> $sources */
    private function firstSourceError(array $sources): ?string {
        foreach ($sources as $source) {
            if ((string)$source['state'] === 'FAILED') {
                return is_string($source['error_message']) ? $source['error_message'] : null;
            }
        }
        return null;
    }

    /** @param array<string, mixed> $workspace */
    private function assertWritablePath(array $workspace, string $path): void {
        if ($path === self::MANIFEST_NAME) {
            throw new \InvalidArgumentException('manifest.json is reserved for the sealed workspace manifest.');
        }
        if (str_starts_with($path, '.meshingress-')) {
            throw new \InvalidArgumentException('The requested path uses a reserved Meshingress prefix.');
        }
    }

    private function workspace(string $uid): Folder {
        $home = $this->rootFolder->getUserFolder($uid);
        try {
            $parent = $home->get(self::WORKSPACE_PARENT);
        } catch (NotFoundException) {
            throw new \RuntimeException('The selected Workspace folder does not exist for this Nextcloud user.');
        }
        if (!$parent instanceof Folder) {
            throw new \RuntimeException('The selected Workspace path is not a folder.');
        }
        return $this->ensureFolder($parent, self::WORKSPACE_NAME);
    }

    private function rootStorage(string $uid, string $toolId, string $rootPath): Folder {
        $folder = $this->ensureFolder($this->ensureFolder($this->workspace($uid), 'storage'), $toolId);
        foreach (explode('/', $rootPath) as $segment) {
            $folder = $this->ensureFolder($folder, $segment);
        }
        return $folder;
    }

    private function ensureFolder(Folder $parent, string $name): Folder {
        try {
            $node = $parent->get($name);
        } catch (NotFoundException) {
            return $parent->newFolder($name);
        }
        if (!$node instanceof Folder) {
            throw new \RuntimeException('A required Meshingress workspace path is not a folder.');
        }
        return $node;
    }

    private function parentFolder(Folder $root, string $path): Folder {
        $folder = $root;
        $parts = explode('/', $path);
        array_pop($parts);
        foreach ($parts as $part) {
            $folder = $this->ensureFolder($folder, $part);
        }
        return $folder;
    }

    private function fileAt(Folder $root, string $path): ?File {
        try {
            $node = $root->get($path);
        } catch (NotFoundException) {
            return null;
        }
        if (!$node instanceof File) {
            throw new \RuntimeException('The requested destination is not a file: ' . $path);
        }
        return $node;
    }

    private function fileSha256(File $file): string {
        $stream = $file->fopen('r');
        $hash = hash_init('sha256');
        try {
            while (!feof($stream)) {
                $chunk = fread($stream, 8192);
                if ($chunk === false) {
                    throw new \RuntimeException('Nextcloud could not read a published file for hashing.');
                }
                if ($chunk !== '') {
                    hash_update($hash, $chunk);
                }
            }
        } finally {
            fclose($stream);
        }
        return hash_final($hash);
    }

    /** @param array<string, mixed> $workspace @return array<string, mixed> */
    private function response(array $workspace): array {
        if ((string)$workspace['state'] === 'COMPLETED' && is_string($workspace['manifest_json']) && $workspace['manifest_json'] !== '') {
            $manifest = json_decode($workspace['manifest_json'], true);
            if (is_array($manifest)) {
                return $manifest;
            }
        }
        $counts = ['pending' => 0, 'completed' => 0, 'failed' => 0];
        foreach ($this->sources((string)$workspace['workspace_id']) as $source) {
            $state = strtolower((string)$source['state']);
            if ($state === 'completed') {
                ++$counts['completed'];
            } elseif ($state === 'failed') {
                ++$counts['failed'];
            } else {
                ++$counts['pending'];
            }
        }
        $value = [
            'type' => 'meshingress.delegated-workspace',
            'workspaceId' => $workspace['workspace_id'],
            'jobId' => $workspace['job_id'],
            'state' => $workspace['state'],
            'sessionId' => $workspace['session_id'],
            'requestId' => $workspace['request_id'],
            'toolId' => $workspace['tool_id'],
            'sourceCount' => array_sum($counts),
            'sourceCounts' => $counts,
            'attempts' => (int)$workspace['attempts'],
            'createdAt' => gmdate(DATE_ATOM, (int)$workspace['created_at']),
        ];
        if ((string)$workspace['state'] === 'FAILED') {
            $value['error'] = $workspace['error_message'];
        }
        return $value;
    }

    private function sourceUrl(string $value): string {
        $value = trim($value);
        $parts = parse_url($value);
        if ($parts === false || strtolower((string)($parts['scheme'] ?? '')) !== 'https' || !isset($parts['host']) || isset($parts['user']) || isset($parts['pass'])) {
            throw new \InvalidArgumentException('Only credential-free HTTPS source URLs are accepted.');
        }
        return $value;
    }

    private function toolId(string $value): string {
        $value = trim($value);
        if (!preg_match('/^[a-z0-9][a-z0-9._-]{0,127}$/', $value)) {
            throw new \InvalidArgumentException('X-Meshingress-Tool-Id must be a stable lowercase tool identifier.');
        }
        return $value;
    }

    private function identifier(string $value, string $field, bool $required = false): string {
        $value = trim($value);
        if ($value === '') {
            if ($required) {
                throw new \InvalidArgumentException($field . ' is required.');
            }
            return '';
        }
        if (strlen($value) > 128 || preg_match('/[\x00-\x1F\x7F]/', $value)) {
            throw new \InvalidArgumentException($field . ' is invalid.');
        }
        return $value;
    }

    private function filePath(string $value, string $field): string {
        $value = trim(str_replace('\\', '/', $value));
        if ($value === '' || str_starts_with($value, '/') || str_ends_with($value, '/')) {
            throw new \InvalidArgumentException($field . ' must identify a relative file path.');
        }
        foreach (explode('/', $value) as $segment) {
            if ($segment === '' || $segment === '.' || $segment === '..' || str_contains($segment, "\0")) {
                throw new \InvalidArgumentException($field . ' contains an invalid path segment.');
            }
        }
        return $value;
    }

    private function optionalMimeType(mixed $value, string $field): ?string {
        if ($value === null || trim((string)$value) === '') {
            return null;
        }
        $mime = $this->mimeType((string)$value);
        if ($mime === 'application/octet-stream' && strtolower(trim((string)$value)) !== $mime) {
            throw new \InvalidArgumentException($field . ' is invalid.');
        }
        return $mime;
    }

    private function mimeType(string $value): string {
        $mime = strtolower(trim(explode(';', $value, 2)[0]));
        return preg_match('#^[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+$#', $mime) ? $mime : 'application/octet-stream';
    }

    private function expectedByteSize(mixed $value, string $field): ?int {
        if ($value === null || $value === '') {
            return null;
        }
        if (filter_var($value, FILTER_VALIDATE_INT) === false || (int)$value < 0 || (int)$value > self::MAX_FILE_BYTES) {
            throw new \InvalidArgumentException($field . ' must be an integer from 0 through ' . self::MAX_FILE_BYTES . '.');
        }
        return (int)$value;
    }

    private function expectedSha256(mixed $value, string $field): ?string {
        if ($value === null || trim((string)$value) === '') {
            return null;
        }
        $hash = strtolower(trim((string)$value));
        if (!preg_match('/^[a-f0-9]{64}$/', $hash)) {
            throw new \InvalidArgumentException($field . ' must be a lowercase SHA-256 hex digest.');
        }
        return $hash;
    }

    private function managedRootPath(string $toolId, string $rootPath): string {
        return self::WORKSPACE_PARENT . '/' . self::WORKSPACE_NAME . '/storage/' . $toolId . '/' . $rootPath;
    }

    private function davUri(string $uid, string $path, bool $directory = false): string {
        $encoded = array_map('rawurlencode', explode('/', $path));
        $relative = '/remote.php/dav/files/' . rawurlencode($uid) . '/' . implode('/', $encoded);
        return $this->urlGenerator->getAbsoluteURL($directory ? $relative . '/' : $relative);
    }

    private function safeError(\Throwable $exception): string {
        return match ($exception->getMessage()) {
            'The source server rejected the download.',
            'The source exceeds the configured one-gigabyte import limit.',
            'The downloaded source does not match expectedByteSize.',
            'The downloaded source does not match expectedSha256.',
            'The selected Workspace folder does not exist for this Nextcloud user.',
            'The selected Workspace path is not a folder.',
            'The delegated workspace manifest already exists.' => $exception->getMessage(),
            default => 'The delegated source failed while fetching, verifying, or writing the file.',
        };
    }
}

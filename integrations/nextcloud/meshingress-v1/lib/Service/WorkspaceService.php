<?php

declare(strict_types=1);

namespace OCA\Meshingress\Service;

use OCA\Meshingress\AppInfo\Application;
use OCA\Meshingress\BackgroundJob\SourceImportJob;
use OCP\BackgroundJob\IJobList;
use OCP\Files\File;
use OCP\Files\Folder;
use OCP\Files\IRootFolder;
use OCP\Files\NotFoundException;
use OCP\Http\Client\IClientService;
use OCP\IDBConnection;
use OCP\IURLGenerator;
use Psr\Log\LoggerInterface;

/** Owns the stable, user-visible Workspace/Meshingress namespace and source-import jobs. */
final class WorkspaceService {
    private const TABLE = 'meshingress_import_jobs';
    private const DELEGATED_WORKSPACES_TABLE = 'meshingress_delegated_workspaces';
    private const DELEGATED_SOURCES_TABLE = 'meshingress_delegated_sources';
    private const WORKSPACE_PARENT = 'Workspace';
    private const WORKSPACE_NAME = 'Meshingress';
    private const DELEGATED_TYPE = 'meshingress.delegated-download/v1';
    private const MANIFEST_TYPE = 'meshingress.tool-storage-manifest/v1';
    private const MAX_FILE_BYTES = 1073741824;
    private const MAX_SOURCES = 10000;
    private const MAX_ATTEMPTS = 3;

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
    public function initialize(string $uid): array {
        $this->workspace($uid);
        return [
            'workspacePath' => self::WORKSPACE_PARENT . '/' . self::WORKSPACE_NAME,
            'directories' => [
                'storage', 'docs', 'generated', 'repository/artifacts', 'repository/quarantine',
                'repository/vendor', 'var/logs',
            ],
        ];
    }

    /** Reserves an immutable-after-seal tool-owned root before any source is queued. @return array<string, mixed> */
    public function reserveDelegatedWorkspace(string $uid, string $toolId, string $sessionId, string $requestId): array {
        $toolId = $this->toolId($toolId);
        $sessionId = $this->optionalIdentifier($sessionId, 'sessionId');
        $requestId = $this->optionalIdentifier($requestId, 'requestId');
        if ($requestId === '') throw new \InvalidArgumentException('requestId is required.');
        $workspaceId = $requestId;
        $this->rootStorage($uid, $toolId, $this->directoryPath($workspaceId, 'requestId'));
        $now = time();
        $this->database->beginTransaction();
        try {
            $query = $this->database->getQueryBuilder();
            $query->insert(self::DELEGATED_WORKSPACES_TABLE)->values([
                'workspace_id' => $query->createNamedParameter($workspaceId),
                'uid' => $query->createNamedParameter($uid),
                'tool_id' => $query->createNamedParameter($toolId),
                'session_id' => $query->createNamedParameter($sessionId === '' ? null : $sessionId),
                'request_id' => $query->createNamedParameter($requestId),
                'root_path' => $query->createNamedParameter($workspaceId),
                'state' => $query->createNamedParameter('OPEN'),
                'created_at' => $query->createNamedParameter($now),
                'updated_at' => $query->createNamedParameter($now),
            ])->executeStatement();
            $this->database->commit();
        } catch (\Throwable $exception) {
            $this->database->rollBack();
            throw new \RuntimeException('The delegated workspace could not be reserved.');
        }
        return $this->delegatedWorkspaceResponse(['workspace_id' => $workspaceId, 'uid' => $uid, 'tool_id' => $toolId, 'session_id' => $sessionId, 'request_id' => $requestId, 'root_path' => $workspaceId, 'state' => 'OPEN', 'job_id' => null, 'created_at' => $now]);
    }

    /** @return array<string, mixed> */
    public function uploadDelegatedNativeFile(string $uid, string $toolId, string $workspaceId, string $path, string $content, string $contentType): array {
        $workspace = $this->openDelegatedWorkspace($uid, $toolId, $workspaceId);
        $path = $this->filePath($path, 'path');
        if ($path === 'manifest.json' || preg_match('/^manifest-nci_[a-f0-9]{32}\\.json$/', $path)) throw new \InvalidArgumentException('The manifest filename is reserved.');
        if (strlen($content) > self::MAX_FILE_BYTES) throw new \InvalidArgumentException('The native file exceeds the one-gigabyte limit.');
        $root = $this->rootStorage($uid, (string)$workspace['tool_id'], (string)$workspace['root_path']);
        if ($root->nodeExists($path) || $this->delegatedSourceExists($workspaceId, $path)) throw new \RuntimeException('The requested destination already exists: ' . $path);
        $parts = explode('/', $path);
        $filename = array_pop($parts);
        foreach ($parts as $part) $root = $this->ensureFolder($root, $part);
        $file = $root->newFile((string)$filename);
        $file->putContent($content);
        return ['path' => $path, 'uri' => $this->davUri($uid, $this->managedRootPath((string)$workspace['tool_id'], (string)$workspace['root_path']) . '/' . $path), 'byteSize' => strlen($content), 'checksumSha256' => hash('sha256', $content)];
    }

    /** @param mixed $sources @return array<string, mixed> */
    public function appendDelegatedSources(string $uid, string $toolId, string $workspaceId, mixed $sources): array {
        $workspace = $this->openDelegatedWorkspace($uid, $toolId, $workspaceId);
        if (is_string($sources)) $sources = json_decode($sources, true, 512, JSON_THROW_ON_ERROR);
        if (!is_array($sources) || $sources === []) throw new \InvalidArgumentException('sources must be a non-empty JSON array.');
        $root = $this->rootStorage($uid, (string)$workspace['tool_id'], (string)$workspace['root_path']);
        $normalized = [];
        foreach ($sources as $index => $source) {
            if (!is_array($source)) throw new \InvalidArgumentException('sources[' . $index . '] must be an object.');
            $url = $this->url((string)($source['url'] ?? ''));
            $path = $this->filePath((string)($source['path'] ?? ''), 'sources[' . $index . '].path');
            if ($path === 'manifest.json' || preg_match('/^manifest-nci_[a-f0-9]{32}\\.json$/', $path)) throw new \InvalidArgumentException('The manifest filename is reserved.');
            if (isset($normalized[$path]) || $root->nodeExists($path) || $this->delegatedSourceExists($workspaceId, $path)) throw new \RuntimeException('The requested destination already exists: ' . $path);
            $normalized[$path] = $url;
        }
        if ($this->delegatedSourceCount($workspaceId) + count($normalized) > self::MAX_SOURCES) throw new \InvalidArgumentException('sources exceed the maximum of ' . self::MAX_SOURCES . '.');
        $position = $this->delegatedSourceCount($workspaceId);
        $this->database->beginTransaction();
        try {
            foreach ($normalized as $path => $url) {
                $query = $this->database->getQueryBuilder();
                $query->insert(self::DELEGATED_SOURCES_TABLE)->values([
                    'workspace_id' => $query->createNamedParameter($workspaceId),
                    'relative_path' => $query->createNamedParameter($path),
                    'source_url' => $query->createNamedParameter($url),
                    'position' => $query->createNamedParameter($position++),
                ])->executeStatement();
            }
            $this->database->commit();
        } catch (\Throwable $exception) {
            $this->database->rollBack();
            throw new \RuntimeException('The delegated source batch could not be recorded.');
        }
        return ['workspaceId' => $workspaceId, 'state' => 'OPEN', 'sourceCount' => $position];
    }

    /** @return array<string, mixed> */
    public function sealDelegatedWorkspace(string $uid, string $toolId, string $workspaceId): array {
        $workspace = $this->openDelegatedWorkspace($uid, $toolId, $workspaceId);
        $sources = $this->delegatedSources($workspaceId);
        if ($sources === []) throw new \InvalidArgumentException('At least one delegated source is required before sealing.');
        $job = $this->queueDelegatedImport($uid, $toolId, self::DELEGATED_TYPE, (string)($workspace['session_id'] ?? ''), (string)$workspace['request_id'], '', (string)$workspace['root_path'], $sources);
        $query = $this->database->getQueryBuilder();
        $query->update(self::DELEGATED_WORKSPACES_TABLE)
            ->set('state', $query->createNamedParameter('SEALED'))
            ->set('job_id', $query->createNamedParameter((string)$job['jobId']))
            ->set('updated_at', $query->createNamedParameter(time()))
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeStatement();
        return $this->delegatedWorkspaceStatus($uid, $toolId, $workspaceId);
    }

    /** @return array<string, mixed> */
    public function delegatedWorkspaceStatus(string $uid, string $toolId, string $workspaceId): array {
        $workspace = $this->delegatedWorkspace($uid, $this->toolId($toolId), $workspaceId);
        if ($workspace === null) throw new \RuntimeException('The delegated workspace was not found.');
        if (is_string($workspace['job_id']) && $workspace['job_id'] !== '') {
            $job = $this->status($uid, (string)$workspace['job_id']);
            if ($job !== null) return $job;
        }
        return $this->delegatedWorkspaceResponse($workspace);
    }

    /** @return array<string, mixed> */
    public function delegatedWorkspaceByRequest(string $uid, string $requestId): array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::DELEGATED_WORKSPACES_TABLE)
            ->where($query->expr()->eq('uid', $query->createNamedParameter($uid)))
            ->andWhere($query->expr()->eq('request_id', $query->createNamedParameter($this->optionalIdentifier($requestId, 'requestId'))))
            ->executeQuery();
        $workspace = $result->fetchAssociative();
        $result->closeCursor();
        if ($workspace === false) throw new \RuntimeException('The delegated workspace was not found.');
        if (is_string($workspace['job_id']) && $workspace['job_id'] !== '') {
            $job = $this->status($uid, (string)$workspace['job_id']);
            if ($job !== null) return $job;
        }
        return $this->delegatedWorkspaceResponse($workspace);
    }

    /** Existing single-source form retained for compatibility. @return array<string, mixed> */
    public function queueSourceImport(string $uid, string $toolId, string $url, string $path): array {
        $toolId = $this->toolId($toolId);
        $url = $this->url($url);
        $path = $this->path($path);
        $this->toolStorage($uid, $toolId);

        $jobId = 'nci_' . bin2hex(random_bytes(16));
        $now = time();
        $this->database->beginTransaction();
        try {
            $query = $this->database->getQueryBuilder();
            $query->insert(self::TABLE)
                ->values([
                    'job_id' => $query->createNamedParameter($jobId),
                    'uid' => $query->createNamedParameter($uid),
                    'tool_id' => $query->createNamedParameter($toolId),
                    'source_url' => $query->createNamedParameter($url),
                    'relative_path' => $query->createNamedParameter($path),
                    'status' => $query->createNamedParameter('QUEUED'),
                    'attempts' => $query->createNamedParameter(0),
                    'created_at' => $query->createNamedParameter($now),
                    'updated_at' => $query->createNamedParameter($now),
                ])->executeStatement();
            $this->jobs->add(SourceImportJob::class, ['jobId' => $jobId]);
            $this->database->commit();
        } catch (\Throwable $exception) {
            $this->database->rollBack();
            throw new \RuntimeException('The source import could not be queued.');
        }

        return ['jobId' => $jobId, 'state' => 'QUEUED'];
    }

    /**
     * @param array<int, mixed> $sources
     * @return array<string, mixed>
     */
    public function queueDelegatedImport(
        string $uid,
        string $toolId,
        string $type,
        string $sessionId,
        string $requestId,
        string $baseUrl,
        string $rootPath,
        array $sources,
    ): array {
        $toolId = $this->toolId($toolId);
        $type = trim($type);
        if ($type !== '' && $type !== self::DELEGATED_TYPE) {
            throw new \InvalidArgumentException('type must be ' . self::DELEGATED_TYPE . '.');
        }

        $sessionId = $this->optionalIdentifier($sessionId, 'sessionId');
        $requestId = $this->optionalIdentifier($requestId, 'requestId');
        $baseUrl = $this->sourceBaseUrl($baseUrl);
        $jobId = 'nci_' . bin2hex(random_bytes(16));
        $rootPath = trim($rootPath) === '' ? $jobId : $this->directoryPath($rootPath, 'rootPath');
        $normalizedSources = $this->sources($sources, $baseUrl);

        // Reserve the externally owned workspace immediately. Meshingress can
        // upload native files such as book.json to this path while the job runs.
        $this->rootStorage($uid, $toolId, $rootPath);

        $now = time();
        $sourcesJson = json_encode($normalizedSources, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES);
        $firstSourceUrl = (string)$normalizedSources[0]['url'];

        $this->database->beginTransaction();
        try {
            $query = $this->database->getQueryBuilder();
            $query->insert(self::TABLE)
                ->values([
                    'job_id' => $query->createNamedParameter($jobId),
                    'uid' => $query->createNamedParameter($uid),
                    'tool_id' => $query->createNamedParameter($toolId),
                    'source_url' => $query->createNamedParameter($firstSourceUrl),
                    'relative_path' => $query->createNamedParameter($rootPath),
                    'session_id' => $query->createNamedParameter($sessionId === '' ? null : $sessionId),
                    'request_id' => $query->createNamedParameter($requestId === '' ? null : $requestId),
                    'base_url' => $query->createNamedParameter($baseUrl),
                    'root_path' => $query->createNamedParameter($rootPath),
                    'sources_json' => $query->createNamedParameter($sourcesJson),
                    'status' => $query->createNamedParameter('QUEUED'),
                    'attempts' => $query->createNamedParameter(0),
                    'created_at' => $query->createNamedParameter($now),
                    'updated_at' => $query->createNamedParameter($now),
                ])->executeStatement();
            $this->jobs->add(SourceImportJob::class, ['jobId' => $jobId]);
            $this->database->commit();
        } catch (\Throwable $exception) {
            $this->database->rollBack();
            throw new \RuntimeException('The delegated source import could not be queued.');
        }

        return $this->delegatedJobResponse([
            'job_id' => $jobId,
            'uid' => $uid,
            'tool_id' => $toolId,
            'session_id' => $sessionId === '' ? null : $sessionId,
            'request_id' => $requestId === '' ? null : $requestId,
            'root_path' => $rootPath,
            'sources_json' => $sourcesJson,
            'status' => 'QUEUED',
            'attempts' => 0,
            'created_at' => $now,
            'error_message' => null,
            'result_json' => null,
        ]);
    }

    /** @return array<string, mixed>|null */
    public function status(string $uid, string $jobId): ?array {
        if (!preg_match('/^nci_[a-f0-9]{32}$/', $jobId)) {
            return null;
        }
        $job = $this->job($jobId, $uid);
        if ($job === null) {
            return null;
        }
        return $job['sources_json'] === null
            ? $this->publicJob($job)
            : $this->delegatedJobResponse($job);
    }

    public function runSourceImport(string $jobId): void {
        $job = $this->job($jobId);
        if ($job === null || $job['status'] === 'COMPLETED' || $job['status'] === 'FAILED') {
            return;
        }

        if ($job['sources_json'] !== null) {
            $this->runDelegatedImport($job);
            return;
        }

        $this->runLegacyImport($job);
    }

    /** Runs the oldest durable queued import, if one exists. */
    public function runNextQueuedImport(): bool {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('job_id')
            ->from(self::TABLE)
            ->where($query->expr()->eq('status', $query->createNamedParameter('QUEUED')))
            // Prefer current client work. Historical failed/retried records
            // must not delay a newly submitted delegated download.
            ->orderBy('created_at', 'DESC')
            ->setMaxResults(1)
            ->executeQuery();
        $jobId = $result->fetchOne();
        $result->closeCursor();
        if (!is_string($jobId) || $jobId === '') {
            return false;
        }

        $this->runSourceImport($jobId);
        return true;
    }

    /** @param array<string, mixed> $job */
    private function runLegacyImport(array $job): void {
        $jobId = (string)$job['job_id'];
        $attempt = ((int)$job['attempts']) + 1;
        $this->update($jobId, ['status' => 'RUNNING', 'attempts' => $attempt, 'error_message' => null]);
        try {
            $metadata = $this->downloadLegacy($job);
            $metadata['status'] = 'COMPLETED';
            $metadata['completed_at'] = time();
            $metadata['error_message'] = null;
            $this->update($jobId, $metadata);
        } catch (\Throwable $exception) {
            $this->handleFailure($jobId, $attempt, $exception, 'source import');
        }
    }

    /** @param array<string, mixed> $job */
    private function runDelegatedImport(array $job): void {
        $jobId = (string)$job['job_id'];
        $attempt = ((int)$job['attempts']) + 1;
        $this->update($jobId, ['status' => 'RUNNING', 'attempts' => $attempt, 'error_message' => null]);

        /** @var list<array{file: File, path: string}> $created */
        $created = [];
        try {
            $sources = json_decode((string)$job['sources_json'], true, 512, JSON_THROW_ON_ERROR);
            if (!is_array($sources)) {
                throw new \RuntimeException('The delegated source blueprint is invalid.');
            }

            $rootPath = (string)$job['root_path'];
            $root = $this->rootStorage((string)$job['uid'], (string)$job['tool_id'], $rootPath);
            foreach ($sources as $source) {
                if (!is_array($source)) {
                    throw new \RuntimeException('The delegated source blueprint is invalid.');
                }
                $created[] = $this->downloadDelegatedSource($root, (string)$source['url'], (string)$source['path']);
            }

            $completedAt = time();
            $result = $this->buildManifest($job, $root, $completedAt);
            $resultJson = json_encode(
                $result,
                JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT,
            ) . "\n";

            $manifestName = $this->manifestName($jobId);
            if ($root->nodeExists($manifestName)) {
                throw new \RuntimeException('The delegated workspace manifest already exists.');
            }
            $root->newFile($manifestName)->putContent($resultJson);

            $this->update($jobId, [
                'status' => 'COMPLETED',
                'completed_at' => $completedAt,
                'error_message' => null,
                'result_json' => rtrim($resultJson),
            ]);
        } catch (\Throwable $exception) {
            foreach (array_reverse($created) as $entry) {
                try {
                    $entry['file']->delete();
                } catch (\Throwable) {
                    // Best-effort rollback. Native files already present in the
                    // reserved workspace are intentionally never deleted here.
                }
            }
            $this->handleFailure($jobId, $attempt, $exception, 'delegated source import');
        }
    }

    private function handleFailure(string $jobId, int $attempt, \Throwable $exception, string $operation): void {
        $retry = $attempt < self::MAX_ATTEMPTS;
        $this->update($jobId, [
            'status' => $retry ? 'QUEUED' : 'FAILED',
            'error_message' => $this->safeError($exception),
        ]);
        if ($retry) {
            $this->jobs->add(SourceImportJob::class, ['jobId' => $jobId]);
        }
        $this->logger->warning('Meshingress ' . $operation . ' failed.', [
            'app' => Application::APP_ID,
            'jobId' => $jobId,
            'attempt' => $attempt,
            'retry' => $retry,
            'exception' => $exception,
        ]);
    }

    /** @param array<string, mixed> $job @return array<string, mixed> */
    private function downloadLegacy(array $job): array {
        $storage = $this->toolStorage((string)$job['uid'], (string)$job['tool_id']);
        $parts = explode('/', (string)$job['relative_path']);
        $filename = array_pop($parts);
        if ($filename === null || $filename === '') {
            throw new \RuntimeException('The requested destination path is invalid.');
        }
        foreach ($parts as $part) {
            $storage = $this->ensureFolder($storage, $part);
        }
        if ($storage->nodeExists($filename)) {
            throw new \RuntimeException('The requested destination already exists.');
        }

        $response = $this->requestSource((string)$job['source_url']);
        $file = $storage->newFile($filename);
        $metadata = $this->writeResponse($response, $file);

        return [
            'file_id' => $file->getId(),
            'file_size' => $metadata['size'],
            'mime_type' => $metadata['mimeType'],
            'etag' => $file->getEtag(),
            'sha256' => $metadata['sha256'],
        ];
    }

    /** @return array{file: File, path: string} */
    private function downloadDelegatedSource(Folder $root, string $url, string $path): array {
        $parts = explode('/', $path);
        $filename = array_pop($parts);
        if ($filename === null || $filename === '') {
            throw new \RuntimeException('The requested destination path is invalid.');
        }

        $folder = $root;
        foreach ($parts as $part) {
            $folder = $this->ensureFolder($folder, $part);
        }
        if ($folder->nodeExists($filename)) {
            throw new \RuntimeException('The requested destination already exists: ' . $path);
        }

        $response = $this->requestSource($url);
        $file = $folder->newFile($filename);
        try {
            $this->writeResponse($response, $file);
        } catch (\Throwable $exception) {
            try {
                $file->delete();
            } catch (\Throwable) {
            }
            throw $exception;
        }

        return ['file' => $file, 'path' => $path];
    }

    private function requestSource(string $url): mixed {
        $response = $this->http->newClient()->get($url, [
            'timeout' => 300,
            'connect_timeout' => 20,
            'allow_redirects' => ['max' => 3],
        ]);
        if ($response->getStatusCode() < 200 || $response->getStatusCode() >= 300) {
            throw new \RuntimeException('The source server rejected the download.');
        }
        return $response;
    }

    /** @return array{size: int, mimeType: string, sha256: string} */
    private function writeResponse(mixed $response, File $file): array {
        // File::fopen('w') fires its post-write hooks before the caller writes
        // into the stream. Nextcloud therefore cached a 0-byte FileInfo and
        // manifests/dashboard entries reported 0 KB. Stage on disk, then use
        // putContent(), which invalidates FileInfo after the complete write.
        $output = tmpfile();
        if ($output === false) {
            throw new \RuntimeException('Nextcloud could not create temporary storage for the imported file.');
        }
        $input = $response->getBody();
        $hash = hash_init('sha256');
        $size = 0;
        $written = false;
        $writeChunk = static function (string $chunk) use (&$size, $output, $hash): void {
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
                    $writeChunk(substr($input, $offset, 8192));
                }
            } else {
                while (!$input->eof()) {
                    $writeChunk($input->read(8192));
                }
            }
            $written = true;
        } catch (\Throwable $exception) {
            throw $exception;
        } finally {
            // putContent must see the whole stream before it refreshes the
            // Nextcloud file metadata used by the final manifest.
            if ($written) {
                rewind($output);
                $file->putContent($output);
            }
            // Nextcloud consumes and may close a resource passed to
            // putContent(). Only close it ourselves when it remains open.
            if (is_resource($output)) {
                fclose($output);
            }
        }

        return [
            'size' => $size,
            'mimeType' => $this->mimeType((string)$response->getHeader('Content-Type')),
            'sha256' => hash_final($hash),
        ];
    }

    /** @param array<string, mixed> $job @return array<string, mixed> */
    private function buildManifest(array $job, Folder $root, int $completedAt): array {
        $jobId = (string)$job['job_id'];
        $rootPath = (string)$job['root_path'];
        $managedPath = $this->managedRootPath((string)$job['tool_id'], $rootPath);
        $files = $this->collectManifestFiles(
            $root,
            '',
            (string)$job['uid'],
            $managedPath,
        );
        usort($files, static fn(array $left, array $right): int => strcmp((string)$left['path'], (string)$right['path']));

        $workspaceUri = $this->davUri((string)$job['uid'], $managedPath, true);
        $manifestName = $this->manifestName($jobId);

        return [
            'type' => self::MANIFEST_TYPE,
            'state' => 'COMPLETED',
            'manifestId' => $jobId,
            'sessionId' => $job['session_id'],
            'requestId' => $job['request_id'],
            'toolId' => $job['tool_id'],
            'baseUrl' => $this->publicBaseUrl(),
            'filesUri' => $workspaceUri,
            'manifestUri' => $this->davUri((string)$job['uid'], $managedPath . '/' . $manifestName),
            'createdAt' => gmdate(DATE_ATOM, (int)$job['created_at']),
            'completedAt' => gmdate(DATE_ATOM, $completedAt),
            'files' => $files,
        ];
    }

    /** @return list<array<string, mixed>> */
    private function collectManifestFiles(Folder $folder, string $prefix, string $uid, string $managedRoot): array {
        $files = [];
        foreach ($folder->getDirectoryListing() as $node) {
            $relative = $prefix === '' ? $node->getName() : $prefix . '/' . $node->getName();
            if ($node instanceof Folder) {
                array_push($files, ...$this->collectManifestFiles($node, $relative, $uid, $managedRoot));
                continue;
            }
            if (!$node instanceof File || ($prefix === '' && preg_match('/^manifest-nci_[a-f0-9]{32}\.json$/', $node->getName()))) {
                continue;
            }

            $files[] = [
                'mimeType' => $node->getMimeType(),
                'uri' => $this->davUri($uid, $managedRoot . '/' . $relative),
                'byteSize' => $node->getSize(),
                'checksumSha256' => $this->fileSha256($node),
                'path' => $relative,
            ];
        }
        return $files;
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

        $root = $this->ensureFolder($parent, self::WORKSPACE_NAME);
        foreach (['storage', 'docs', 'generated', 'repository', 'var'] as $path) {
            $this->ensureFolder($root, $path);
        }
        $repository = $this->ensureFolder($root, 'repository');
        foreach (['artifacts', 'quarantine', 'vendor'] as $path) {
            $this->ensureFolder($repository, $path);
        }
        $var = $this->ensureFolder($root, 'var');
        $this->ensureFolder($var, 'logs');
        return $root;
    }

    private function toolStorage(string $uid, string $toolId): Folder {
        return $this->ensureFolder($this->ensureFolder($this->workspace($uid), 'storage'), $toolId);
    }

    private function rootStorage(string $uid, string $toolId, string $rootPath): Folder {
        $folder = $this->toolStorage($uid, $toolId);
        foreach (explode('/', $rootPath) as $segment) {
            $folder = $this->ensureFolder($folder, $segment);
        }
        return $folder;
    }

    /** @return array<string, mixed> */
    private function openDelegatedWorkspace(string $uid, string $toolId, string $workspaceId): array {
        $workspace = $this->delegatedWorkspace($uid, $this->toolId($toolId), $workspaceId);
        if ($workspace === null) throw new \RuntimeException('The delegated workspace was not found.');
        if ((string)$workspace['state'] !== 'OPEN') throw new \RuntimeException('The delegated workspace is sealed and cannot be modified.');
        return $workspace;
    }

    /** @return array<string, mixed>|null */
    private function delegatedWorkspace(string $uid, string $toolId, string $workspaceId): ?array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('*')->from(self::DELEGATED_WORKSPACES_TABLE)
            ->where($query->expr()->eq('uid', $query->createNamedParameter($uid)))
            ->andWhere($query->expr()->eq('tool_id', $query->createNamedParameter($toolId)))
            ->andWhere($query->expr()->eq('workspace_id', $query->createNamedParameter($this->optionalIdentifier($workspaceId, 'workspaceId'))))
            ->executeQuery();
        $row = $result->fetchAssociative();
        $result->closeCursor();
        return $row === false ? null : $row;
    }

    private function delegatedSourceExists(string $workspaceId, string $path): bool {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('id')->from(self::DELEGATED_SOURCES_TABLE)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->andWhere($query->expr()->eq('relative_path', $query->createNamedParameter($path)))
            ->setMaxResults(1)->executeQuery();
        $exists = $result->fetchOne() !== false;
        $result->closeCursor();
        return $exists;
    }

    private function delegatedSourceCount(string $workspaceId): int {
        $query = $this->database->getQueryBuilder();
        $result = $query->select($query->createFunction('COUNT(*)'))->from(self::DELEGATED_SOURCES_TABLE)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->executeQuery();
        $value = $result->fetchOne();
        $result->closeCursor();
        return (int)$value;
    }

    /** @return list<array{url: string, path: string}> */
    private function delegatedSources(string $workspaceId): array {
        $query = $this->database->getQueryBuilder();
        $result = $query->select('source_url', 'relative_path')->from(self::DELEGATED_SOURCES_TABLE)
            ->where($query->expr()->eq('workspace_id', $query->createNamedParameter($workspaceId)))
            ->orderBy('position', 'ASC')->executeQuery();
        $sources = [];
        while (($row = $result->fetchAssociative()) !== false) $sources[] = ['url' => (string)$row['source_url'], 'path' => (string)$row['relative_path']];
        $result->closeCursor();
        return $sources;
    }

    /** @param array<string, mixed> $workspace @return array<string, mixed> */
    private function delegatedWorkspaceResponse(array $workspace): array {
        $managedPath = $this->managedRootPath((string)$workspace['tool_id'], (string)$workspace['root_path']);
        return [
            'type' => 'meshingress.delegated-workspace/v1',
            'workspaceId' => $workspace['workspace_id'],
            'state' => $workspace['state'],
            'sessionId' => $workspace['session_id'],
            'requestId' => $workspace['request_id'],
            'toolId' => $workspace['tool_id'],
            'workspaceUri' => $this->davUri((string)$workspace['uid'], $managedPath, true),
            'sourceCount' => $this->delegatedSourceCount((string)$workspace['workspace_id']),
            'createdAt' => gmdate(DATE_ATOM, (int)$workspace['created_at']),
        ];
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

    /** @return array<string, mixed>|null */
    private function job(string $jobId, ?string $uid = null): ?array {
        $query = $this->database->getQueryBuilder();
        $query->select('*')->from(self::TABLE)
            ->where($query->expr()->eq('job_id', $query->createNamedParameter($jobId)));
        if ($uid !== null) {
            $query->andWhere($query->expr()->eq('uid', $query->createNamedParameter($uid)));
        }
        $result = $query->executeQuery();
        $row = $result->fetchAssociative();
        $result->closeCursor();
        return $row === false ? null : $row;
    }

    /** @param array<string, mixed> $values */
    private function update(string $jobId, array $values): void {
        $query = $this->database->getQueryBuilder();
        $query->update(self::TABLE);
        foreach ($values as $column => $value) {
            // set() accepts parameter and function-expression objects. The
            // latter keeps SQL NULL unquoted while ordinary values remain
            // bound parameters.
            $query->set($column, $value === null ? $query->createFunction('NULL') : $query->createNamedParameter($value));
        }
        $query->set('updated_at', $query->createNamedParameter(time()))
            ->where($query->expr()->eq('job_id', $query->createNamedParameter($jobId)))
            ->executeStatement();
    }

    /** @param array<string, mixed> $job @return array<string, mixed> */
    private function delegatedJobResponse(array $job): array {
        if ($job['status'] === 'COMPLETED' && is_string($job['result_json']) && $job['result_json'] !== '') {
            $result = json_decode($job['result_json'], true);
            if (is_array($result)) {
                return $result;
            }
        }

        $rootPath = (string)$job['root_path'];
        $managedPath = $this->managedRootPath((string)$job['tool_id'], $rootPath);
        $value = [
            'type' => 'meshingress.delegated-download-job/v1',
            'jobId' => $job['job_id'],
            'state' => $job['status'],
            'sessionId' => $job['session_id'],
            'requestId' => $job['request_id'],
            'toolId' => $job['tool_id'],
            'rootPath' => $rootPath,
            'workspacePath' => $managedPath,
            'workspaceUri' => $this->davUri((string)$job['uid'], $managedPath, true),
            'manifestUri' => $this->davUri(
                (string)$job['uid'],
                $managedPath . '/' . $this->manifestName((string)$job['job_id']),
            ),
            'sourceCount' => $this->sourceCount((string)$job['sources_json']),
            'attempts' => (int)$job['attempts'],
            'createdAt' => gmdate(DATE_ATOM, (int)$job['created_at']),
        ];
        if ($job['status'] === 'FAILED') {
            $value['error'] = $job['error_message'];
        }
        return $value;
    }

    private function sourceCount(string $sourcesJson): int {
        $sources = json_decode($sourcesJson, true);
        return is_array($sources) ? count($sources) : 0;
    }

    /** @param array<string, mixed> $job @return array<string, mixed> */
    private function publicJob(array $job): array {
        $value = [
            'jobId' => $job['job_id'],
            'state' => $job['status'],
            'toolId' => $job['tool_id'],
            'sourceUrl' => $job['source_url'],
            'path' => 'storage/' . $job['tool_id'] . '/' . $job['relative_path'],
            'attempts' => (int)$job['attempts'],
            'createdAt' => gmdate(DATE_ATOM, (int)$job['created_at']),
        ];
        if ($job['status'] === 'COMPLETED') {
            $value += [
                'fileId' => (int)$job['file_id'],
                'size' => (int)$job['file_size'],
                'mimeType' => $job['mime_type'],
                'etag' => $job['etag'],
                'sha256' => $job['sha256'],
                'completedAt' => gmdate(DATE_ATOM, (int)$job['completed_at']),
            ];
        }
        if ($job['status'] === 'FAILED') {
            $value['error'] = $job['error_message'];
        }
        return $value;
    }

    /** @param array<int, mixed> $sources @return list<array{url: string, path: string}> */
    private function sources(array $sources, ?string $baseUrl): array {
        if ($sources === [] || count($sources) > self::MAX_SOURCES) {
            throw new \InvalidArgumentException('sources must contain between 1 and ' . self::MAX_SOURCES . ' entries.');
        }

        $normalized = [];
        $paths = [];
        foreach ($sources as $index => $source) {
            if (!is_array($source)) {
                throw new \InvalidArgumentException('sources[' . $index . '] must be an object.');
            }
            $rawUrl = trim((string)($source['url'] ?? ''));
            if ($rawUrl === '') {
                throw new \InvalidArgumentException('sources[' . $index . '].url is required.');
            }

            $outlet = $source['outlet'] ?? null;
            $outletPath = null;
            if ($outlet !== null) {
                if (!is_array($outlet)) {
                    throw new \InvalidArgumentException('sources[' . $index . '].outlet must be an object.');
                }
                if (array_key_exists('path', $outlet) && trim((string)$outlet['path']) !== '') {
                    $outletPath = $this->filePath((string)$outlet['path'], 'sources[' . $index . '].outlet.path');
                }
            }

            $path = $outletPath ?? $this->pathFromSourceUrl($rawUrl, $index);
            if (isset($paths[$path])) {
                throw new \InvalidArgumentException('Multiple sources resolve to the same outlet path: ' . $path);
            }
            $paths[$path] = true;
            $normalized[] = [
                'url' => $this->resolveSourceUrl($rawUrl, $baseUrl),
                'path' => $path,
            ];
        }
        return $normalized;
    }

    private function toolId(string $toolId): string {
        $toolId = trim($toolId);
        if (!preg_match('/^[a-z0-9][a-z0-9._-]{0,127}$/', $toolId)) {
            throw new \InvalidArgumentException('X-Meshingress-Tool-Id must be a stable lowercase tool identifier.');
        }
        return $toolId;
    }

    private function optionalIdentifier(string $value, string $field): string {
        $value = trim($value);
        if ($value === '') {
            return '';
        }
        if (strlen($value) > 128 || preg_match('/[\x00-\x1F\x7F]/', $value)) {
            throw new \InvalidArgumentException($field . ' is invalid.');
        }
        return $value;
    }

    private function sourceBaseUrl(string $url): ?string {
        $url = trim($url);
        if ($url === '') {
            return null;
        }
        $validated = $this->url($url);
        $parts = parse_url($validated);
        if (isset($parts['query']) || isset($parts['fragment'])) {
            throw new \InvalidArgumentException('baseUrl must not contain a query or fragment.');
        }
        return rtrim($validated, '/') . '/';
    }

    private function resolveSourceUrl(string $sourceUrl, ?string $baseUrl): string {
        $parts = parse_url($sourceUrl);
        if ($parts !== false && isset($parts['scheme'])) {
            return $this->url($sourceUrl);
        }
        if (str_starts_with($sourceUrl, '//')) {
            throw new \InvalidArgumentException('Protocol-relative source URLs are not accepted.');
        }
        if ($baseUrl === null) {
            throw new \InvalidArgumentException('baseUrl is required when a source URL is relative.');
        }

        $base = parse_url($baseUrl);
        if ($base === false || !isset($base['scheme'], $base['host'])) {
            throw new \InvalidArgumentException('baseUrl is invalid.');
        }

        $fragmentless = explode('#', $sourceUrl, 2)[0];
        $queryParts = explode('?', $fragmentless, 2);
        $relativePath = $queryParts[0];
        $query = $queryParts[1] ?? null;
        $basePath = (string)($base['path'] ?? '/');
        $joinedPath = str_starts_with($relativePath, '/')
            ? $relativePath
            : rtrim($basePath, '/') . '/' . $relativePath;
        $joinedPath = $this->normalizeUrlPath($joinedPath);

        $authority = $base['scheme'] . '://' . $base['host'];
        if (isset($base['port'])) {
            $authority .= ':' . $base['port'];
        }
        $resolved = $authority . $joinedPath;
        if ($query !== null && $query !== '') {
            $resolved .= '?' . $query;
        }
        return $this->url($resolved);
    }

    private function normalizeUrlPath(string $path): string {
        $segments = [];
        foreach (explode('/', $path) as $segment) {
            if ($segment === '' || $segment === '.') {
                continue;
            }
            if ($segment === '..') {
                array_pop($segments);
                continue;
            }
            $segments[] = $segment;
        }
        return '/' . implode('/', $segments);
    }

    private function pathFromSourceUrl(string $sourceUrl, int $index): string {
        $path = parse_url($sourceUrl, PHP_URL_PATH);
        if (!is_string($path) || $path === '' || str_ends_with($path, '/')) {
            throw new \InvalidArgumentException('sources[' . $index . '] requires outlet.path because its URL has no file path.');
        }

        $decoded = [];
        foreach (explode('/', trim($path, '/')) as $segment) {
            $value = rawurldecode($segment);
            if (str_contains($value, '/') || str_contains($value, '\\')) {
                throw new \InvalidArgumentException('sources[' . $index . '] contains an encoded path separator.');
            }
            $decoded[] = $value;
        }
        return $this->filePath(implode('/', $decoded), 'sources[' . $index . '].url');
    }

    private function url(string $url): string {
        $url = trim($url);
        $parts = parse_url($url);
        if ($parts === false || strtolower((string)($parts['scheme'] ?? '')) !== 'https' || !isset($parts['host']) || isset($parts['user']) || isset($parts['pass'])) {
            throw new \InvalidArgumentException('Only credential-free HTTPS source URLs are accepted.');
        }
        return $url;
    }

    private function path(string $path): string {
        $path = trim(str_replace('\\', '/', $path));
        if ($path === '' || str_starts_with($path, '/') || str_ends_with($path, '/')) {
            throw new \InvalidArgumentException('path must be a non-empty relative file path.');
        }
        return $this->validatePathSegments($path, 'path');
    }

    private function filePath(string $path, string $field): string {
        $path = trim(str_replace('\\', '/', $path));
        $path = ltrim($path, '/');
        if ($path === '' || str_ends_with($path, '/')) {
            throw new \InvalidArgumentException($field . ' must identify a file beneath the delegated root.');
        }
        return $this->validatePathSegments($path, $field);
    }

    private function directoryPath(string $path, string $field): string {
        $path = trim(str_replace('\\', '/', $path));
        $path = trim($path, '/');
        if ($path === '') {
            throw new \InvalidArgumentException($field . ' must be a non-empty relative directory path.');
        }
        return $this->validatePathSegments($path, $field);
    }

    private function validatePathSegments(string $path, string $field): string {
        $segments = explode('/', $path);
        foreach ($segments as $segment) {
            if ($segment === '' || $segment === '.' || $segment === '..' || str_contains($segment, "\0")) {
                throw new \InvalidArgumentException($field . ' contains an invalid segment.');
            }
        }
        return implode('/', $segments);
    }

    private function mimeType(string $header): string {
        $mime = strtolower(trim(explode(';', $header, 2)[0]));
        return preg_match('#^[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+$#', $mime) ? $mime : 'application/octet-stream';
    }

    private function manifestName(string $jobId): string {
        return 'manifest-' . $jobId . '.json';
    }

    private function managedRootPath(string $toolId, string $rootPath): string {
        return self::WORKSPACE_PARENT . '/' . self::WORKSPACE_NAME . '/storage/' . $toolId . '/' . $rootPath;
    }

    private function publicBaseUrl(): string {
        return rtrim($this->urlGenerator->getBaseUrl(), '/');
    }

    private function davUri(string $uid, string $path, bool $directory = false): string {
        $encoded = array_map('rawurlencode', explode('/', $path));
        $relative = '/remote.php/dav/files/' . rawurlencode($uid) . '/' . implode('/', $encoded);
        if ($directory) {
            $relative .= '/';
        }
        return $this->urlGenerator->getAbsoluteURL($relative);
    }

    private function safeError(\Throwable $exception): string {
        $message = $exception->getMessage();
        if (
            str_starts_with($message, 'The requested destination already exists') ||
            str_starts_with($message, 'Multiple sources resolve to the same outlet path')
        ) {
            return $message;
        }
        return match ($message) {
            'The source server rejected the download.',
            'The source exceeds the configured one-gigabyte import limit.',
            'The selected Workspace folder does not exist for this Nextcloud user.',
            'The selected Workspace path is not a folder.',
            'The delegated workspace manifest already exists.' => $message,
            default => 'The source import failed while fetching, verifying, or writing the files.',
        };
    }
}

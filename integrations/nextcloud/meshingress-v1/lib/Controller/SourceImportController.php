<?php

declare(strict_types=1);

namespace OCA\Meshingress\Controller;

use OCA\Meshingress\Service\WorkspaceService;
use OCP\AppFramework\Http\Attribute\NoAdminRequired;
use OCP\AppFramework\Http\Attribute\NoCSRFRequired;
use OCP\AppFramework\Http\DataResponse;
use OCP\AppFramework\OCSController;
use OCP\IRequest;
use OCP\IUserSession;

final class SourceImportController extends OCSController {
    public function __construct(
        string $appName,
        IRequest $request,
        private readonly IUserSession $userSession,
        private readonly WorkspaceService $workspace,
    ) {
        parent::__construct($appName, $request);
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function create(): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) {
            return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        }

        try {
            $sources = $this->request->getParam('sources', null);
            if (is_string($sources)) {
                $sources = json_decode($sources, true, 512, JSON_THROW_ON_ERROR);
            }

            if ($sources !== null) {
                if (!is_array($sources)) {
                    throw new \InvalidArgumentException('sources must be a JSON array.');
                }

                $job = $this->workspace->queueDelegatedImport(
                    $user->getUID(),
                    (string)$this->request->getHeader('X-Meshingress-Tool-Id'),
                    (string)$this->request->getParam('type', ''),
                    (string)$this->request->getParam('sessionId', ''),
                    (string)$this->request->getParam('requestId', ''),
                    (string)$this->request->getParam('baseUrl', ''),
                    (string)$this->request->getParam('rootPath', ''),
                    $sources,
                );
            } else {
                $job = $this->workspace->queueSourceImport(
                    $user->getUID(),
                    (string)$this->request->getHeader('X-Meshingress-Tool-Id'),
                    (string)$this->request->getParam('url', ''),
                    (string)$this->request->getParam('path', ''),
                );
            }

            return new DataResponse($job, 202);
        } catch (\JsonException|\InvalidArgumentException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 400);
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function show(string $jobId): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) {
            return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        }

        $job = $this->workspace->status($user->getUID(), $jobId);
        return $job === null
            ? new DataResponse(['message' => 'Source import job was not found.'], 404)
            : new DataResponse($job);
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function initialize(): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) {
            return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        }

        try {
            return new DataResponse($this->workspace->initialize($user->getUID()));
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }
}

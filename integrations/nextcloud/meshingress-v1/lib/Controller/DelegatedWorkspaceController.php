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

final class DelegatedWorkspaceController extends OCSController {
    public function __construct(string $appName, IRequest $request, private readonly IUserSession $userSession, private readonly WorkspaceService $workspace) {
        parent::__construct($appName, $request);
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function reserve(): DataResponse {
        return $this->execute(fn(string $uid, string $toolId) => $this->workspace->reserveDelegatedWorkspace($uid, $toolId, (string)$this->request->getParam('sessionId', ''), (string)$this->request->getParam('requestId', '')));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function upload(string $workspaceId): DataResponse {
        return $this->execute(function (string $uid, string $toolId) use ($workspaceId): array {
            $encoded = (string)$this->request->getParam('contentBase64', '');
            $content = base64_decode($encoded, true);
            if ($content === false) {
                throw new \InvalidArgumentException('contentBase64 must be valid Base64.');
            }
            return $this->workspace->uploadDelegatedNativeFile(
                $uid,
                $toolId,
                $workspaceId,
                (string)$this->request->getParam('path', ''),
                $content,
                (string)$this->request->getParam('contentType', 'application/octet-stream'),
            );
        });
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function appendSources(string $workspaceId): DataResponse {
        return $this->execute(fn(string $uid, string $toolId) => $this->workspace->appendDelegatedSources($uid, $toolId, $workspaceId, $this->request->getParam('sources', null)));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function seal(string $workspaceId): DataResponse {
        return $this->execute(fn(string $uid, string $toolId) => $this->workspace->sealDelegatedWorkspace($uid, $toolId, $workspaceId));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function show(string $workspaceId): DataResponse {
        return $this->execute(fn(string $uid, string $toolId) => $this->workspace->delegatedWorkspaceStatus($uid, $toolId, $workspaceId));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function showByRequest(string $requestId): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        try {
            return new DataResponse($this->workspace->delegatedWorkspaceByRequest($user->getUID(), $requestId));
        } catch (\InvalidArgumentException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 400);
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }

    private function execute(callable $operation): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        try {
            return new DataResponse($operation($user->getUID(), (string)$this->request->getHeader('X-Meshingress-Tool-Id')));
        } catch (\InvalidArgumentException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 400);
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }
}

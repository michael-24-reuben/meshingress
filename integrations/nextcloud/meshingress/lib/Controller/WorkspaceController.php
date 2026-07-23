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

/** OCS surface for the one mutable-until-sealed delegated workspace lifecycle. */
final class WorkspaceController extends OCSController {
    public function __construct(
        string $appName,
        IRequest $request,
        private readonly IUserSession $userSession,
        private readonly WorkspaceService $workspaces,
    ) {
        parent::__construct($appName, $request);
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function reserve(): DataResponse {
        return $this->withTool(fn(string $uid, string $toolId): array => $this->workspaces->reserve(
            $uid,
            $toolId,
            (string)$this->request->getParam('requestId', ''),
            (string)$this->request->getParam('sessionId', ''),
        ));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function upload(string $workspaceId): DataResponse {
        return $this->withTool(function (string $uid, string $toolId) use ($workspaceId): array {
            $content = base64_decode((string)$this->request->getParam('contentBase64', ''), true);
            if ($content === false) {
                throw new \InvalidArgumentException('contentBase64 must be valid Base64.');
            }
            return $this->workspaces->uploadNativeFile(
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
        return $this->withTool(fn(string $uid, string $toolId): array => $this->workspaces->appendSources(
            $uid,
            $toolId,
            $workspaceId,
            $this->request->getParam('sources', null),
        ));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function seal(string $workspaceId): DataResponse {
        return $this->withTool(fn(string $uid, string $toolId): array => $this->workspaces->seal($uid, $toolId, $workspaceId));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function show(string $workspaceId): DataResponse {
        return $this->withTool(fn(string $uid, string $toolId): array => $this->workspaces->status($uid, $toolId, $workspaceId));
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function showByRequest(string $requestId): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) {
            return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        }
        try {
            return new DataResponse($this->workspaces->statusByRequest($user->getUID(), $requestId));
        } catch (\InvalidArgumentException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 400);
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }

    /** @param callable(string, string):array<string, mixed> $operation */
    private function withTool(callable $operation): DataResponse {
        $user = $this->userSession->getUser();
        if ($user === null) {
            return new DataResponse(['message' => 'Nextcloud authentication is required.'], 401);
        }
        try {
            return new DataResponse($operation($user->getUID(), (string)$this->request->getHeader('X-Meshingress-Tool-Id')));
        } catch (\InvalidArgumentException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 400);
        } catch (\RuntimeException $exception) {
            return new DataResponse(['message' => $exception->getMessage()], 409);
        }
    }
}

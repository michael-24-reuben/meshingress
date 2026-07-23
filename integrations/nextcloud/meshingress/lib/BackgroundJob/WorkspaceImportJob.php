<?php

declare(strict_types=1);

namespace OCA\Meshingress\BackgroundJob;

use OCA\Meshingress\Service\WorkspaceService;
use OCP\AppFramework\Utility\ITimeFactory;
use OCP\BackgroundJob\QueuedJob;

/** A wake-up trigger only; workspace state and retryability remain durable in the database. */
final class WorkspaceImportJob extends QueuedJob {
    public function __construct(ITimeFactory $time, private readonly WorkspaceService $workspaces) {
        parent::__construct($time);
    }

    #[\Override]
    protected function run($argument): void {
        if (is_string($argument)) {
            $argument = json_decode($argument, true);
        } elseif (is_object($argument)) {
            $argument = (array)$argument;
        }
        if (is_array($argument) && isset($argument['workspaceId']) && is_string($argument['workspaceId'])) {
            $this->workspaces->runWorkspace($argument['workspaceId']);
        }
    }
}

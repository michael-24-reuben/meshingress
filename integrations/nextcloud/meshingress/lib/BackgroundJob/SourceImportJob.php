<?php

declare(strict_types=1);

namespace OCA\Meshingress\BackgroundJob;

use OCA\Meshingress\Service\WorkspaceService;
use OCP\AppFramework\Utility\ITimeFactory;
use OCP\BackgroundJob\QueuedJob;

final class SourceImportJob extends QueuedJob {
    public function __construct(ITimeFactory $time, private readonly WorkspaceService $workspace) {
        parent::__construct($time);
    }

    #[\Override]
    protected function run($argument): void {
        if (is_string($argument)) {
            $argument = json_decode($argument, true);
        } elseif (is_object($argument)) {
            $argument = (array)$argument;
        }
        if (!is_array($argument) || !isset($argument['jobId']) || !is_string($argument['jobId'])) {
            return;
        }
        $this->workspace->runSourceImport($argument['jobId']);
    }
}

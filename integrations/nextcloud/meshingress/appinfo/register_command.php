<?php

declare(strict_types=1);

use OCA\Meshingress\Command\WorkspaceWorkerCommand;

$application->add(\OC::$server->get(WorkspaceWorkerCommand::class));

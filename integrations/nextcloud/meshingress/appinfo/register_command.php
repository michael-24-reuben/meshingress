<?php

declare(strict_types=1);

use OCA\Meshingress\Command\SourceImportWorkerCommand;

$application->add(\OC::$server->get(SourceImportWorkerCommand::class));

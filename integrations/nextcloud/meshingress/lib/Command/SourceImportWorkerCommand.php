<?php

declare(strict_types=1);

namespace OCA\Meshingress\Command;

use OCA\Meshingress\Service\WorkspaceService;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;

/** Runs one durable Meshingress source-import record for the host scheduler. */
final class SourceImportWorkerCommand extends Command {
    public function __construct(private readonly WorkspaceService $workspace) {
        parent::__construct();
    }

    #[\Override]
    protected function configure(): void {
        $this->setName('meshingress:source-import:work')
            ->setDescription('Run queued Meshingress source imports.')
            ->addOption('watch', null, InputOption::VALUE_NONE, 'Keep polling for queued imports.')
            ->addOption('interval', null, InputOption::VALUE_REQUIRED, 'Polling interval in seconds when --watch is set.', '1');
    }

    #[\Override]
    protected function execute(InputInterface $input, OutputInterface $output): int {
        $watch = (bool)$input->getOption('watch');
        $intervalSeconds = filter_var($input->getOption('interval'), FILTER_VALIDATE_INT, [
            'options' => ['min_range' => 1, 'max_range' => 60],
        ]);
        if ($intervalSeconds === false) {
            $output->writeln('<error>--interval must be an integer from 1 through 60 seconds.</error>');
            return self::INVALID;
        }

        do {
            if ($this->workspace->runNextQueuedImport()) {
                $output->writeln('Processed one queued Meshingress source import.');
                continue;
            }
            if (!$watch) {
                $output->writeln('No queued Meshingress source imports.');
                return self::SUCCESS;
            }
            sleep($intervalSeconds);
        } while (true);
    }
}

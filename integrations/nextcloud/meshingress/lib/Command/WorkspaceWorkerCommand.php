<?php

declare(strict_types=1);

namespace OCA\Meshingress\Command;

use OCA\Meshingress\Service\WorkspaceService;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;

/** Dedicated worker bypasses generic Nextcloud queue starvation without bypassing durable state. */
final class WorkspaceWorkerCommand extends Command {
    public function __construct(private readonly WorkspaceService $workspaces) {
        parent::__construct();
    }

    #[\Override]
    protected function configure(): void {
        $this->setName('meshingress:workspace:work')
            ->setDescription('Run sealed Meshingress delegated workspaces.')
            ->addOption('watch', null, InputOption::VALUE_NONE, 'Keep polling for sealed or queued workspaces.')
            ->addOption('interval', null, InputOption::VALUE_REQUIRED, 'Polling interval in seconds when --watch is set.', '1');
    }

    #[\Override]
    protected function execute(InputInterface $input, OutputInterface $output): int {
        $watch = (bool)$input->getOption('watch');
        $interval = filter_var($input->getOption('interval'), FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 60]]);
        if ($interval === false) {
            $output->writeln('<error>--interval must be an integer from 1 through 60 seconds.</error>');
            return self::INVALID;
        }
        do {
            if ($this->workspaces->runNextWorkspace()) {
                $output->writeln('Processed one Meshingress delegated workspace.');
                continue;
            }
            if (!$watch) {
                $output->writeln('No sealed or queued Meshingress workspaces.');
                return self::SUCCESS;
            }
            sleep($interval);
        } while (true);
    }
}

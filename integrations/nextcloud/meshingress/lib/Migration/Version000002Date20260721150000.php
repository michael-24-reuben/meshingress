<?php

declare(strict_types=1);

namespace OCA\Meshingress\Migration;

use Closure;
use Doctrine\DBAL\Types\Types;
use OCP\DB\ISchemaWrapper;
use OCP\Migration\IOutput;
use OCP\Migration\SimpleMigrationStep;

final class Version000002Date20260721150000 extends SimpleMigrationStep {
    #[\Override]
    public function changeSchema(IOutput $output, Closure $schemaClosure, array $options): ?ISchemaWrapper {
        /** @var ISchemaWrapper $schema */
        $schema = $schemaClosure();
        if (!$schema->hasTable('meshingress_import_jobs')) {
            return null;
        }

        $table = $schema->getTable('meshingress_import_jobs');
        $changed = false;

        if (!$table->hasColumn('session_id')) {
            $table->addColumn('session_id', Types::STRING, ['notnull' => false, 'length' => 128]);
            $changed = true;
        }
        if (!$table->hasColumn('request_id')) {
            $table->addColumn('request_id', Types::STRING, ['notnull' => false, 'length' => 128]);
            $changed = true;
        }
        if (!$table->hasColumn('base_url')) {
            $table->addColumn('base_url', Types::TEXT, ['notnull' => false]);
            $changed = true;
        }
        if (!$table->hasColumn('root_path')) {
            $table->addColumn('root_path', Types::TEXT, ['notnull' => false]);
            $changed = true;
        }
        if (!$table->hasColumn('sources_json')) {
            $table->addColumn('sources_json', Types::TEXT, ['notnull' => false]);
            $changed = true;
        }
        if (!$table->hasColumn('result_json')) {
            $table->addColumn('result_json', Types::TEXT, ['notnull' => false]);
            $changed = true;
        }

        return $changed ? $schema : null;
    }
}

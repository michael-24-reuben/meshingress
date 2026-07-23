<?php

declare(strict_types=1);

namespace OCA\Meshingress\Migration;

use Closure;
use Doctrine\DBAL\Types\Types;
use OCP\DB\ISchemaWrapper;
use OCP\Migration\IOutput;
use OCP\Migration\SimpleMigrationStep;

final class Version000003Date20260722010000 extends SimpleMigrationStep {
    #[\Override]
    public function changeSchema(IOutput $output, Closure $schemaClosure, array $options): ?ISchemaWrapper {
        /** @var ISchemaWrapper $schema */
        $schema = $schemaClosure();
        $changed = false;
        if (!$schema->hasTable('meshingress_delegated_workspaces')) {
            $table = $schema->createTable('meshingress_delegated_workspaces');
            $table->addColumn('id', Types::BIGINT, ['autoincrement' => true, 'unsigned' => true]);
            $table->addColumn('workspace_id', Types::STRING, ['length' => 128]);
            $table->addColumn('uid', Types::STRING, ['length' => 64]);
            $table->addColumn('tool_id', Types::STRING, ['length' => 128]);
            $table->addColumn('session_id', Types::STRING, ['length' => 128, 'notnull' => false]);
            $table->addColumn('request_id', Types::STRING, ['length' => 128]);
            $table->addColumn('root_path', Types::TEXT);
            $table->addColumn('state', Types::STRING, ['length' => 16]);
            $table->addColumn('job_id', Types::STRING, ['length' => 40, 'notnull' => false]);
            $table->addColumn('created_at', Types::BIGINT, ['unsigned' => true]);
            $table->addColumn('updated_at', Types::BIGINT, ['unsigned' => true]);
            $table->setPrimaryKey(['id']);
            $table->addUniqueIndex(['workspace_id'], 'meshingress_dws_id_uq');
            $table->addUniqueIndex(['uid', 'request_id'], 'meshingress_dws_request_uq');
            $table->addIndex(['uid', 'workspace_id'], 'meshingress_dws_owner_ix');
            $changed = true;
        }
        if (!$schema->hasTable('meshingress_delegated_sources')) {
            $table = $schema->createTable('meshingress_delegated_sources');
            $table->addColumn('id', Types::BIGINT, ['autoincrement' => true, 'unsigned' => true]);
            $table->addColumn('workspace_id', Types::STRING, ['length' => 128]);
            $table->addColumn('relative_path', Types::TEXT);
            $table->addColumn('source_url', Types::TEXT);
            $table->addColumn('position', Types::INTEGER, ['unsigned' => true]);
            $table->setPrimaryKey(['id']);
            $table->addUniqueIndex(['workspace_id', 'relative_path'], 'meshingress_dws_path_uq');
            $table->addIndex(['workspace_id', 'position'], 'meshingress_dws_position_ix');
            $changed = true;
        }
        return $changed ? $schema : null;
    }
}

<?php

declare(strict_types=1);

namespace OCA\Meshingress\Migration;

use Closure;
use Doctrine\DBAL\Types\Types;
use OCP\DB\ISchemaWrapper;
use OCP\Migration\IOutput;
use OCP\Migration\SimpleMigrationStep;

/** Fresh schema. Legacy meshingress_* import tables are deliberately untouched. */
final class Version000001Date20260723090000 extends SimpleMigrationStep {
    #[\Override]
    public function changeSchema(IOutput $output, Closure $schemaClosure, array $options): ?ISchemaWrapper {
        /** @var ISchemaWrapper $schema */
        $schema = $schemaClosure();
        $changed = false;

        if (!$schema->hasTable('meshingress_workspaces')) {
            $table = $schema->createTable('meshingress_workspaces');
            $table->addColumn('id', Types::BIGINT, ['autoincrement' => true, 'unsigned' => true]);
            $table->addColumn('workspace_id', Types::STRING, ['length' => 128]);
            $table->addColumn('uid', Types::STRING, ['length' => 64]);
            $table->addColumn('tool_id', Types::STRING, ['length' => 128]);
            $table->addColumn('session_id', Types::STRING, ['length' => 128, 'notnull' => false]);
            $table->addColumn('request_id', Types::STRING, ['length' => 128]);
            $table->addColumn('root_path', Types::TEXT);
            $table->addColumn('state', Types::STRING, ['length' => 16]);
            $table->addColumn('job_id', Types::STRING, ['length' => 40, 'notnull' => false]);
            $table->addColumn('attempts', Types::INTEGER, ['default' => 0, 'unsigned' => true]);
            $table->addColumn('error_message', Types::TEXT, ['notnull' => false]);
            $table->addColumn('lease_expires_at', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->addColumn('manifest_json', Types::TEXT, ['notnull' => false]);
            $table->addColumn('created_at', Types::BIGINT, ['unsigned' => true]);
            $table->addColumn('updated_at', Types::BIGINT, ['unsigned' => true]);
            $table->addColumn('completed_at', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->setPrimaryKey(['id']);
            $table->addUniqueIndex(['workspace_id'], 'meshingress_workspace_id_uq');
            $table->addUniqueIndex(['uid', 'request_id'], 'meshingress_workspace_request_uq');
            $table->addIndex(['state', 'lease_expires_at'], 'meshingress_workspace_queue_ix');
            $table->addIndex(['uid', 'tool_id', 'workspace_id'], 'meshingress_workspace_owner_ix');
            $changed = true;
        }

        if (!$schema->hasTable('meshingress_workspace_sources')) {
            $table = $schema->createTable('meshingress_workspace_sources');
            $table->addColumn('id', Types::BIGINT, ['autoincrement' => true, 'unsigned' => true]);
            $table->addColumn('workspace_id', Types::STRING, ['length' => 128]);
            $table->addColumn('source_id', Types::STRING, ['length' => 40]);
            $table->addColumn('source_url', Types::TEXT);
            $table->addColumn('relative_path', Types::TEXT);
            $table->addColumn('content_type', Types::STRING, ['length' => 255, 'notnull' => false]);
            $table->addColumn('expected_byte_size', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->addColumn('expected_sha256', Types::STRING, ['length' => 64, 'notnull' => false]);
            $table->addColumn('state', Types::STRING, ['length' => 16]);
            $table->addColumn('attempts', Types::INTEGER, ['default' => 0, 'unsigned' => true]);
            $table->addColumn('error_message', Types::TEXT, ['notnull' => false]);
            $table->addColumn('file_id', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->addColumn('file_size', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->addColumn('mime_type', Types::STRING, ['length' => 255, 'notnull' => false]);
            $table->addColumn('checksum_sha256', Types::STRING, ['length' => 64, 'notnull' => false]);
            $table->addColumn('position', Types::INTEGER, ['unsigned' => true]);
            $table->addColumn('completed_at', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
            $table->setPrimaryKey(['id']);
            $table->addUniqueIndex(['source_id'], 'meshingress_source_id_uq');
            $table->addUniqueIndex(['workspace_id', 'relative_path'], 'meshingress_source_path_uq');
            $table->addIndex(['workspace_id', 'state', 'position'], 'meshingress_source_queue_ix');
            $changed = true;
        }

        return $changed ? $schema : null;
    }
}

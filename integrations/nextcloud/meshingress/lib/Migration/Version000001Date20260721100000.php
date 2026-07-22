<?php

declare(strict_types=1);

namespace OCA\Meshingress\Migration;

use Closure;
use Doctrine\DBAL\Types\Types;
use OCP\DB\ISchemaWrapper;
use OCP\Migration\IOutput;
use OCP\Migration\SimpleMigrationStep;

final class Version000001Date20260721100000 extends SimpleMigrationStep {
    #[\Override]
    public function changeSchema(IOutput $output, Closure $schemaClosure, array $options): ?ISchemaWrapper {
        /** @var ISchemaWrapper $schema */
        $schema = $schemaClosure();
        if ($schema->hasTable('meshingress_import_jobs')) {
            return null;
        }

        $table = $schema->createTable('meshingress_import_jobs');
        $table->addColumn('id', Types::BIGINT, ['autoincrement' => true, 'unsigned' => true]);
        $table->addColumn('job_id', Types::STRING, ['length' => 40]);
        $table->addColumn('uid', Types::STRING, ['length' => 64]);
        $table->addColumn('tool_id', Types::STRING, ['length' => 128]);
        $table->addColumn('source_url', Types::TEXT);
        $table->addColumn('relative_path', Types::TEXT);
        $table->addColumn('status', Types::STRING, ['length' => 16]);
        $table->addColumn('attempts', Types::INTEGER, ['default' => 0, 'unsigned' => true]);
        $table->addColumn('error_message', Types::TEXT, ['notnull' => false]);
        $table->addColumn('file_id', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
        $table->addColumn('file_size', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
        $table->addColumn('mime_type', Types::STRING, ['notnull' => false, 'length' => 255]);
        $table->addColumn('etag', Types::STRING, ['notnull' => false, 'length' => 255]);
        $table->addColumn('sha256', Types::STRING, ['notnull' => false, 'length' => 64]);
        $table->addColumn('created_at', Types::BIGINT, ['unsigned' => true]);
        $table->addColumn('updated_at', Types::BIGINT, ['unsigned' => true]);
        $table->addColumn('completed_at', Types::BIGINT, ['notnull' => false, 'unsigned' => true]);
        $table->setPrimaryKey(['id']);
        $table->addUniqueIndex(['job_id'], 'meshingress_job_id_uq');
        $table->addIndex(['uid', 'job_id'], 'meshingress_owner_job_ix');
        $table->addIndex(['status'], 'meshingress_status_ix');
        return $schema;
    }
}

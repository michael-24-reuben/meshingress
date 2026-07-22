package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceMetadataStoreTests {
    @Test
    void createWrapsJdbcFailuresAsStorageFailures() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:missing_storage_schema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        MeshingressProperties.Storage.Sql sql = new MeshingressProperties.Storage.Sql("missing", "storage_",
                new MeshingressProperties.Storage.Tables("entries", "usage", "events"), false);
        WorkspaceMetadataStore store = new WorkspaceMetadataStore(new JdbcTemplate(source), new TransactionTemplate(new DataSourceTransactionManager(source)), sql);
        OffsetDateTime now = OffsetDateTime.now();

        ToolStorageException failure = assertThrows(ToolStorageException.class,
                () -> store.create(new WorkspaceRecord("session-1", "request-1", "tool", WorkspaceState.STAGING, 0, 1, 0, now, now.plusMinutes(1)), 1));

        assertEquals("Storage metadata operation failed.", failure.getMessage());
    }

    @Test
    void archivesAnIncompatibleLegacyEntriesTableBeforeCreatingTheCurrentSchema() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:legacy_storage_schema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("create schema meshingress");
        jdbc.execute("create table meshingress.storage_entries (legacy_id varchar(64) not null)");
        MeshingressProperties.Storage.Sql sql = new MeshingressProperties.Storage.Sql("meshingress", "storage_",
                new MeshingressProperties.Storage.Tables("storage_entries", "storage_usage", "storage_events"), true);
        WorkspaceMetadataStore store = new WorkspaceMetadataStore(jdbc, new TransactionTemplate(new DataSourceTransactionManager(source)), sql);
        OffsetDateTime now = OffsetDateTime.now();

        store.create(new WorkspaceRecord("session-1", "request-1", "tool", WorkspaceState.STAGING, 0, 1, 0, now, now.plusMinutes(1)), 1);

        assertEquals(1, jdbc.queryForObject("select count(*) from meshingress.storage_entries", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.tables where table_schema = 'MESHINGRESS' and table_name = 'STORAGE_ENTRIES_LEGACY_1'", Integer.class));
    }
}

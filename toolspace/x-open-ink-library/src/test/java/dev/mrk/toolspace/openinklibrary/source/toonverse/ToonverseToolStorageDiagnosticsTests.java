package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToonverseToolStorageDiagnosticsTests {
    @Test
    void storageFailureMessageExposesOnlyTheSafeSqlState() {
        String message = ToonverseTool.storageFailureMessage(new ToolStorageException("Storage metadata operation failed.", new SQLException("relation details must stay server-side", "42S02")));
        assertEquals("Storage metadata operation failed. [database SQLState=42S02]", message);
    }

    @Test
    void storageFailureMessageSafelyNamesTheH2NullColumn() {
        String message = ToonverseTool.storageFailureMessage(new ToolStorageException("Storage metadata operation failed.", new SQLException("NULL not allowed for column \"LEGACY_ID\"; SQL statement must stay server-side", "23502")));
        assertEquals("Storage metadata operation failed. [database SQLState=23502, null column=legacy_id]", message);
    }
}

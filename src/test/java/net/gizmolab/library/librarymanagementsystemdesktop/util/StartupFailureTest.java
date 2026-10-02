package net.gizmolab.library.librarymanagementsystemdesktop.util;

import com.zaxxer.hikari.HikariDataSource;
import net.gizmolab.library.librarymanagementsystemdesktop.config.DataSourceConfig;
import net.gizmolab.library.librarymanagementsystemdesktop.service.KeyStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** When the application cannot start, the librarian gets a sentence that says why, not a stack trace. */
class StartupFailureTest {

    @TempDir Path dir;

    private HikariDataSource encrypted(String dek) {
        KeyStoreService keyStore = new KeyStoreService();
        ReflectionTestUtils.setField(keyStore, "inMemoryOnly", true);
        keyStore.init();
        keyStore.storeSecret(KeyStoreService.KEY_DEK, dek);
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "keyStoreService", keyStore);
        ReflectionTestUtils.setField(config, "h2Path", dir.resolve("library").toString());
        ReflectionTestUtils.setField(config, "encryptionEnabled", true);
        return (HikariDataSource) config.dataSource();
    }

    @Test
    void aDatabaseThatDoesNotOpenWithTheKeystoreKeyIsExplained() throws Exception {
        try (HikariDataSource first = encrypted("dek-one")) {
            first.getConnection().close();
        }
        Exception wrongKey = assertThrows(Exception.class, () -> encrypted("dek-two"));
        String message = StartupFailure.message(new IllegalStateException("context failed", new RuntimeException(wrongKey)));
        assertTrue(message.contains("κλειδί"), message);
    }

    @Test
    void aDatabaseFromAnOlderVersionIsExplained() {
        Exception e = new IllegalStateException("context failed", new SQLException(
                "Unsupported database file version or invalid file header in file \"data/library.mv.db\"", "90048", 90048));
        assertTrue(StartupFailure.message(e).contains("παλαιότερη"), StartupFailure.message(e));
    }

    @Test
    void aDatabaseAlreadyInUseIsExplained() {
        Exception e = new RuntimeException(new SQLException("Database may be already in use: \"data/library.mv.db\"", "90020", 90020));
        assertTrue(StartupFailure.message(e).contains("ήδη ανοιχτή"), StartupFailure.message(e));
    }

    @Test
    void anythingElseSaysThatTheApplicationCouldNotStartWithTheDetail() {
        String message = StartupFailure.message(new IllegalStateException("context failed", new RuntimeException("boom")));
        assertTrue(message.startsWith("Η εφαρμογή δεν μπόρεσε να ξεκινήσει"), message);
        assertTrue(message.contains("boom"), message);
    }

    @Test
    void theDataSourceOpensTheDatabaseAtStartupSoTheRealCauseIsKept() throws Exception {
        // Hibernate 7 hides a failed connection behind "Unable to determine Dialect"; opening it here keeps H2's cause
        try (HikariDataSource first = encrypted("dek-one")) {
            first.getConnection().close();
        }
        Exception atStartup = assertThrows(Exception.class, () -> encrypted("dek-two").close());
        assertTrue(StartupFailure.message(atStartup).contains("κλειδί"), StartupFailure.message(atStartup));
    }

    @Test
    void noExceptionStillGivesASentence() {
        assertEquals("Η εφαρμογή δεν μπόρεσε να ξεκινήσει.", StartupFailure.message(null));
    }
}

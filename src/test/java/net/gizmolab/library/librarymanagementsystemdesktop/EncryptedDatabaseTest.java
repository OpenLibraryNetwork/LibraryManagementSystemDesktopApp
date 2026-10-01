package net.gizmolab.library.librarymanagementsystemdesktop;

import com.zaxxer.hikari.HikariDataSource;
import net.gizmolab.library.librarymanagementsystemdesktop.config.DataSourceConfig;
import net.gizmolab.library.librarymanagementsystemdesktop.service.KeyStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The real configuration: an AES-encrypted H2 file opened with the DEK from the keystore.
 * The test profile never takes this path, so it is exercised here on a temporary file.
 */
class EncryptedDatabaseTest {

    @TempDir Path dir;

    private static KeyStoreService keystoreWithDek(String dek) {
        KeyStoreService keyStore = new KeyStoreService();
        ReflectionTestUtils.setField(keyStore, "inMemoryOnly", true);
        keyStore.init();
        keyStore.storeSecret(KeyStoreService.KEY_DEK, dek);
        return keyStore;
    }

    private DataSource encrypted(String dek) {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "keyStoreService", keystoreWithDek(dek));
        ReflectionTestUtils.setField(config, "h2Path", dir.resolve("library").toString());
        ReflectionTestUtils.setField(config, "encryptionEnabled", true);
        return config.dataSource();
    }

    @Test
    void theEncryptedFileOpensAgainWithTheSameKeyOnly() throws Exception {
        DataSource first = encrypted("dek-one");
        try (Connection c = first.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE probe(v VARCHAR(10))");
            s.execute("INSERT INTO probe VALUES ('ok')");
        }
        ((HikariDataSource) first).close();
        byte[] header = java.util.Arrays.copyOf(java.nio.file.Files.readAllBytes(dir.resolve("library.mv.db")), 9);
        assertEquals("H2encrypt", new String(header, java.nio.charset.StandardCharsets.US_ASCII), "the file must be AES-encrypted");

        DataSource again = encrypted("dek-one");
        try (Connection c = again.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT v FROM probe")) {
            assertTrue(rs.next());
            assertEquals("ok", rs.getString(1));
        }
        ((HikariDataSource) again).close();

        // the data source opens the file at once, so a wrong key fails right there
        IllegalStateException wrongKey = assertThrows(IllegalStateException.class, () -> encrypted("dek-two"));
        assertTrue(hasSqlCause(wrongKey), "H2's SQLException must stay in the cause chain");
    }

    private static boolean hasSqlCause(Throwable t) {
        for (; t != null; t = t.getCause()) if (t instanceof SQLException) return true;
        return false;
    }
}

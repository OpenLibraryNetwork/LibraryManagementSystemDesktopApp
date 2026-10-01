package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.KeyStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/** Tests must never open ./data/library or the OS keystore that the running application uses. */
@SpringBootTest
class TestIsolationTest {

    @Autowired private DataSource dataSource;
    @Autowired private KeyStoreService keyStoreService;

    @Test
    void theTestProfileUsesAnInMemoryDatabase() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            String url = c.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:h2:mem:"), url);
        }
    }

    @Test
    void theTestProfileDoesNotUseTheOsKeystore() {
        assertFalse(keyStoreService.isAvailable());
        keyStoreService.storeSecret("probe", "v");
        assertEquals("v", keyStoreService.getSecret("probe"));
    }
}

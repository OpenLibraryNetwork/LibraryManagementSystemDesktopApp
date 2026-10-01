package net.gizmolab.library.librarymanagementsystemdesktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Review Focus 1: the test-only switches must never reach the real configuration. */
class MainConfigTest {

    @Test
    void mainConfigurationKeepsTheOsKeystoreAndTheEncryptedFileDatabase() throws IOException {
        String props = Files.readString(Path.of("src/main/resources/application.properties"));
        assertFalse(props.contains("app.keystore.in-memory"), "in-memory keystore belongs to the test profile only");
        assertFalse(props.contains("mem:"), "in-memory database belongs to the test profile only");
        assertTrue(props.contains("app.h2.encryption.enabled=true"));
    }
}

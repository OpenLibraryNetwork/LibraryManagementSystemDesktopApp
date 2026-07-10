package net.gizmolab.library.librarymanagementsystemdesktop.config;

import net.gizmolab.library.librarymanagementsystemdesktop.service.KeyStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Dynamic H2 DataSource with encryption.
 *
 * H2 encryption:
 * - URL: jdbc:h2:file:./data/library;CIPHER=AES
 * - Password: "DEK sa" (H2 format: "filePassword userPassword")
 *   where DEK comes from OS Keystore and "sa" is the H2 user password
 *
 * First run:
 * - KeyStoreService generates a random 256-bit DEK
 * - DEK stored in OS Keystore
 * - H2 database created with encryption
 *
 * Subsequent runs:
 * - DEK retrieved from OS Keystore
 * - H2 connected with existing encrypted database
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    private KeyStoreService keyStoreService;

    @Value("${app.h2.path:./data/library}")
    private String h2Path;

    @Value("${app.h2.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (!encryptionEnabled) {
            log.warn("H2 encryption DISABLED (dev mode) — data stored in plaintext");
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:file:" + h2Path + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;FILE_LOCK=NO")
                    .username("sa")
                    .password("")
                    .build();
        }

        // Get or create DEK
        String dek = keyStoreService.getOrCreateDEK();

        // H2 encrypted connection
        // Password format: "filePassword userPassword" (space-separated)
        // filePassword = DEK (used for AES encryption of the file)
        // userPassword = "sa" (the H2 login password, can be empty but we use "sa")
        String h2Url = "jdbc:h2:file:" + h2Path
                + ";CIPHER=AES;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;FILE_LOCK=NO";
        String h2Password = dek + " sa";

        log.info("Configured encrypted H2 database at: {}", h2Path);

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url(h2Url)
                .username("sa")
                .password(h2Password)
                .build();
    }
}

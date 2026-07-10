package net.gizmolab.library.librarymanagementsystemdesktop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Encrypted backup service using RSA Recovery Keys.
 *
 * DESIGN:
 * - RSA keypair (2048-bit) is SEPARATE from the DEK
 * - Public key stays on the machine (used to encrypt backups)
 * - Private key is given to the admin (stored OFFLINE)
 * - If DEK is lost, admin can decrypt backup with private key → restore
 *
 * ENCRYPTION SCHEME (hybrid):
 * - Data encrypted with random AES-256-GCM session key
 * - Session key encrypted with RSA public key (RSA/ECB/OAEPWithSHA-256AndMGF1Padding)
 * - Backup = {encrypted_data.enc, session_key.enc, backup_metadata.json}
 *
 * 15 libraries → 15 RSA keypairs, one central admin holds ALL private keys.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_SIZE = 12;
    private static final int GCM_TAG_SIZE = 128;

    @Autowired
    private KeyStoreService keyStoreService;

    @Value("${app.backup.dir:./backups}")
    private String backupDir;

    @Value("${app.h2.path:./data/library}")
    private String h2Path;

    private PublicKey rsaPublicKey;

    // ═══════════════════════════════════════════════════════
    // Initialization — RSA Keypair
    // ═══════════════════════════════════════════════════════

    /**
     * Initialize the backup system. On first run:
     * - Generate RSA keypair
     * - Store public key locally
     * - Return private key (PEM) for admin to save offline
     *
     * On subsequent runs:
     * - Load public key from local storage
     *
     * @return PEM-encoded private key (ONLY on first run), or null if already initialized
     */
    public String initialize() {
        Path publicKeyPath = Path.of(backupDir, "recovery_public_key.pem");

        try {
            Files.createDirectories(Path.of(backupDir));

            if (Files.exists(publicKeyPath)) {
                // Load existing public key
                String publicKeyPem = Files.readString(publicKeyPath);
                rsaPublicKey = loadPublicKeyFromPem(publicKeyPem);
                log.info("Loaded existing recovery public key from {}", publicKeyPath);
                return null; // Already initialized
            }

            // First run — generate keypair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            rsaPublicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // Save public key locally
            String publicKeyPem = encodePublicKeyToPem(rsaPublicKey);
            Files.writeString(publicKeyPath, publicKeyPem);
            log.info("Generated new RSA recovery keypair. Public key saved to: {}", publicKeyPath);

            // Return private key as PEM — caller must display/export for admin
            String privateKeyPem = encodePrivateKeyToPem(privateKey);

            log.warn("╔══════════════════════════════════════════════════╗");
            log.warn("║  RECOVERY PRIVATE KEY GENERATED                  ║");
            log.warn("║  Save this key securely — it cannot be recovered! ║");
            log.warn("╚══════════════════════════════════════════════════╝");

            return privateKeyPem;

        } catch (Exception e) {
            log.error("Failed to initialize backup system: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if the backup system is initialized (public key exists).
     */
    public boolean isInitialized() {
        return rsaPublicKey != null ||
            Files.exists(Path.of(backupDir, "recovery_public_key.pem"));
    }

    // ═══════════════════════════════════════════════════════
    // Backup — Encrypt & Save
    // ═══════════════════════════════════════════════════════

    /**
     * Perform encrypted backup of the H2 database.
     *
     * @return path to the backup directory, or null on failure
     */
    public String performBackup() {
        if (rsaPublicKey == null) {
            initialize(); // Try to load
            if (rsaPublicKey == null) {
                log.error("Cannot perform backup — recovery key not initialized");
                return null;
            }
        }

        try {
            // Timestamp for unique backup name
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = Path.of(backupDir, "backup_" + timestamp);
            Files.createDirectories(backupPath);

            // Step 1: H2 SQL dump
            Path dumpPath = backupPath.resolve("dump.sql");
            performH2Dump(dumpPath);

            // Step 2: GZIP compress
            byte[] compressed = gzipCompress(Files.readAllBytes(dumpPath));
            Files.delete(dumpPath); // Remove uncompressed dump

            // Step 3: Generate random AES session key
            KeyGenerator aesKeyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            aesKeyGen.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey sessionKey = aesKeyGen.generateKey();

            // Step 4: AES-GCM encrypt the compressed data
            byte[] iv = new byte[GCM_IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher aesCipher = Cipher.getInstance(AES_CIPHER);
            aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_SIZE, iv));
            byte[] encryptedData = aesCipher.doFinal(compressed);

            // Write IV + encrypted data
            Path encDataPath = backupPath.resolve("data.enc");
            try (FileOutputStream fos = new FileOutputStream(encDataPath.toFile())) {
                fos.write(iv);
                fos.write(encryptedData);
            }

            // Step 5: RSA encrypt the session key
            Cipher rsaCipher = Cipher.getInstance(RSA_CIPHER);
            rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            byte[] encryptedSessionKey = rsaCipher.doFinal(sessionKey.getEncoded());

            Path encKeyPath = backupPath.resolve("session_key.enc");
            Files.write(encKeyPath, encryptedSessionKey);

            // Metadata
            Path metaPath = backupPath.resolve("backup_metadata.json");
            String metadata = String.format(
                "{\"timestamp\":\"%s\",\"h2Path\":\"%s\",\"rsaKeySize\":%d,\"aesKeySize\":%d}",
                timestamp, h2Path, RSA_KEY_SIZE, AES_KEY_SIZE);
            Files.writeString(metaPath, metadata);

            log.info("Backup completed successfully: {}", backupPath);
            return backupPath.toString();

        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════
    // Restore — Decrypt & Import
    // ═══════════════════════════════════════════════════════

    /**
     * Restore from encrypted backup using admin's RSA private key.
     *
     * @param backupDirPath path to backup directory (contains data.enc, session_key.enc)
     * @param privateKeyPem PEM-encoded RSA private key from admin
     * @return true if restore successful
     */
    public boolean restoreFromBackup(String backupDirPath, String privateKeyPem) {
        try {
            Path backupPath = Path.of(backupDirPath);

            // Load private key
            PrivateKey privateKey = loadPrivateKeyFromPem(privateKeyPem);

            // Step 1: RSA decrypt session key
            byte[] encryptedSessionKey = Files.readAllBytes(backupPath.resolve("session_key.enc"));
            Cipher rsaCipher = Cipher.getInstance(RSA_CIPHER);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] sessionKeyBytes = rsaCipher.doFinal(encryptedSessionKey);
            SecretKey sessionKey = new SecretKeySpec(sessionKeyBytes, AES_ALGORITHM);

            // Step 2: AES-GCM decrypt data
            byte[] encDataWithIv = Files.readAllBytes(backupPath.resolve("data.enc"));
            byte[] iv = new byte[GCM_IV_SIZE];
            System.arraycopy(encDataWithIv, 0, iv, 0, GCM_IV_SIZE);
            byte[] encData = new byte[encDataWithIv.length - GCM_IV_SIZE];
            System.arraycopy(encDataWithIv, GCM_IV_SIZE, encData, 0, encData.length);

            Cipher aesCipher = Cipher.getInstance(AES_CIPHER);
            aesCipher.init(Cipher.DECRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_SIZE, iv));
            byte[] compressed = aesCipher.doFinal(encData);

            // Step 3: GZIP decompress
            byte[] sqlDump = gzipDecompress(compressed);

            // Step 4: Generate new DEK and create fresh H2
            String newDek = keyStoreService.generateAndStoreDEK();
            log.info("Generated new DEK for restored database");

            // Step 5: Import SQL dump into new H2
            Path restoreDumpPath = backupPath.resolve("restore_dump.sql");
            Files.write(restoreDumpPath, sqlDump);
            importH2Dump(restoreDumpPath, newDek);
            Files.deleteIfExists(restoreDumpPath);

            log.info("Database restored successfully from backup: {}", backupDirPath);
            return true;

        } catch (Exception e) {
            log.error("Restore from backup failed: {}", e.getMessage(), e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════
    // H2 Operations
    // ═══════════════════════════════════════════════════════

    private void performH2Dump(Path outputPath) throws Exception {
        String dek = keyStoreService.getSecret(KeyStoreService.KEY_DEK);
        String jdbcUrl = "jdbc:h2:file:" + h2Path + ";CIPHER=AES";
        String password = (dek != null ? dek : "") + " sa";

        try (var conn = java.sql.DriverManager.getConnection(jdbcUrl, "sa", password);
             var stmt = conn.createStatement()) {
            stmt.execute("SCRIPT TO '" + outputPath.toAbsolutePath() + "'");
        }
        log.debug("H2 dump written to: {}", outputPath);
    }

    private void importH2Dump(Path dumpPath, String newDek) throws Exception {
        String jdbcUrl = "jdbc:h2:file:" + h2Path + "_restored;CIPHER=AES";
        String password = newDek + " sa";

        try (var conn = java.sql.DriverManager.getConnection(jdbcUrl, "sa", password);
             var stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM '" + dumpPath.toAbsolutePath() + "'");
        }
        log.debug("H2 dump imported from: {}", dumpPath);
    }

    // ═══════════════════════════════════════════════════════
    // Compression
    // ═══════════════════════════════════════════════════════

    private byte[] gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] gzipDecompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════
    // PEM Encoding/Decoding
    // ═══════════════════════════════════════════════════════

    private String encodePublicKeyToPem(PublicKey key) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
    }

    private String encodePrivateKeyToPem(PrivateKey key) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    private PublicKey loadPublicKeyFromPem(String pem) throws Exception {
        String base64 = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(RSA_ALGORITHM)
            .generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        String base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(RSA_ALGORITHM)
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}

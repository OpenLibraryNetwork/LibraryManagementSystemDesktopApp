package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * OS Keystore service for secure secret storage.
 * Uses java-keyring library for cross-platform support:
 * - Windows: Credential Store
 * - macOS: Keychain
 * - Linux: Secret Service (GNOME Keyring / KDE Wallet)
 *
 * Stores 4 secrets under SERVICE_NAME:
 * - "dek"       → Data Encryption Key for H2 (256-bit, Base64)
 * - "jwt"       → Strapi JWT token
 * - "libraryId" → Strapi library ID (as String)
 * - "strapiUrl" → Strapi base URL
 */
@Service
public class KeyStoreService {

    private static final Logger log = LoggerFactory.getLogger(KeyStoreService.class);
    private static final String SERVICE_NAME = "LibraryManagementSystem";

    // Key names
    public static final String KEY_DEK = "dek";
    public static final String KEY_JWT = "jwt";
    public static final String KEY_LIBRARY_ID = "libraryId";
    public static final String KEY_STRAPI_URL = "strapiUrl";

    private Keyring keyring;
    private boolean available = false;

    // In-memory fallback (development/testing only — NOT SECURE for production)
    private final java.util.Map<String, String> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            keyring = Keyring.create();
            available = true;
            log.info("OS Keystore initialized successfully");
        } catch (Exception e) {
            log.warn("OS Keystore not available, falling back to in-memory storage: {}",
                e.getMessage());
            available = false;
        }
    }

    /**
     * Store a secret in the OS keystore.
     */
    public void storeSecret(String key, String value) {
        if (!available) {
            log.warn("Keystore not available — secret '{}' stored in-memory only (NOT SECURE)", key);
            inMemoryStore.put(key, value);
            return;
        }
        try {
            keyring.setPassword(SERVICE_NAME, key, value);
            log.debug("Stored secret '{}' in OS keystore", key);
        } catch (PasswordAccessException e) {
            log.error("Failed to store secret '{}' in OS keystore: {}", key, e.getMessage());
            // Fallback to in-memory
            inMemoryStore.put(key, value);
        }
    }

    /**
     * Retrieve a secret from the OS keystore.
     * @return the secret value, or null if not found
     */
    public String getSecret(String key) {
        if (!available) {
            return inMemoryStore.get(key);
        }
        try {
            return keyring.getPassword(SERVICE_NAME, key);
        } catch (PasswordAccessException e) {
            log.debug("Secret '{}' not found in OS keystore", key);
            return inMemoryStore.get(key); // Try fallback
        }
    }

    /**
     * Delete a secret from the OS keystore.
     */
    public void deleteSecret(String key) {
        if (!available) {
            inMemoryStore.remove(key);
            return;
        }
        try {
            keyring.deletePassword(SERVICE_NAME, key);
            log.debug("Deleted secret '{}' from OS keystore", key);
        } catch (PasswordAccessException e) {
            log.debug("Secret '{}' not found for deletion", key);
        }
        inMemoryStore.remove(key);
    }

    /**
     * Check if a secret exists in the OS keystore.
     */
    public boolean hasSecret(String key) {
        return getSecret(key) != null;
    }

    /**
     * Generate a cryptographically secure random DEK (256-bit)
     * and store it in the keystore.
     * @return the generated DEK as Base64 string
     */
    public String generateAndStoreDEK() {
        SecureRandom random = new SecureRandom();
        byte[] dekBytes = new byte[32]; // 256 bits
        random.nextBytes(dekBytes);
        String dek = Base64.getEncoder().encodeToString(dekBytes);
        storeSecret(KEY_DEK, dek);
        log.info("Generated and stored new DEK (256-bit)");
        return dek;
    }

    /**
     * Get the DEK, or generate one if it doesn't exist (first run).
     * @return DEK as Base64 string
     */
    public String getOrCreateDEK() {
        String dek = getSecret(KEY_DEK);
        if (dek == null || dek.isEmpty()) {
            dek = generateAndStoreDEK();
        }
        return dek;
    }

    /**
     * Clear all stored secrets (full logout/reset).
     */
    public void clearAll() {
        deleteSecret(KEY_JWT);
        deleteSecret(KEY_LIBRARY_ID);
        deleteSecret(KEY_STRAPI_URL);
        // NOTE: DEK is NOT cleared — it's needed to access existing H2 data
        log.info("Cleared auth secrets from keystore (DEK preserved)");
    }

    /**
     * Check if the keystore backend is available.
     */
    public boolean isAvailable() {
        return available;
    }
}

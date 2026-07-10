package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Authentication service for Strapi integration.
 *
 * Login flow:
 * 1. POST /api/auth/local with {identifier, password}
 * 2. Strapi returns {jwt, user: {id, library: {id}}}
 * 3. Store JWT + libraryId + strapiUrl in OS Keystore
 *
 * Startup flow:
 * 1. Try to restore JWT + libraryId + strapiUrl from keystore
 * 2. If found, set authenticated state (JWT validity checked on first API call)
 * 3. If not found, show login screen
 *
 * NOTE: Password is NEVER stored. JWT has 90-day expiry.
 * If JWT expires, user must re-login manually.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private KeyStoreService keyStoreService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // In-memory state (loaded from keystore on startup)
    private String jwt;
    private Long libraryId;
    private String strapiBaseUrl;
    private Long userId;
    private boolean online = false;

    public AuthService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Try to restore session from OS Keystore on startup.
     */
    @PostConstruct
    public void init() {
        try {
            String storedJwt = keyStoreService.getSecret(KeyStoreService.KEY_JWT);
            String storedLibraryId = keyStoreService.getSecret(KeyStoreService.KEY_LIBRARY_ID);
            String storedUrl = keyStoreService.getSecret(KeyStoreService.KEY_STRAPI_URL);

            if (storedJwt != null && storedLibraryId != null && storedUrl != null) {
                this.jwt = storedJwt;
                this.libraryId = Long.parseLong(storedLibraryId);
                this.strapiBaseUrl = storedUrl;
                this.online = testConnection();
                log.info("Restored session from keystore — library={}, online={}", libraryId, online);
            } else {
                log.info("No stored session found — login required");
            }
        } catch (Exception e) {
            log.warn("Failed to restore session from keystore: {}", e.getMessage());
        }
    }

    /**
     * Login to Strapi and obtain JWT.
     *
     * POST /api/auth/local
     * Body: {"identifier": "user@example.com", "password": "pass"}
     * Response: {"jwt": "...", "user": {"id": 1, "library": {"id": 3}}}
     *
     * @param url Strapi base URL (e.g., "http://localhost:1337")
     * @param username Strapi username/email
     * @param password Strapi password
     * @return true if login successful
     */
    public boolean login(String url, String username, String password) {
        try {
            // Normalize URL
            String baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            // Build request body
            String body = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", username, "password", password)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/local"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Login failed — status {}: {}", response.statusCode(), response.body());
                return false;
            }

            // Parse response
            JsonNode json = objectMapper.readTree(response.body());

            // Extract JWT
            String newJwt = json.path("jwt").asText(null);
            if (newJwt == null || newJwt.isEmpty()) {
                log.error("Login response missing JWT");
                return false;
            }

            // Extract user info
            JsonNode userNode = json.path("user");
            Long newUserId = userNode.path("id").asLong(0);

            // Extract library ID (user.library.id or user.library — depends on populate)
            Long newLibraryId = null;
            JsonNode libraryNode = userNode.path("library");
            if (libraryNode.isObject()) {
                newLibraryId = libraryNode.path("id").asLong(0);
            } else if (libraryNode.isNumber()) {
                newLibraryId = libraryNode.asLong();
            }

            if (newLibraryId == null || newLibraryId == 0) {
                log.error("User has no assigned library — cannot proceed");
                return false;
            }

            // Store in memory
            this.jwt = newJwt;
            this.libraryId = newLibraryId;
            this.strapiBaseUrl = baseUrl;
            this.userId = newUserId;
            this.online = true;

            // Persist to OS Keystore
            keyStoreService.storeSecret(KeyStoreService.KEY_JWT, newJwt);
            keyStoreService.storeSecret(KeyStoreService.KEY_LIBRARY_ID, String.valueOf(newLibraryId));
            keyStoreService.storeSecret(KeyStoreService.KEY_STRAPI_URL, baseUrl);

            log.info("Login successful — user={}, library={}", newUserId, newLibraryId);
            return true;

        } catch (java.net.ConnectException e) {
            log.error("Cannot connect to Strapi at {}: {}", url, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if the user is currently authenticated.
     * Note: This checks local state only. JWT may be expired server-side.
     * Server-side expiry triggers 401 → StrapiApiClient.AuthenticationExpiredException.
     */
    public boolean isAuthenticated() {
        return jwt != null && !jwt.isEmpty();
    }

    /**
     * Test connection to Strapi with the current JWT.
     */
    private boolean testConnection() {
        if (jwt == null || strapiBaseUrl == null) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(strapiBaseUrl + "/api/users/me"))
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if we have internet connectivity to Strapi.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Refresh online status by testing connection.
     */
    public void refreshOnlineStatus() {
        this.online = testConnection();
    }

    /**
     * Get the current JWT token.
     */
    public String getJwt() {
        return jwt;
    }

    /**
     * Get the authenticated user's library ID.
     */
    public Long getLibraryId() {
        return libraryId;
    }

    /**
     * Get the Strapi base URL.
     */
    public String getStrapiBaseUrl() {
        return strapiBaseUrl;
    }

    /**
     * Get the authenticated user's Strapi ID.
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Logout — clear JWT and library from memory and keystore.
     * DEK is preserved (needed for H2 access).
     */
    public void logout() {
        this.jwt = null;
        this.libraryId = null;
        this.userId = null;
        this.online = false;
        // Don't clear strapiBaseUrl — keep as default for next login

        keyStoreService.clearAll();
        log.info("Logged out — auth secrets cleared, DEK preserved");
    }
}

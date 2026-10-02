package net.gizmolab.library.librarymanagementsystemdesktop.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
 * 2. Strapi returns {jwt, user: {id, role: {type}, library: {documentId}}}
 * 3. Only the Librarian role may log in
 * 4. Store JWT + library documentId + strapiUrl in OS Keystore
 *
 * Startup flow:
 * 1. Try to restore JWT + library documentId + strapiUrl from keystore
 *    (a Strapi 4 session, which has only the numeric library id, is cleared)
 * 2. If found, set authenticated state (JWT validity checked on first API call)
 * 3. If not found, show login screen
 *
 * NOTE: Password is NEVER stored. JWT has 90-day expiry.
 * If JWT expires, user must re-login manually.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    // Written on login/logout, read by background tasks: volatile so they see the current session
    private static volatile AuthService instance;

    @Autowired
    private KeyStoreService keyStoreService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // In-memory state (loaded from keystore on startup)
    private volatile String jwt;
    private volatile String libraryDocumentId;
    private volatile String strapiBaseUrl;
    private volatile Long userId;
    private volatile boolean online = false;

    /** Outcome of a login attempt, with the Greek message the login screen shows. */
    public enum LoginResult {
        SUCCESS(null),
        BAD_CREDENTIALS("Λάθος όνομα χρήστη ή κωδικός."),
        NOT_LIBRARIAN("Ο λογαριασμός δεν είναι βιβλιοθηκονόμου. Επικοινωνήστε με τον διαχειριστή."),
        NO_LIBRARY("Ο λογαριασμός δεν έχει βιβλιοθήκη. Ζητήστε από τον διαχειριστή να σας αντιστοιχίσει σε βιβλιοθήκη."),
        UNREACHABLE("Δεν υπάρχει σύνδεση με τον server."),
        INSECURE_URL("Η διεύθυνση του server πρέπει να ξεκινά με https:// (http:// επιτρέπεται μόνο για server σε αυτόν τον υπολογιστή)."),
        FAILED("Η σύνδεση απέτυχε.");

        private final String message;

        LoginResult(String message) { this.message = message; }

        public String getMessage() { return message; }
    }

    public AuthService() {
        this.httpClient = HttpClient.newBuilder()
                // HTTP/1.1 only: the default HTTP/2 client sends "Upgrade: h2c", which Strapi 5 in develop mode never answers
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
    }

    /**
     * Try to restore session from OS Keystore on startup.
     */
    @PostConstruct
    public void init() {
        instance = this;
        try {
            String storedJwt = keyStoreService.getSecret(KeyStoreService.KEY_JWT);
            String storedLibraryDocumentId = keyStoreService.getSecret(KeyStoreService.KEY_LIBRARY_DOCUMENT_ID);
            String storedUrl = keyStoreService.getSecret(KeyStoreService.KEY_STRAPI_URL);

            if (storedJwt != null && storedLibraryDocumentId == null) {
                // A Strapi 4 session (numeric library id only): its token and ids mean nothing to Strapi 5
                log.info("Stored session is from Strapi 4 — login required");
                logout();
                return;
            }
            if (storedUrl != null && !isAllowedServerUrl(storedUrl)) {
                // Saved before https became mandatory: the token must not travel unencrypted again
                log.info("Stored session uses plain http to a remote server — login required");
                logout();
                return;
            }
            if (storedJwt != null && storedUrl != null) {
                this.jwt = storedJwt;
                this.libraryDocumentId = storedLibraryDocumentId;
                this.strapiBaseUrl = storedUrl;
                int status = sessionCheckStatus();
                if (status == 401) {
                    // Token rejected (expired, or the Strapi database was recreated): force a new login
                    log.info("Stored session rejected by the server — login required");
                    logout();
                    return;
                }
                this.online = status == 200;
                log.info("Restored session from keystore — library={}, online={}", libraryDocumentId, online);
            } else {
                log.info("No stored session found — login required");
            }
        } catch (Exception e) {
            log.warn("Failed to restore session from keystore: {}", e.getMessage());
        }
    }

    /**
     * https everywhere; plain http only for a server on this computer,
     * so the password and the token never cross the network unencrypted.
     */
    static boolean isAllowedServerUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return false;
            if (scheme.equalsIgnoreCase("https")) return true;
            return scheme.equalsIgnoreCase("http")
                    && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("[::1]"));
        } catch (java.net.URISyntaxException _) {
            return false;
        }
    }

    /**
     * Login to Strapi and obtain JWT.
     *
     * POST /api/auth/local
     * Body: {"identifier": "user@example.com", "password": "pass"}
     * Response: {"jwt": "...", "user": {"id": 1, "role": {"type": "librarian"}, "library": {"documentId": "..."}}}
     *
     * @param url Strapi base URL (e.g., "http://localhost:1337")
     * @param username Strapi username/email
     * @param password Strapi password
     * @return the outcome; only SUCCESS stores a session
     */
    public LoginResult login(String url, String username, String password) {
        try {
            // Normalize URL
            String baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            if (!isAllowedServerUrl(baseUrl)) {
                log.warn("Login refused: {} is neither https nor a local server", baseUrl);
                return LoginResult.INSECURE_URL;
            }

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
            int status = response.statusCode();
            String responseBody = response.body();

            if (status == 400 || status == 401) {
                log.warn("Login rejected — status {}: {}", status, responseBody);
                return LoginResult.BAD_CREDENTIALS;
            }
            if (status != 200) {
                log.warn("Login failed — status {}: {}", status, responseBody);
                return LoginResult.FAILED;
            }

            // Parse response
            JsonNode json = objectMapper.readTree(response.body());

            // Extract JWT
            String newJwt = json.path("jwt").asString(null);
            if (newJwt == null || newJwt.isEmpty()) {
                log.error("Login response missing JWT");
                return LoginResult.FAILED;
            }

            // Extract user info
            JsonNode userNode = json.path("user");
            Long newUserId = userNode.path("id").asLong(0);

            // Only librarians use the desktop app; checked before anything is stored
            if (!"librarian".equals(userNode.path("role").path("type").asString())) {
                log.warn("Login refused — user {} is not a librarian", newUserId);
                return LoginResult.NOT_LIBRARIAN;
            }

            String newLibraryDocumentId = userNode.path("library").path("documentId").asString(null);
            if (newLibraryDocumentId == null || newLibraryDocumentId.isEmpty()) {
                log.error("User has no assigned library — cannot proceed");
                return LoginResult.NO_LIBRARY;
            }

            // Store in memory
            this.jwt = newJwt;
            this.libraryDocumentId = newLibraryDocumentId;
            this.strapiBaseUrl = baseUrl;
            this.userId = newUserId;
            this.online = true;

            // Persist to OS Keystore
            keyStoreService.storeSecret(KeyStoreService.KEY_JWT, newJwt);
            keyStoreService.storeSecret(KeyStoreService.KEY_LIBRARY_DOCUMENT_ID, newLibraryDocumentId);
            keyStoreService.storeSecret(KeyStoreService.KEY_STRAPI_URL, baseUrl);

            log.info("Login successful — user={}, library={}", newUserId, newLibraryDocumentId);
            return LoginResult.SUCCESS;

        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            log.error("Cannot connect to Strapi at {}: {}", url, e.getMessage());
            return LoginResult.UNREACHABLE;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Login failed: {}", e.getMessage(), e);
            return LoginResult.FAILED;
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
     * HTTP status of GET /api/users/me with the current JWT, or -1 when the server cannot be reached.
     */
    private int sessionCheckStatus() {
        if (jwt == null || strapiBaseUrl == null) return -1;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(strapiBaseUrl + "/api/users/me"))
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("Connection test failed: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Check if we have internet connectivity to Strapi.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Get the current JWT token.
     */
    public String getJwt() {
        return jwt;
    }

    /** The authenticated user's library documentId (Strapi 5). */
    public String getLibraryDocumentId() {
        return libraryDocumentId;
    }

    /** The authenticated user's library documentId (Strapi 5), or null when there is no session. */
    public static String getCurrentLibraryDocumentId() {
        return instance != null ? instance.libraryDocumentId : null;
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
        this.libraryDocumentId = null;
        this.userId = null;
        this.online = false;
        // Don't clear strapiBaseUrl — keep as default for next login

        keyStoreService.clearAll();
        log.info("Logged out — auth secrets cleared, DEK preserved");
    }
}

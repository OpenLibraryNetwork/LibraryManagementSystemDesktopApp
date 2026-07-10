package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Central HTTP client for Strapi REST API communication.
 *
 * Features:
 * - Automatic JWT injection in every request
 * - 401 detection (throws AuthenticationExpiredException for UI to handle)
 * - JSON parsing via Jackson
 * - Strapi v4 response format: { "data": { "id": N, "attributes": {...} } }
 */
@Service
public class StrapiApiClient {

    private final AuthService authService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public StrapiApiClient(AuthService authService) {
        this.authService = authService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ═══════════════════════════════════════════════════════
    // HTTP Infrastructure
    // ═══════════════════════════════════════════════════════

    /**
     * Perform a GET request to Strapi.
     */
    public JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authService.getStrapiBaseUrl() + path))
                .header("Authorization", "Bearer " + authService.getJwt())
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeRequest(request);
    }

    /**
     * Perform a POST request to Strapi.
     */
    public JsonNode post(String path, Object body) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authService.getStrapiBaseUrl() + path))
                .header("Authorization", "Bearer " + authService.getJwt())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeRequest(request);
    }

    /**
     * Perform a PUT request to Strapi.
     */
    public JsonNode put(String path, Object body) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authService.getStrapiBaseUrl() + path))
                .header("Authorization", "Bearer " + authService.getJwt())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeRequest(request);
    }

    /**
     * Perform a DELETE request to Strapi.
     */
    public JsonNode delete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authService.getStrapiBaseUrl() + path))
                .header("Authorization", "Bearer " + authService.getJwt())
                .header("Content-Type", "application/json")
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeRequest(request);
    }

    private JsonNode executeRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new AuthenticationExpiredException("Session expired. Please login again.");
        }

        if (response.statusCode() == 409) {
            throw new ConflictException("Resource conflict (e.g., already borrowed or already returned).");
        }

        if (response.statusCode() >= 400) {
            throw new StrapiApiException("Strapi API error " + response.statusCode() + ": " + response.body());
        }

        if (response.body() == null || response.body().isEmpty()) {
            return null;
        }

        return objectMapper.readTree(response.body());
    }

    // ═══════════════════════════════════════════════════════
    // Server connectivity
    // ═══════════════════════════════════════════════════════

    /**
     * Check if Strapi server is reachable.
     */
    public boolean isServerReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authService.getStrapiBaseUrl() + "/api/libraries"))
                    .header("Authorization", "Bearer " + authService.getJwt())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════
    // Publications (Έντυπα)
    // ═══════════════════════════════════════════════════════

    /**
     * Search Biblionet by ISBN. Returns { source, data } or null.
     */
    public JsonNode searchBiblionetByIsbn(String isbn) throws IOException, InterruptedException {
        return get("/api/books/search-biblionet?isbn=" + isbn);
    }

    /**
     * Get a publication by Strapi ID.
     */
    public JsonNode getPublicationById(Long id) throws IOException, InterruptedException {
        return get("/api/books/" + id + "?populate=authors,publisher,copies");
    }

    /**
     * Search publications by title (case-insensitive contains).
     */
    public JsonNode searchPublications(String query) throws IOException, InterruptedException {
        return get("/api/books?filters[title][$containsi]=" + encode(query) + "&populate=authors,publisher,copies");
    }

    /**
     * Get all publications of a specific type.
     */
    public JsonNode searchByType(String type) throws IOException, InterruptedException {
        return get("/api/books?filters[type][$eq]=" + encode(type) + "&populate=authors,publisher,copies&pagination[pageSize]=100");
    }

    /**
     * Create a new publication (Brochure or Periodical — Books come from Biblionet).
     */
    public JsonNode createPublication(Map<String, Object> data) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return post("/api/books", body);
    }

    // ═══════════════════════════════════════════════════════
    // Copies (Αντίτυπα)
    // ═══════════════════════════════════════════════════════

    /**
     * Get copies for a publication in a specific library.
     */
    public JsonNode getCopiesForPublication(Long publicationId, Long libraryId) throws IOException, InterruptedException {
        return get("/api/copies?filters[publication]=" + publicationId +
                "&filters[library]=" + libraryId +
                "&populate=publication");
    }

    /**
     * Get available copies for a publication in a specific library.
     */
    public JsonNode getAvailableCopies(Long publicationId, Long libraryId) throws IOException, InterruptedException {
        return get("/api/copies?filters[publication]=" + publicationId +
                "&filters[library]=" + libraryId +
                "&filters[isAvailable]=true" +
                "&populate=publication");
    }

    /**
     * Create a new copy.
     */
    public JsonNode createCopy(Long publicationId, int copyNumber, String condition) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("publication", publicationId);
        data.put("copyNumber", copyNumber);
        data.put("condition", condition);
        // Note: library is forced by Strapi beforeCreate lifecycle hook

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return post("/api/copies", body);
    }

    /**
     * Borrow a copy (atomic — handled by Strapi).
     */
    public JsonNode borrowCopy(Long copyId) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("copyId", copyId);
        return post("/api/copies/borrow", body);
    }

    /**
     * Return a copy (atomic — handled by Strapi).
     */
    public JsonNode returnCopy(Long copyId) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("copyId", copyId);
        return post("/api/copies/return", body);
    }

    /**
     * Update copy condition.
     */
    public JsonNode updateCopyCondition(Long copyId, String condition) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("condition", condition);
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return put("/api/copies/" + copyId, body);
    }

    /**
     * Delete a copy.
     */
    public JsonNode deleteCopy(Long copyId) throws IOException, InterruptedException {
        return delete("/api/copies/" + copyId);
    }

    // ═══════════════════════════════════════════════════════
    // Magazines (Περιοδικά)
    // ═══════════════════════════════════════════════════════

    /**
     * Get all magazines.
     */
    public JsonNode getAllMagazines() throws IOException, InterruptedException {
        return get("/api/magazines?populate=publisher,issues&pagination[pageSize]=100");
    }

    /**
     * Create a new magazine.
     */
    public JsonNode createMagazine(String title, String issn, Long publisherId) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("issn", issn);
        if (publisherId != null) {
            data.put("publisher", publisherId);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return post("/api/magazines", body);
    }

    // ═══════════════════════════════════════════════════════
    // Authors (Συγγραφείς)
    // ═══════════════════════════════════════════════════════

    public JsonNode getAllAuthors() throws IOException, InterruptedException {
        return get("/api/authors?pagination[pageSize]=100&populate=books");
    }

    public JsonNode createAuthor(String name, String firstname, String lastname) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("firstname", firstname);
        data.put("lastname", lastname);
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return post("/api/authors", body);
    }

    // ═══════════════════════════════════════════════════════
    // Publishers (Εκδότες)
    // ═══════════════════════════════════════════════════════

    public JsonNode getAllPublishers() throws IOException, InterruptedException {
        return get("/api/publishers?pagination[pageSize]=100&populate=books");
    }

    public JsonNode createPublisher(String name) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return post("/api/publishers", body);
    }

    // ═══════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ═══════════════════════════════════════════════════════
    // Custom Exceptions
    // ═══════════════════════════════════════════════════════

    public static class AuthenticationExpiredException extends RuntimeException {
        public AuthenticationExpiredException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class StrapiApiException extends RuntimeException {
        public StrapiApiException(String message) { super(message); }
    }
}

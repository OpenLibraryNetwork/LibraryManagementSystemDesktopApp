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
 * - Strapi 5 response format (flat): { "data": { "id": N, "documentId": "…", ... } }; records are addressed by documentId
 */
@Service
public class StrapiApiClient {

    /**
     * Populate for every publication request: contributors (person + role), publisher, subjects, copies with library.
     * Must stay equal to JAVAFX_BOOK_POPULATE in library-strapi/tests/integration/contract-fixtures.test.js.
     */
    public static final String BOOK_POPULATE =
            "populate[contributors][populate][0]=person&populate[contributors][populate][1]=role"
            + "&populate[publisher]=true&populate[subjects]=true&populate[copies][populate][0]=library";

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
        int status = response.statusCode();

        if (status == 401) {
            throw new AuthenticationExpiredException("Session expired. Please login again.");
        }

        if (status == 409) {
            throw new ConflictException(response.body(), serverMessageOf(response.body()));
        }

        if (status == 403) {
            throw new ForbiddenException(response.body(), serverMessageOf(response.body()));
        }

        if (status >= 400) {
            throw new StrapiApiException(status, response.body(), serverMessageOf(response.body()));
        }

        if (response.body() == null || response.body().isEmpty()) {
            return null;
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Strapi errors look like { "error": { "status": 400, "message": "..." } }.
     * Returns that message, or null when the body is not such JSON.
     */
    private String serverMessageOf(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode message = objectMapper.readTree(body).path("error").path("message");
            return message.isTextual() ? message.asText() : null;
        } catch (IOException e) {
            return null;
        }
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
     * Get a publication by its Strapi documentId.
     */
    public JsonNode getPublicationById(String documentId) throws IOException, InterruptedException {
        return get("/api/books/" + documentId + "?" + BOOK_POPULATE);
    }

    /**
     * Search publications by title (case-insensitive contains).
     */
    public JsonNode searchPublications(String query) throws IOException, InterruptedException {
        String libId = AuthService.getCurrentLibraryDocumentId();
        String url = "/api/books?filters[title][$containsi]=" + encode(query) + "&" + BOOK_POPULATE;
        if (libId != null) {
            url += "&filters[copies][library][documentId][$eq]=" + libId;
        }
        return get(url);
    }

    /**
     * Get all publications of a specific type.
     */
    public JsonNode searchByType(String type) throws IOException, InterruptedException {
        String libId = AuthService.getCurrentLibraryDocumentId();
        String url = "/api/books?filters[type][$eq]=" + encode(type) + "&" + BOOK_POPULATE + "&pagination[pageSize]=100";
        if (libId != null) {
            url += "&filters[copies][library][documentId][$eq]=" + libId;
        }
        return get(url);
    }


    /**
     * Get publications with server-side pagination, optional type filter and search.
     *
     * @param page        1-based page number
     * @param pageSize    items per page
     * @param typeFilter  null = all types, or "Βιβλίο", "Μπροσούρα", "Περιοδικό"
     * @param searchQuery null = no search, or text to search in title/isbn
     * @return Strapi response with data array and meta.pagination
     */
    public JsonNode getPublicationsPaginated(int page, int pageSize, String typeFilter, String searchQuery)
            throws IOException, InterruptedException {
        String libId = AuthService.getCurrentLibraryDocumentId();

        StringBuilder url = new StringBuilder("/api/books?" + BOOK_POPULATE);
        url.append("&pagination[page]=").append(page);
        url.append("&pagination[pageSize]=").append(pageSize);

        if (typeFilter != null && !typeFilter.isEmpty()) {
            url.append("&filters[type][$eq]=").append(encode(typeFilter));
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = encode(searchQuery.trim());
            url.append("&filters[$or][0][title][$containsi]=").append(q);
            url.append("&filters[$or][1][isbn][$containsi]=").append(q);
        }
        if (libId != null) {
            url.append("&filters[copies][library][documentId][$eq]=").append(libId);
        }

        return get(url.toString());
    }

    // ═══════════════════════════════════════════════════════
    // Copies (Αντίτυπα)
    // ═══════════════════════════════════════════════════════

    /**
     * Get copies for a publication in a specific library.
     */
    public JsonNode getCopiesForPublication(String publicationDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        return get("/api/copies?filters[publication][documentId][$eq]=" + publicationDocumentId +
                "&filters[library][documentId][$eq]=" + libraryDocumentId +
                "&populate=publication");
    }

    /**
     * Get available copies for a publication in a specific library.
     */
    public JsonNode getAvailableCopies(String publicationDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        return get("/api/copies?filters[publication][documentId][$eq]=" + publicationDocumentId +
                "&filters[library][documentId][$eq]=" + libraryDocumentId +
                "&filters[isAvailable]=true" +
                "&populate=publication");
    }

    /**
     * Create a new copy.
     */
    public JsonNode createCopy(String publicationDocumentId, int copyNumber, String condition) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("publication", publicationDocumentId);
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
    public JsonNode borrowCopy(String copyDocumentId) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("documentId", copyDocumentId);
        return post("/api/copies/borrow", body);
    }

    /**
     * Return a copy (atomic — handled by Strapi).
     */
    public JsonNode returnCopy(String copyDocumentId) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("documentId", copyDocumentId);
        return post("/api/copies/return", body);
    }

    /**
     * Update copy condition.
     */
    public JsonNode updateCopyCondition(String copyDocumentId, String condition) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("condition", condition);
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return put("/api/copies/" + copyDocumentId, body);
    }

    /**
     * Delete a copy. Strapi 5 answers 204 without a body.
     */
    public JsonNode deleteCopy(String copyDocumentId) throws IOException, InterruptedException {
        return delete("/api/copies/" + copyDocumentId);
    }

    // ═══════════════════════════════════════════════════════
    // Library catalog views (read-only): authors, publishers, their books
    // ═══════════════════════════════════════════════════════

    /** Authors with at least one publication that has a copy in the user's library (server decides the library). */
    public JsonNode getAuthorsInLibrary(int page, int pageSize, String query) throws IOException, InterruptedException {
        return get("/api/persons/authors?page=" + page + "&pageSize=" + pageSize + queryParam(query));
    }

    /** Publishers of publications that have a copy in the user's library. */
    public JsonNode getPublishersInLibrary(int page, int pageSize, String query) throws IOException, InterruptedException {
        return get("/api/publishers/in-library?page=" + page + "&pageSize=" + pageSize + queryParam(query));
    }

    /**
     * Publications of the library in which the person is a contributor (any role).
     * The caller keeps only those where the person is an author (AuthorWorksFilter).
     */
    public JsonNode getAuthorBooksInLibrary(String personDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        return get("/api/books?" + BOOK_POPULATE
                + "&filters[contributors][person][documentId][$eq]=" + personDocumentId
                + "&filters[copies][library][documentId][$eq]=" + libraryDocumentId
                + "&pagination[pageSize]=100&sort=title");
    }

    /** Publications of the library by this publisher. */
    public JsonNode getPublisherBooksInLibrary(String publisherDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        return get("/api/books?" + BOOK_POPULATE
                + "&filters[publisher][documentId][$eq]=" + publisherDocumentId
                + "&filters[copies][library][documentId][$eq]=" + libraryDocumentId
                + "&pagination[pageSize]=100&sort=title");
    }

    private String queryParam(String query) {
        return (query == null || query.isBlank()) ? "" : "&q=" + encode(query.trim());
    }

    // ═══════════════════════════════════════════════════════
    // Subjects (Θέματα DDC) — Read only (Biblionet)
    // ═══════════════════════════════════════════════════════

    /** One page of subjects; Strapi caps pageSize at 100 (config/api.js maxLimit), so callers loop over pages. */
    public JsonNode getSubjectsPage(int page) throws IOException, InterruptedException {
        return get("/api/subjects?sort=subjectDDC&pagination[page]=" + page + "&pagination[pageSize]=100");
    }


    // ═══════════════════════════════════════════════════════
    // Adding publications (sub-project 2β)
    // ═══════════════════════════════════════════════════════

    /** Catalog first, then Biblionet import. Response: { source: catalog|biblionet|not-found, data }. */
    public JsonNode isbnLookup(String isbn) throws IOException, InterruptedException {
        return post("/api/books/isbn-lookup", Map.of("isbn", isbn));
    }

    /** { data: {...} } — see PublicationDraft.toPayload(). 201 local, 200 biblionet/catalog, 409 with candidates. */
    public JsonNode createLocalPublication(Map<String, Object> payload) throws IOException, InterruptedException {
        return post("/api/books/local", payload);
    }

    public JsonNode createLocalPerson(Map<String, Object> payload) throws IOException, InterruptedException {
        return post("/api/persons/local", payload);
    }

    public JsonNode createLocalPublisher(Map<String, Object> payload) throws IOException, InterruptedException {
        return post("/api/publishers/local", payload);
    }

    /** Brochures of the whole network (not only this library). */
    public JsonNode searchBrochures(String query) throws IOException, InterruptedException {
        return get("/api/books/search?type=" + encode("Μπροσούρα") + "&q=" + encode(query.trim()));
    }

    public JsonNode searchPersons(String query) throws IOException, InterruptedException {
        return get("/api/persons/search?q=" + encode(query.trim()));
    }

    public JsonNode searchPublishers(String query) throws IOException, InterruptedException {
        return get("/api/publishers/search?q=" + encode(query.trim()));
    }

    public JsonNode getContributorRoles() throws IOException, InterruptedException {
        return get("/api/contributor-roles?sort=biblionetTypeId&pagination[pageSize]=100");
    }

    /** The highest-numbered copy of a publication in a library (for the next free copy number). */
    public JsonNode getCopiesInLibrary(String publicationDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        return get("/api/copies?filters[publication][documentId][$eq]=" + publicationDocumentId
                + "&filters[library][documentId][$eq]=" + libraryDocumentId
                + "&sort=copyNumber:desc&pagination[pageSize]=1");
    }

    // ═══════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════
    // Magazines (sub-project 2γ): shared catalog, issues are books of type "Περιοδικό"
    // ═══════════════════════════════════════════════════════

    /** ISSN or serial barcode → { source: catalog|nlg|not-found|unavailable, issn, data }. */
    public JsonNode magazineIssnLookup(String code) throws IOException, InterruptedException {
        return post("/api/magazines/issn-lookup", Map.of("code", code));
    }

    /** { data: {...} } — see MagazineDraft.toPayload(). 201, or 409 with candidates. */
    public JsonNode createLocalMagazine(Map<String, Object> payload) throws IOException, InterruptedException {
        return post("/api/magazines/local", payload);
    }

    /** Whole network, with issuesInLibrary for the user's library. */
    public JsonNode searchMagazines(String query) throws IOException, InterruptedException {
        return get("/api/magazines/search?q=" + encode(query.trim()));
    }

    /** Magazines with an issue that has a copy in the user's library (server decides the library). */
    public JsonNode getMagazinesInLibrary(int page, int pageSize, String query) throws IOException, InterruptedException {
        return get("/api/magazines/in-library?page=" + page + "&pageSize=" + pageSize + queryParam(query));
    }

    /** One page (of 100) of a magazine's issues; only those with a copy in the library when a library is given. */
    public JsonNode getIssues(String magazineDocumentId, String libraryDocumentId, int page) throws IOException, InterruptedException {
        return get("/api/books?" + BOOK_POPULATE
                + "&filters[type][$eq]=" + encode("Περιοδικό")
                + "&filters[magazine][documentId][$eq]=" + magazineDocumentId
                + (libraryDocumentId != null ? "&filters[copies][library][documentId][$eq]=" + libraryDocumentId : "")
                + "&pagination[page]=" + page + "&pagination[pageSize]=100");
    }

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

    /** 409; the body may carry "candidates" (local cataloguing) — see CatalogService. */
    public static class ConflictException extends StrapiApiException {
        public ConflictException(String body, String serverMessage) {
            super(409, body, serverMessage);
        }
    }

    public static class StrapiApiException extends RuntimeException {
        private final int status;
        private final String body;
        private final String serverMessage;

        public StrapiApiException(int status, String body, String serverMessage) {
            super("Strapi API error " + status + ": " + body);
            this.status = status;
            this.body = body;
            this.serverMessage = serverMessage;
        }

        public int getStatus() { return status; }
        public String getBody() { return body; }
        public String getServerMessage() { return serverMessage; }
    }

    public static class ForbiddenException extends StrapiApiException {
        public ForbiddenException(String body, String serverMessage) {
            super(403, body, serverMessage);
        }
    }
}

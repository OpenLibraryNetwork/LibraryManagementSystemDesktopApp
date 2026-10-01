package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StrapiApiClientTest {

    private HttpServer server;
    private StrapiApiClient client;
    private final List<String> requestedUris = new CopyOnWriteArrayList<>();
    private final List<String> requestBodies = new CopyOnWriteArrayList<>();
    private final List<String> upgradeHeaders = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = "{\"data\":[],\"meta\":{\"pagination\":{\"page\":1,\"pageSize\":25,\"pageCount\":1,\"total\":0}}}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestedUris.add(exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() != null ? "?" + exchange.getRequestURI().getRawQuery() : ""));
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
            if (upgrade != null) upgradeHeaders.add(upgrade);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        AuthService auth = mock(AuthService.class);
        when(auth.getStrapiBaseUrl()).thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
        when(auth.getJwt()).thenReturn("test-jwt");
        client = new StrapiApiClient(auth);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void forbiddenIsMappedToForbiddenException() {
        status = 403;
        body = "{\"data\":null,\"error\":{\"status\":403,\"name\":\"ForbiddenError\",\"message\":\"Forbidden\"}}";
        StrapiApiClient.ForbiddenException ex =
                assertThrows(StrapiApiClient.ForbiddenException.class, () -> client.get("/api/books"));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void strapiErrorMessageIsKept() {
        status = 400;
        body = "{\"data\":null,\"error\":{\"status\":400,\"name\":\"ApplicationError\",\"message\":\"Το αντίτυπο είναι δανεισμένο.\"}}";
        StrapiApiClient.StrapiApiException ex =
                assertThrows(StrapiApiClient.StrapiApiException.class, () -> client.delete("/api/copies/1"));
        assertEquals(400, ex.getStatus());
        assertEquals("Το αντίτυπο είναι δανεισμένο.", ex.getServerMessage());
    }

    @Test
    void nonJsonErrorBodyHasNoServerMessage() {
        status = 500;
        body = "Internal Server Error";
        StrapiApiClient.StrapiApiException ex =
                assertThrows(StrapiApiClient.StrapiApiException.class, () -> client.get("/api/books"));
        assertNull(ex.getServerMessage());
    }

    private String lastRequestDecoded() {
        return URLDecoder.decode(requestedUris.get(requestedUris.size() - 1), StandardCharsets.UTF_8);
    }

    @Test
    void authorsInLibraryUrlEncodesGreekQuery() throws Exception {
        client.getAuthorsInLibrary(2, 25, " ΛΟΪΖΊΔΗ ");
        assertEquals("/api/persons/authors?page=2&pageSize=25&q=ΛΟΪΖΊΔΗ", lastRequestDecoded());
        assertFalse(requestedUris.get(0).contains("Λ"), "Greek text must be percent-encoded on the wire");
    }

    @Test
    void blankQueryIsOmitted() throws Exception {
        client.getPublishersInLibrary(1, 25, "  ");
        assertEquals("/api/publishers/in-library?page=1&pageSize=25", lastRequestDecoded());
    }

    @Test
    void authorBooksInLibraryFiltersByPersonAndLibrary() throws Exception {
        client.getAuthorBooksInLibrary("p5", "l3");
        assertEquals("/api/books?" + StrapiApiClient.BOOK_POPULATE
                + "&filters[contributors][person][documentId][$eq]=p5&filters[copies][library][documentId][$eq]=l3"
                + "&pagination[pageSize]=100&sort=title", lastRequestDecoded());
    }

    @Test
    void publisherBooksInLibraryFiltersByPublisherAndLibrary() throws Exception {
        client.getPublisherBooksInLibrary("pb7", "l3");
        assertEquals("/api/books?" + StrapiApiClient.BOOK_POPULATE
                + "&filters[publisher][documentId][$eq]=pb7&filters[copies][library][documentId][$eq]=l3"
                + "&pagination[pageSize]=100&sort=title", lastRequestDecoded());
    }

    @Test
    void publicationQueriesPopulateContributors() throws Exception {
        client.getPublicationsPaginated(1, 15, null, null);
        client.getPublicationById("bk9");
        for (String uri : requestedUris) {
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            assertTrue(decoded.contains(StrapiApiClient.BOOK_POPULATE), decoded);
            assertFalse(decoded.contains("authors"), decoded);
        }
    }

    @Test
    void conflictKeepsBodyForCandidates() {
        status = 409;
        body = "{\"data\":null,\"error\":{\"status\":409,\"message\":\"dup\"},\"candidates\":[{\"id\":7}]}";
        StrapiApiClient.ConflictException ex =
                assertThrows(StrapiApiClient.ConflictException.class, () -> client.post("/api/persons/local", java.util.Map.of()));
        assertEquals(409, ex.getStatus());
        assertTrue(ex.getBody().contains("\"candidates\""));
    }

    @Test
    void isbnLookupPostsIsbn() throws Exception {
        client.isbnLookup("978-960-211-652-4");
        assertEquals("/api/books/isbn-lookup", lastRequestDecoded());
        assertEquals("{\"isbn\":\"978-960-211-652-4\"}", requestBodies.get(requestBodies.size() - 1));
    }

    @Test
    void localCreationEndpoints() throws Exception {
        client.createLocalPublication(java.util.Map.of("data", java.util.Map.of("title", "Τ")));
        client.createLocalPerson(java.util.Map.of("data", java.util.Map.of("name", "Ν")));
        client.createLocalPublisher(java.util.Map.of("data", java.util.Map.of("name", "Ε")));
        assertEquals(java.util.List.of("/api/books/local", "/api/persons/local", "/api/publishers/local"),
                requestedUris.stream().map(u -> URLDecoder.decode(u, StandardCharsets.UTF_8)).toList());
        assertEquals("{\"data\":{\"title\":\"Τ\"}}", requestBodies.get(0));
    }

    @Test
    void searchAndLookupUrls() throws Exception {
        client.searchBrochures(" μανιφέστο ");
        client.searchPersons("λοϊζ");
        client.searchPublishers("νεφ");
        client.getContributorRoles();
        client.getCopiesInLibrary("p5", "l3");
        assertEquals(java.util.List.of(
                "/api/books/search?type=Μπροσούρα&q=μανιφέστο",
                "/api/persons/search?q=λοϊζ",
                "/api/publishers/search?q=νεφ",
                "/api/contributor-roles?sort=biblionetTypeId&pagination[pageSize]=100",
                "/api/copies?filters[publication][documentId][$eq]=p5&filters[library][documentId][$eq]=l3&sort=copyNumber:desc&pagination[pageSize]=1"),
                requestedUris.stream().map(u -> URLDecoder.decode(u, StandardCharsets.UTF_8)).toList());
    }

    @Test
    void magazineUrlsAndBodies() throws Exception {
        body = "{\"source\":\"not-found\",\"issn\":\"0317-8471\",\"data\":null}";
        client.magazineIssnLookup("0317-8471");
        client.createLocalMagazine(java.util.Map.of("data", java.util.Map.of("title", "Τ")));
        body = "{\"data\":[]}";
        client.searchMagazines(" αναρχ ");
        client.getMagazinesInLibrary(2, 15, "κοιν");
        client.getIssues("m7", "l3", 1);
        client.getIssues("m7", null, 2);

        List<String> uris = requestedUris.stream().map(u -> URLDecoder.decode(u, StandardCharsets.UTF_8)).toList();
        assertEquals("/api/magazines/issn-lookup", uris.get(0));
        assertEquals("{\"code\":\"0317-8471\"}", requestBodies.get(0));
        assertEquals("/api/magazines/local", uris.get(1));
        assertEquals("{\"data\":{\"title\":\"Τ\"}}", requestBodies.get(1));
        assertEquals("/api/magazines/search?q=αναρχ", uris.get(2));
        assertEquals("/api/magazines/in-library?page=2&pageSize=15&q=κοιν", uris.get(3));
        assertTrue(uris.get(4).startsWith("/api/books?" + StrapiApiClient.BOOK_POPULATE), uris.get(4));
        assertTrue(uris.get(4).endsWith("&filters[type][$eq]=Περιοδικό&filters[magazine][documentId][$eq]=m7"
                + "&filters[copies][library][documentId][$eq]=l3&pagination[page]=1&pagination[pageSize]=100"), uris.get(4));
        assertTrue(uris.get(5).endsWith("&filters[type][$eq]=Περιοδικό&filters[magazine][documentId][$eq]=m7&pagination[page]=2&pagination[pageSize]=100"), uris.get(5));
    }

    @Test
    void copyEndpointsUseDocumentId() throws Exception {
        client.getPublicationById("bk9");
        client.createCopy("p5", 2, "NEW");
        client.borrowCopy("c1");
        client.returnCopy("c1");
        client.updateCopyCondition("c1", "GOOD");
        status = 204;
        body = "";
        client.deleteCopy("c1");

        List<String> uris = requestedUris.stream().map(u -> URLDecoder.decode(u, StandardCharsets.UTF_8)).toList();
        assertTrue(uris.get(0).startsWith("/api/books/bk9?"), uris.get(0));
        assertEquals("/api/copies", uris.get(1));
        assertTrue(requestBodies.get(1).contains("\"publication\":\"p5\""), requestBodies.get(1));
        assertEquals("/api/copies/borrow", uris.get(2));
        assertEquals("{\"documentId\":\"c1\"}", requestBodies.get(2));
        assertEquals("/api/copies/return", uris.get(3));
        assertEquals("{\"documentId\":\"c1\"}", requestBodies.get(3));
        assertEquals("/api/copies/c1", uris.get(4));
        assertEquals("/api/copies/c1", uris.get(5));
    }

    @Test
    void requestsDoNotAskForAnHttp2Upgrade() throws Exception {
        // Strapi 5 in develop mode never answers a request carrying "Upgrade: h2c" (the JDK client's default)
        client.getPublicationById("bk9");
        client.borrowCopy("c1");
        assertEquals(List.of(), upgradeHeaders);
    }

    @Test
    void nonJsonErrorBodyKeepsTheStatus() { // Review Focus 2: e.g. an HTML 502 page from a proxy
        status = 502;
        body = "<html><body>Bad Gateway</body></html>";
        StrapiApiClient.StrapiApiException ex =
                assertThrows(StrapiApiClient.StrapiApiException.class, () -> client.get("/api/books"));
        assertEquals(502, ex.getStatus());
    }
}

package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** The login screen must tell wrong credentials, a non-librarian, a user without library and an unreachable server apart. */
class AuthServiceLoginTest {

    private HttpServer server;
    private KeyStoreService keyStore;
    private volatile String upgradeHeader;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        keyStore = mock(KeyStoreService.class);
        auth = new AuthService();
        ReflectionTestUtils.setField(auth, "keyStoreService", keyStore);
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        ReflectionTestUtils.setField(AuthService.class, "instance", null); // the static session must not leak into other tests
    }

    private String serverAnswering(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/local", exchange -> {
            upgradeHeader = exchange.getRequestHeaders().getFirst("Upgrade");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void wrongCredentials() throws IOException {
        String url = serverAnswering(400, "{\"data\":null,\"error\":{\"status\":400,\"message\":\"Invalid identifier or password\"}}");
        assertEquals(AuthService.LoginResult.BAD_CREDENTIALS, auth.login(url, "test", "wrong"));
        assertFalse(auth.isAuthenticated());
    }

    @Test
    void userWithoutLibraryIsNotLoggedIn() throws IOException {
        String url = serverAnswering(200, "{\"jwt\":\"t\",\"user\":{\"id\":1,\"username\":\"test\",\"role\":{\"type\":\"librarian\"}}}");
        AuthService.LoginResult result = auth.login(url, "test", "right");
        assertEquals(AuthService.LoginResult.NO_LIBRARY, result);
        assertTrue(result.getMessage().contains("βιβλιοθήκη"));
        assertFalse(auth.isAuthenticated());
        verify(keyStore, never()).storeSecret(anyString(), anyString());
    }

    @Test
    void successStoresTheSession() throws IOException {
        String url = serverAnswering(200, "{\"jwt\":\"t\",\"user\":{\"id\":1,\"role\":{\"type\":\"librarian\"},\"library\":{\"id\":3,\"documentId\":\"libA\"}}}");
        assertEquals(AuthService.LoginResult.SUCCESS, auth.login(url + "/", "test", "right"));
        assertTrue(auth.isAuthenticated());
        assertEquals("libA", auth.getLibraryDocumentId());
        verify(keyStore).storeSecret(KeyStoreService.KEY_JWT, "t");
        assertNull(upgradeHeader); // Strapi 5 in develop mode never answers a request carrying "Upgrade: h2c"
    }

    @Test
    void aUserWithoutTheLibrarianRoleCannotLogIn() throws IOException { // Review Focus 3
        String body = "{\"jwt\":\"j\",\"user\":{\"id\":5,\"role\":{\"type\":\"authenticated\"},\"library\":{\"documentId\":\"libA\"}}}";
        AuthService.LoginResult result = auth.login(serverAnswering(200, body), "u", "p");
        assertEquals(AuthService.LoginResult.NOT_LIBRARIAN, result);
        assertTrue(result.getMessage().contains("βιβλιοθηκονόμου"));
        assertFalse(auth.isAuthenticated());
        verify(keyStore, never()).storeSecret(eq(KeyStoreService.KEY_JWT), anyString());
    }

    @Test
    void aLibrarianLogsInAndTheLibraryDocumentIdIsKept() throws IOException {
        String body = "{\"jwt\":\"j\",\"user\":{\"id\":5,\"role\":{\"type\":\"librarian\"},\"library\":{\"documentId\":\"libA\"}}}";
        assertEquals(AuthService.LoginResult.SUCCESS, auth.login(serverAnswering(200, body), "u", "p"));
        verify(keyStore).storeSecret(KeyStoreService.KEY_LIBRARY_DOCUMENT_ID, "libA");
        assertEquals("libA", auth.getLibraryDocumentId());
    }

    @Test
    void unreachableServer() throws IOException {
        String url;
        try (ServerSocket socket = new ServerSocket(0)) {
            url = "http://127.0.0.1:" + socket.getLocalPort();
        }
        assertEquals(AuthService.LoginResult.UNREACHABLE, auth.login(url, "test", "right"));
    }
}

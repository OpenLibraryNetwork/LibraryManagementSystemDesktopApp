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
import static org.mockito.Mockito.*;

/** The login screen must tell wrong credentials, a user without library and an unreachable server apart. */
class AuthServiceLoginTest {

    private HttpServer server;
    private KeyStoreService keyStore;
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
    }

    private String serverAnswering(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/local", exchange -> {
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
        String url = serverAnswering(200, "{\"jwt\":\"t\",\"user\":{\"id\":1,\"username\":\"test\"}}");
        AuthService.LoginResult result = auth.login(url, "test", "right");
        assertEquals(AuthService.LoginResult.NO_LIBRARY, result);
        assertTrue(result.getMessage().contains("βιβλιοθήκη"));
        assertFalse(auth.isAuthenticated());
        verify(keyStore, never()).storeSecret(anyString(), anyString());
    }

    @Test
    void successStoresTheSession() throws IOException {
        String url = serverAnswering(200, "{\"jwt\":\"t\",\"user\":{\"id\":1,\"library\":{\"id\":3}}}");
        assertEquals(AuthService.LoginResult.SUCCESS, auth.login(url + "/", "test", "right"));
        assertTrue(auth.isAuthenticated());
        assertEquals(3L, auth.getLibraryId());
        verify(keyStore).storeSecret(KeyStoreService.KEY_JWT, "t");
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

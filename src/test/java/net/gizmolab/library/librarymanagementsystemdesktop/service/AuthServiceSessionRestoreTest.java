package net.gizmolab.library.librarymanagementsystemdesktop.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * At startup the saved session is checked with GET /api/users/me.
 * A rejected token (401, e.g. the Strapi database was recreated) must lead to the login screen;
 * an unreachable server must keep the session (the app just shows "Offline").
 */
class AuthServiceSessionRestoreTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    private String serverAnswering(int status) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String unreachableUrl() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort(); // closed right after: nothing listens
        }
    }

    private static AuthService restoredFrom(String url, KeyStoreService keyStore) {
        when(keyStore.getSecret(KeyStoreService.KEY_JWT)).thenReturn("saved-jwt");
        when(keyStore.getSecret(KeyStoreService.KEY_LIBRARY_ID)).thenReturn("1");
        when(keyStore.getSecret(KeyStoreService.KEY_STRAPI_URL)).thenReturn(url);
        AuthService auth = new AuthService();
        ReflectionTestUtils.setField(auth, "keyStoreService", keyStore);
        auth.init();
        return auth;
    }

    @Test
    void rejectedTokenClearsTheSessionSoTheLoginScreenShows() throws IOException {
        KeyStoreService keyStore = mock(KeyStoreService.class);
        AuthService auth = restoredFrom(serverAnswering(401), keyStore);
        assertFalse(auth.isAuthenticated());
        verify(keyStore).clearAll();
    }

    @Test
    void validTokenIsOnline() throws IOException {
        KeyStoreService keyStore = mock(KeyStoreService.class);
        AuthService auth = restoredFrom(serverAnswering(200), keyStore);
        assertTrue(auth.isAuthenticated());
        assertTrue(auth.isOnline());
        verify(keyStore, never()).clearAll();
    }

    @Test
    void unreachableServerKeepsTheSessionButOffline() throws IOException {
        KeyStoreService keyStore = mock(KeyStoreService.class);
        AuthService auth = restoredFrom(unreachableUrl(), keyStore);
        assertTrue(auth.isAuthenticated());
        assertFalse(auth.isOnline());
        verify(keyStore, never()).clearAll();
    }
}

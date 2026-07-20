package org.freemind.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.freemind.web.model.MindMapMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M1 exit test: two clients on one server — a node operation sent by one
 * arrives at the other (and only the other). Servers bind ephemeral ports
 * (port 0) so the suite runs regardless of what occupies 8080.
 */
class WebSocketRelayTest {

    /** Collects incoming messages for one test client. */
    @WebSocket
    public static class TestClient {
        private final LinkedBlockingQueue<MindMapMessage> received = new LinkedBlockingQueue<>();
        private final ObjectMapper mapper = new ObjectMapper();

        @OnWebSocketMessage
        public void onMessage(String message) throws Exception {
            received.add(mapper.readValue(message, MindMapMessage.class));
        }

        MindMapMessage poll(long millis) throws InterruptedException {
            return received.poll(millis, TimeUnit.MILLISECONDS);
        }

        /** Next message, asserting its type. */
        MindMapMessage expect(MindMapMessage.MessageType type) throws InterruptedException {
            MindMapMessage msg = poll(5000);
            assertNotNull(msg, "expected a " + type + " message, got none");
            assertEquals(type, msg.getType());
            return msg;
        }
    }

    private WebServer server;
    private WebSocketClient client;

    @BeforeEach
    void startServer() throws Exception {
        server = new WebServer(0);
        server.start();
        client = new WebSocketClient();
        client.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        client.stop();
        server.stop();
    }

    private Session connect(WebServer target, TestClient socket) throws Exception {
        URI uri = URI.create("ws://localhost:" + target.getPort() + WebServer.WEBSOCKET_PATH);
        return client.connect(socket, uri).get(5, TimeUnit.SECONDS);
    }

    @Test
    void serverBindsEphemeralPort() {
        assertTrue(server.getPort() > 0, "port 0 should bind a real ephemeral port");
    }

    @Test
    void nodeMoveRelaysToOtherClientOnly() throws Exception {
        TestClient a = new TestClient();
        TestClient b = new TestClient();

        Session sessionA = connect(server, a);
        a.expect(MindMapMessage.MessageType.SYSTEM); // welcome

        connect(server, b);
        b.expect(MindMapMessage.MessageType.SYSTEM);          // welcome
        a.expect(MindMapMessage.MessageType.USER_CONNECTED);  // B joined

        // A drags a node; B must see it move
        MindMapMessage move = MindMapMessage.builder(MindMapMessage.MessageType.NODE_MOVED)
                .nodeId("2")
                .position(250.0, 475.0)
                .build();
        sessionA.getRemote().sendString(new ObjectMapper().writeValueAsString(move));

        MindMapMessage relayed = b.expect(MindMapMessage.MessageType.NODE_MOVED);
        assertEquals("2", relayed.getNodeId());
        assertEquals(250.0, relayed.getX());
        assertEquals(475.0, relayed.getY());
        assertNotNull(relayed.getSenderId(), "server must stamp a sender id");
        assertNotEquals("server", relayed.getSenderId());

        // ...and A must NOT receive its own message back
        assertNull(a.poll(500), "sender must not receive an echo of its own operation");
    }

    @Test
    void disconnectNotifiesRemainingClientAsUserDisconnected() throws Exception {
        TestClient a = new TestClient();
        TestClient b = new TestClient();

        Session sessionA = connect(server, a);
        a.expect(MindMapMessage.MessageType.SYSTEM);
        connect(server, b);
        b.expect(MindMapMessage.MessageType.SYSTEM);
        a.expect(MindMapMessage.MessageType.USER_CONNECTED);

        sessionA.close();

        // Formerly broadcast as a bogus NODE_DELETED (HLD defect 6)
        b.expect(MindMapMessage.MessageType.USER_DISCONNECTED);
    }

    @Test
    void invalidMessageGetsErrorReply() throws Exception {
        TestClient a = new TestClient();
        Session sessionA = connect(server, a);
        a.expect(MindMapMessage.MessageType.SYSTEM);

        sessionA.getRemote().sendString("{\"type\":\"NO_SUCH_TYPE\"}");

        a.expect(MindMapMessage.MessageType.ERROR);
    }

    @Test
    void twoServersDoNotShareSessions() throws Exception {
        WebServer second = new WebServer(0);
        second.start();
        try {
            TestClient a = new TestClient();
            TestClient other = new TestClient();

            Session sessionA = connect(server, a);
            a.expect(MindMapMessage.MessageType.SYSTEM);
            connect(second, other);
            other.expect(MindMapMessage.MessageType.SYSTEM);

            // No cross-talk on connect (state is per-server, not static)...
            assertNull(a.poll(500), "client of server 1 saw a connect on server 2");

            // ...and none on node operations either
            MindMapMessage move = MindMapMessage.builder(MindMapMessage.MessageType.NODE_MOVED)
                    .nodeId("1")
                    .position(10.0, 20.0)
                    .build();
            sessionA.getRemote().sendString(new ObjectMapper().writeValueAsString(move));
            assertNull(other.poll(500), "message crossed between server instances");
        } finally {
            second.stop();
        }
    }
}

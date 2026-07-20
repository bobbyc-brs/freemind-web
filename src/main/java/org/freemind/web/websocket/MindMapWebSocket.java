package org.freemind.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.freemind.web.model.MindMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * One instance per client connection (created by the WebSocketCreator in
 * {@link org.freemind.web.WebServer}); shared state lives in the {@link MindMapHub}.
 *
 * M1 semantics: pure relay — node operations from one client are forwarded
 * verbatim to every other client of the same server.
 */
@WebSocket
public class MindMapWebSocket {
    private static final Logger logger = LoggerFactory.getLogger(MindMapWebSocket.class);
    private static final String SERVER_ID = "server";

    private final MindMapHub hub;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId = UUID.randomUUID().toString().substring(0, 8);

    public MindMapWebSocket(MindMapHub hub) {
        this.hub = hub;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Client {} connected from {}", clientId, session.getRemoteAddress());
        hub.add(session);

        hub.send(session, MindMapMessage.builder(MindMapMessage.MessageType.SYSTEM)
                .text("Connected to FreeMind Web")
                .senderId(SERVER_ID)
                .data("clientId", clientId)
                .build());

        hub.broadcastToOthers(session, MindMapMessage.builder(MindMapMessage.MessageType.USER_CONNECTED)
                .text("User " + clientId + " connected")
                .senderId(SERVER_ID)
                .data("clientId", clientId)
                .build());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        logger.debug("Message from {}: {}", clientId, message);
        try {
            MindMapMessage msg = objectMapper.readValue(message, MindMapMessage.class);
            if (msg.getSenderId() == null) {
                msg.setSenderId(clientId);
            }
            hub.broadcastToOthers(session, msg);
        } catch (IOException e) {
            logger.error("Error processing message from {}: {}", clientId, e.getMessage());
            hub.send(session, MindMapMessage.builder(MindMapMessage.MessageType.ERROR)
                    .text("Invalid message: " + e.getMessage())
                    .senderId(SERVER_ID)
                    .build());
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        logger.info("Client {} disconnected: {} {}", clientId, statusCode, reason);
        hub.remove(session);

        hub.broadcastToOthers(session, MindMapMessage.builder(MindMapMessage.MessageType.USER_DISCONNECTED)
                .text("User " + clientId + " disconnected")
                .senderId(SERVER_ID)
                .data("clientId", clientId)
                .build());
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        logger.error("WebSocket error for client {}: {}", clientId, throwable.getMessage(), throwable);
        if (session != null && session.isOpen()) {
            hub.send(session, MindMapMessage.builder(MindMapMessage.MessageType.ERROR)
                    .text("Error occurred: " + throwable.getMessage())
                    .senderId(SERVER_ID)
                    .build());
            session.close(StatusCode.SERVER_ERROR, "Error occurred: " + throwable.getMessage());
        }
    }
}

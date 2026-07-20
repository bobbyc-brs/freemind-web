package org.freemind.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.freemind.web.model.MindMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@WebSocket
public class MindMapWebSocket {
    private static final Logger logger = LoggerFactory.getLogger(MindMapWebSocket.class);
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("New WebSocket connection: {}", session.getRemoteAddress());
        sessions.add(session);
        
        // Send welcome message
        MindMapMessage welcomeMsg = MindMapMessage.builder(MindMapMessage.MessageType.MAP_LOADED)
                .text("Connected to FreeMind WebSocket")
                .senderId("server")
                .build();
                
        try {
            session.getRemote().sendString(objectMapper.writeValueAsString(welcomeMsg));
        } catch (IOException e) {
            logger.error("Error sending welcome message: {}", e.getMessage(), e);
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        logger.debug("Message from {}: {}", session.getRemoteAddress(), message);
        
        try {
            // Parse the message
            MindMapMessage msg = objectMapper.readValue(message, MindMapMessage.class);
            
            // Set the sender ID
            if (msg.getSenderId() == null) {
                msg = MindMapMessage.builder(msg.getType())
                        .nodeId(msg.getNodeId())
                        .parentId(msg.getParentId())
                        .text(msg.getText())
                        .data(msg.getData())
                        .senderId(session.getRemoteAddress().toString())
                        .build();
            }
            
            // Broadcast to all connected clients
            broadcastMessage(msg);
            
        } catch (IOException e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            String errorMessage = (e instanceof JsonProcessingException) ? 
                "Invalid message format: " + e.getMessage() : 
                "Error processing message: " + e.getMessage();
            sendError(session, errorMessage);
        }
    }
    
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        logger.info("WebSocket connection closed: {} - {}", session.getRemoteAddress(), reason);
        sessions.remove(session);
        
        // Notify other clients about the disconnection
        MindMapMessage disconnectMsg = MindMapMessage.builder(MindMapMessage.MessageType.NODE_DELETED)
                .text("User disconnected")
                .senderId("server")
                .build();
                
        broadcastMessage(disconnectMsg);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        logger.error("WebSocket error for {}: {}", session.getRemoteAddress(), throwable.getMessage(), throwable);
        
        if (session.isOpen()) {
            try {
                MindMapMessage errorMsg = MindMapMessage.builder(MindMapMessage.MessageType.ERROR)
                        .text("Error occurred: " + throwable.getMessage())
                        .senderId("server")
                        .build();
                session.getRemote().sendString(objectMapper.writeValueAsString(errorMsg));
            } catch (Exception e) {
                logger.error("Error sending error message: {}", e.getMessage(), e);
            }
            
            session.close(StatusCode.SERVER_ERROR, 
                "Error occurred: " + throwable.getMessage());
        }
    }

    private void broadcastMessage(MindMapMessage message) {
        if (message == null) return;
        
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
            logger.debug("Broadcasting message: {}", jsonMessage);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing message: {}", e.getMessage(), e);
            return;
        }

        synchronized (sessions) {
            for (Session session : sessions) {
                try {
                    if (session != null && session.isOpen()) {
                        session.getRemote().sendString(jsonMessage);
                    }
                } catch (IOException e) {
                    logger.error("Error sending message to {}: {}", 
                        session != null ? session.getRemoteAddress() : "unknown", 
                        e.getMessage(), e);
                } catch (Exception e) {
                    logger.error("Unexpected error sending message: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    private void sendError(Session session, String errorMessage) {
        if (session != null && session.isOpen()) {
            try {
                MindMapMessage errorMsg = MindMapMessage.builder(MindMapMessage.MessageType.ERROR)
                        .text(errorMessage)
                        .senderId("server")
                        .build();
                        
                session.getRemote().sendString(objectMapper.writeValueAsString(errorMsg));
            } catch (IOException e) {
                System.err.println("Error sending error message: " + e.getMessage());
            }
        }
    }
}

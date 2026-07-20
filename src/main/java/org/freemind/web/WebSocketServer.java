package org.freemind.web;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
public class WebSocketServer {
    
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, Object> mindMapData = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket opened: " + session.getId());
        sessions.add(session);
        
        // Send current mind map data to the new client
        sendMessage(session, createMessage("mapData", mindMapData));
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message from client " + session.getId() + ": " + message);
        
        // In a real implementation, you would parse the message and update the mind map data
        // For now, we'll just echo the message back to all clients
        broadcast(createMessage("echo", message), session);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket closed: " + session.getId() + " - " + reason);
        sessions.remove(session);
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error on session " + session.getId() + ":");
        error.printStackTrace();
    }
    
    private void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            System.err.println("Error sending message to client " + session.getId() + ":");
            e.printStackTrace();
        }
    }
    
    private void broadcast(String message, Session excludeSession) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (!session.equals(excludeSession)) {
                    sendMessage(session, message);
                }
            }
        }
    }
    
    private String createMessage(String type, Object data) {
        // In a real app, use a proper JSON library like Jackson
        return String.format("{\"type\":\"%s\",\"data\":%s}", 
            type, 
            data instanceof String ? "\"" + data + "\"" : data.toString());
    }
}

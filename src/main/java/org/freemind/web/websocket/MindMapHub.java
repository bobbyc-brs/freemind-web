package org.freemind.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.freemind.web.model.MindMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Session registry for one server instance. Owned by {@link org.freemind.web.WebServer};
 * every {@link MindMapWebSocket} connection of that server shares this hub, so two
 * servers in one JVM (tests) never see each other's clients.
 */
public class MindMapHub {
    private static final Logger logger = LoggerFactory.getLogger(MindMapHub.class);

    private final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void add(Session session) {
        sessions.add(session);
    }

    public void remove(Session session) {
        sessions.remove(session);
    }

    public void send(Session to, MindMapMessage message) {
        if (to == null || !to.isOpen()) {
            return;
        }
        try {
            to.getRemote().sendString(objectMapper.writeValueAsString(message));
        } catch (IOException e) {
            logger.error("Error sending message to {}: {}", to.getRemoteAddress(), e.getMessage(), e);
        }
    }

    /**
     * Relays a message to every open session except the sender — the sender
     * already applied the change locally, so echoing it back would be wasted
     * work at best and a feedback loop at worst.
     */
    public void broadcastToOthers(Session from, MindMapMessage message) {
        if (message == null) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing message: {}", e.getMessage(), e);
            return;
        }
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session == from || !session.isOpen()) {
                    continue;
                }
                try {
                    session.getRemote().sendString(json);
                } catch (IOException e) {
                    logger.error("Error sending message to {}: {}",
                            session.getRemoteAddress(), e.getMessage(), e);
                }
            }
        }
    }
}

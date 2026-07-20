package org.freemind.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a message in the FreeMind WebSocket communication.
 * This class is used to serialize/deserialize messages between the client and server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MindMapMessage {
    /**
     * The single source of truth for message type names, client and server:
     * AppServlet serves this enum to the browser as /js/message-types.js,
     * so the two sides cannot drift.
     */
    public enum MessageType {
        NODE_CREATED,      // When a new node is created
        NODE_UPDATED,      // When a node's properties are updated
        NODE_DELETED,      // When a node is deleted
        NODE_MOVED,        // When a node is moved to a new position
        MAP_LOADED,        // When the initial map is loaded
        USER_CONNECTED,    // When another client joins
        USER_DISCONNECTED, // When another client leaves
        ERROR,             // When an error occurs
        SYSTEM             // For system notifications
    }

    private String messageId;          // Unique ID for the message
    private MessageType type;          // Type of the message
    private String nodeId;             // ID of the affected node (if any)
    private String parentId;           // ID of the parent node (if any)
    private String text;               // Text content of the message
    private Double x;                  // X coordinate for node position
    private Double y;                  // Y coordinate for node position
    private Map<String, Object> data;  // Additional data as key-value pairs
    private String senderId;           // ID of the message sender
    private Long timestamp;            // When the message was created

    /**
     * Default constructor for JSON deserialization.
     * Initializes the message with a unique ID and current timestamp.
     */
    public MindMapMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    private MindMapMessage(Builder builder) {
        this();
        this.type = builder.type;
        this.nodeId = builder.nodeId;
        this.parentId = builder.parentId;
        this.text = builder.text;
        this.x = builder.x;
        this.y = builder.y;
        this.senderId = builder.senderId;
        if (builder.data != null) {
            this.data.putAll(builder.data);
        }
    }

    /**
     * Creates a new builder instance with the specified message type.
     * @param type The type of the message
     * @return A new builder instance
     */
    public static Builder builder(MessageType type) {
        return new Builder(type);
    }

    // Getters and Setters with documentation

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Converts this message to a JSON string.
     * @return A JSON representation of this message
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
    }

    /**
     * Creates a MindMapMessage from a JSON string.
     * @param json The JSON string to deserialize
     * @return A new MindMapMessage instance
     */
    public static MindMapMessage fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, MindMapMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing message from JSON: " + json, e);
        }
    }

    /**
     * Builder pattern for creating MindMapMessage instances.
     */
    public static class Builder {
        private final MessageType type;
        private String nodeId;
        private String parentId;
        private String text;
        private Double x;
        private Double y;
        private Map<String, Object> data;
        private String senderId;

        public Builder(MessageType type) {
            this.type = type;
            this.data = new HashMap<>();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder position(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder data(String key, Object value) {
            if (key != null && value != null) {
                this.data.put(key, value);
            }
            return this;
        }

        public Builder data(Map<String, Object> data) {
            if (data != null) {
                this.data.putAll(data);
            }
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public MindMapMessage build() {
            return new MindMapMessage(this);
        }
    }

    @Override
    public String toString() {
        return "MindMapMessage{" +
                "messageId='" + messageId + '\'' +
                ", type=" + type +
                ", nodeId='" + nodeId + '\'' +
                ", parentId='" + parentId + '\'' +
                ", text='" + text + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", data=" + data +
                ", senderId='" + senderId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MindMapMessage that = (MindMapMessage) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}

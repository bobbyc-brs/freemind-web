/**
 * WebSocket client for FreeMind Web
 * Handles connection, reconnection, and message passing with the server.
 *
 * Pure transport: no DOM access, no connection at script load — the app
 * creates an instance and calls connect() when it is ready (testability
 * hook: pages and tests control exactly when the socket opens).
 */
class WebSocketClient {
    constructor() {
        this.socket = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 1000; // Start with 1 second
        this.maxReconnectDelay = 30000; // Max 30 seconds
        this.eventHandlers = {};
    }

    /**
     * Connect to the WebSocket server
     */
    connect() {
        // Determine WebSocket URL based on current location
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/mindmap`;

        console.log(`Connecting to WebSocket at ${wsUrl}...`);
        this.updateStatus('Connecting...', 'connecting');

        try {
            this.socket = new WebSocket(wsUrl);

            this.socket.onopen = () => {
                console.log('WebSocket connected');
                this.connected = true;
                this.reconnectAttempts = 0;
                this.updateStatus('Connected', 'connected');
                this.emit('connected');
            };

            this.socket.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);
                    console.debug('WebSocket message received:', message);

                    // Emit generic message event
                    this.emit('message', message);

                    // Emit specific event based on message type
                    // (NODE_MOVED -> 'node-moved')
                    if (message.type) {
                        const eventName = message.type.toLowerCase().replace(/_/g, '-');
                        this.emit(eventName, message);
                    }
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error, event.data);
                }
            };

            this.socket.onclose = (event) => {
                console.log('WebSocket disconnected:', event);
                this.connected = false;

                // Only emit disconnected if we weren't expecting this
                if (this.reconnectAttempts === 0) {
                    this.emit('disconnected', event);
                }

                // Attempt to reconnect
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    const delay = Math.min(
                        this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
                        this.maxReconnectDelay
                    );

                    console.log(`Attempting to reconnect in ${delay}ms... (Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
                    this.updateStatus(`Reconnecting in ${Math.ceil(delay / 1000)}s... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`, 'warning');

                    setTimeout(() => this.connect(), delay);
                } else {
                    console.error('Max reconnection attempts reached');
                    this.updateStatus('Disconnected. Please refresh the page to reconnect.', 'error');
                    this.emit('reconnect_failed');
                }
            };

            this.socket.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.emit('error', error);
                this.updateStatus('Connection error', 'error');
            };

        } catch (error) {
            console.error('Error creating WebSocket:', error);
            this.updateStatus('Connection failed', 'error');

            // Try to reconnect after a delay
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                const delay = Math.min(
                    this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
                    this.maxReconnectDelay
                );

                console.log(`Retrying connection in ${delay}ms... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
                this.updateStatus(`Retrying in ${Math.ceil(delay / 1000)}s... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`, 'warning');

                setTimeout(() => this.connect(), delay);
            } else {
                this.updateStatus('Connection failed. Please refresh the page.', 'error');
            }
        }
    }

    /**
     * Send a message through the WebSocket
     * @param {Object|string} message - The message to send
     * @returns {boolean} True if the message was sent, false otherwise
     */
    send(message) {
        if (!this.connected || !this.socket) {
            console.error('WebSocket is not connected');
            return false;
        }

        try {
            const messageString = typeof message === 'string' ? message : JSON.stringify(message);
            this.socket.send(messageString);
            console.debug('WebSocket message sent:', message);
            return true;
        } catch (error) {
            console.error('Error sending WebSocket message:', error);
            this.emit('error', { type: 'send_error', error });
            return false;
        }
    }

    /**
     * Register an event handler
     * @param {string} event - The event name
     * @param {Function} callback - The callback function
     * @returns {WebSocketClient} The WebSocketClient instance for chaining
     */
    on(event, callback) {
        if (!event || typeof callback !== 'function') {
            console.warn('Invalid event handler registration:', { event, callback });
            return this;
        }

        if (!this.eventHandlers[event]) {
            this.eventHandlers[event] = [];
        }

        this.eventHandlers[event].push(callback);
        return this; // Allow chaining
    }

    /**
     * Remove an event handler
     * @param {string} event - The event name
     * @param {Function} [callback] - The specific callback to remove (or all if not provided)
     * @returns {WebSocketClient} The WebSocketClient instance for chaining
     */
    off(event, callback) {
        if (!event) {
            console.warn('No event specified for off()');
            return this;
        }

        if (!this.eventHandlers[event]) {
            return this;
        }

        if (callback) {
            this.eventHandlers[event] = this.eventHandlers[event].filter(cb => cb !== callback);

            // Clean up empty arrays to prevent memory leaks
            if (this.eventHandlers[event].length === 0) {
                delete this.eventHandlers[event];
            }
        } else {
            delete this.eventHandlers[event];
        }

        return this; // Allow chaining
    }

    /**
     * Emit an event with the given arguments
     * @param {string} event - The event name
     * @param {...*} args - Arguments to pass to the event handlers
     * @private
     */
    emit(event, ...args) {
        if (!event) {
            console.warn('No event specified for emit()');
            return;
        }

        const handlers = this.eventHandlers[event];
        if (!handlers || !handlers.length) {
            return;
        }

        // Create a copy of the handlers array to avoid issues if handlers are removed during iteration
        const handlersCopy = [...handlers];

        for (const handler of handlersCopy) {
            if (typeof handler === 'function') {
                try {
                    handler(...args);
                } catch (error) {
                    console.error(`Error in '${event}' event handler:`, error);
                }
            }
        }
    }

    /**
     * Report a status change; UI rendering is the app's job via the
     * 'status' event.
     * @param {string} message - Status message
     * @param {string} status - Status type (connected, disconnected, error, warning, etc.)
     * @private
     */
    updateStatus(message, status = 'info') {
        this.emit('status', { status, message });
    }

    disconnect() {
        if (this.socket) {
            // Suppress the reconnect loop for a deliberate disconnect
            this.reconnectAttempts = this.maxReconnectAttempts;
            this.socket.close();
            this.connected = false;
        }
        return this; // Allow chaining
    }

    isConnected() {
        return this.connected;
    }
}

// Make the class (not an instance) available globally
window.WebSocketClient = WebSocketClient;

// Main application class.
// Message type names come from /js/message-types.js, generated server-side
// from the MindMapMessage.MessageType enum — the single constants source.
class FreeMindApp {
    constructor(renderer, wsClient) {
        this.mindMapRenderer = renderer;
        this.wsClient = wsClient;
        this.currentMapId = null;
        this.isConnected = false;

        this.setupEventListeners();
    }

    /**
     * Show the local sample map, then connect. Connection is on demand —
     * nothing touches the network until start() is called.
     */
    start() {
        this.initializeSampleData();
        this.wsClient.connect();
    }

    /**
     * Set up event listeners for the application
     */
    setupEventListeners() {
        // Toolbar buttons (ids match index.html)
        document.getElementById('new-map').addEventListener('click', () => this.newMap());
        document.getElementById('save-map').addEventListener('click', () => this.saveMap());
        document.getElementById('open-map').addEventListener('click', () => {
            document.getElementById('file-input').click();
        });

        // File input handler
        document.getElementById('file-input').addEventListener('change', (e) => this.handleFileSelect(e));

        // WebSocket connection events
        this.wsClient
            .on('connected', () => this.handleWebSocketConnected())
            .on('disconnected', () => this.handleWebSocketDisconnected())
            .on('error', (error) => this.handleWebSocketError(error))
            .on('status', (status) => this.updateConnectionStatus(status));

        // WebSocket message events (kebab-case of the MessageType names)
        this.wsClient
            .on('system', (message) => this.handleSystemMessage(message))
            .on('map-loaded', (message) => this.handleMapLoaded(message))
            .on('node-created', (message) => this.handleNodeCreated(message))
            .on('node-updated', (message) => this.handleNodeUpdated(message))
            .on('node-moved', (message) => this.handleNodeMoved(message))
            .on('node-deleted', (message) => this.handleNodeDeleted(message))
            .on('user-connected', (message) => this.showStatus(message.text))
            .on('user-disconnected', (message) => this.showStatus(message.text));

        // Node interaction handlers
        this.mindMapRenderer.onNodeClick = (node) => this.handleNodeClick(node);
        this.mindMapRenderer.onNodeMove = (node) => this.handleNodeMove(node);

        // Window resize handler
        window.addEventListener('resize', () => this.mindMapRenderer.onResize());
    }

    /**
     * Handle WebSocket connection established
     */
    handleWebSocketConnected() {
        console.log('WebSocket connected');
        this.isConnected = true;
        this.updateConnectionStatus({ status: 'connected', message: 'Connected to server' });
    }

    /**
     * Handle WebSocket disconnection
     */
    handleWebSocketDisconnected() {
        console.log('WebSocket disconnected');
        this.isConnected = false;
        this.updateConnectionStatus({ status: 'disconnected', message: 'Disconnected from server' });
    }

    /**
     * Handle WebSocket errors
     */
    handleWebSocketError(error) {
        console.error('WebSocket error:', error);
        this.updateConnectionStatus({
            status: 'error',
            message: `Connection error: ${error.message || 'Unknown error'}`
        });
    }

    /**
     * Update the connection indicator and message in the header
     */
    updateConnectionStatus(status) {
        const indicator = document.getElementById('connection-indicator');
        if (indicator) {
            indicator.className = `status-indicator status-${status.status}`;
        }
        const statusElement = document.getElementById('connection-status');
        if (statusElement) {
            statusElement.textContent = status.message;
        }
    }

    /**
     * Show a transient message in the status bar
     */
    showStatus(text) {
        const statusMessage = document.getElementById('status-message');
        if (statusMessage) {
            statusMessage.textContent = text;
        }
    }

    /**
     * Handle server welcome / notifications
     */
    handleSystemMessage(message) {
        console.log('Server message:', message);
        this.showStatus(message.text);
    }

    /**
     * Handle incoming map data (arrives with M2's server-side map model)
     */
    handleMapLoaded(message) {
        console.log('Received map data:', message);
        if (message.data && message.data.nodes) {
            this.currentMapId = message.data.mapId || this.currentMapId;
            this.mindMapRenderer.setNodes(message.data.nodes);
            this.updateDocumentTitle(message.data.title);
        }
    }

    /**
     * Handle new node added by another client
     */
    handleNodeCreated(message) {
        if (message.data) {
            this.mindMapRenderer.addNode(message.data);
        }
    }

    /**
     * Handle node updated by another client
     */
    handleNodeUpdated(message) {
        if (message.nodeId && message.data) {
            this.mindMapRenderer.updateNode(message.nodeId, message.data);
        }
    }

    /**
     * Handle node moved by another client
     */
    handleNodeMoved(message) {
        if (message.nodeId && message.x !== undefined && message.y !== undefined) {
            this.mindMapRenderer.updateNode(message.nodeId, { x: message.x, y: message.y });
        }
    }

    /**
     * Handle node deleted by another client
     */
    handleNodeDeleted(message) {
        if (message.nodeId) {
            this.mindMapRenderer.removeNode(message.nodeId);
        }
    }

    /**
     * Handle node click event
     */
    handleNodeClick(node) {
        console.log('Node clicked:', node);
        // Node editing UI comes later
    }

    /**
     * Local node drag → relay the new position to the other clients
     */
    handleNodeMove(node) {
        if (!this.isConnected) return;

        this.wsClient.send({
            type: MessageType.NODE_MOVED,
            nodeId: node.id,
            x: node.x,
            y: node.y
        });
    }

    /**
     * Initialize with sample data for demonstration purposes.
     * Both browsers get the same node ids, so relayed operations apply
     * cleanly until M2 introduces the real server-side map.
     */
    initializeSampleData() {
        console.log('Initializing with sample data');

        const now = new Date().toISOString();
        const sampleNodes = [
            { id: '1', text: 'Central Topic', x: 400, y: 300, width: 140, height: 50,
              isRoot: true, color: '#4a90e2', createdAt: now, updatedAt: now },
            { id: '2', text: 'Main Topic 1', parentId: '1', x: 200, y: 450, width: 120,
              height: 40, color: '#50c878', createdAt: now, updatedAt: now },
            { id: '3', text: 'Main Topic 2', parentId: '1', x: 500, y: 450, width: 120,
              height: 40, color: '#ff7f50', createdAt: now, updatedAt: now },
            { id: '4', text: 'Sub Topic 1', parentId: '2', x: 100, y: 550, width: 100,
              height: 35, color: '#ffd700', createdAt: now, updatedAt: now },
            { id: '5', text: 'Sub Topic 2', parentId: '2', x: 220, y: 550, width: 100,
              height: 35, color: '#ff69b4', createdAt: now, updatedAt: now }
        ];

        this.mindMapRenderer.setNodes(sampleNodes);
        this.updateDocumentTitle('Sample Mind Map');
    }

    /**
     * Save the current mind map. Real persistence arrives with M2's
     * serializer; until then the server answers honestly that it can't.
     */
    saveMap() {
        fetch('/api/save', { method: 'POST' })
            .then((response) => response.json())
            .then((result) => {
                this.showStatus(result.message || result.status);
            })
            .catch((error) => {
                console.error('Save failed:', error);
                this.showStatus('Save failed: ' + error.message);
            });
    }

    /**
     * Update the document title with the current map name
     */
    updateDocumentTitle(title) {
        const appTitle = 'FreeMind Web';
        document.title = title ? title + ' | ' + appTitle : appTitle;
    }

    newMap() {
        if (confirm('Create a new mind map? Any unsaved changes will be lost.')) {
            this.currentMapId = null;
            this.mindMapRenderer.setNodes([
                { id: '1', text: 'Central Topic', x: 400, y: 300, width: 140, height: 50, isRoot: true }
            ]);
            this.updateDocumentTitle('New Mind Map');
        }
    }

    handleFileSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            // .mm parsing arrives with M2's server-side serializer; the
            // browser never reimplements the format.
            console.log('File selected:', file.name, '-', e.target.result.length, 'bytes');
            this.showStatus('Opening .mm files arrives with server-side save/open');
        };
        reader.readAsText(file);
    }
}

// Initialize the application when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    try {
        const renderer = new MindMapRenderer('mindmap-canvas');
        const wsClient = new WebSocketClient();
        const app = new FreeMindApp(renderer, wsClient);

        // Expose for debugging
        window.app = app;
        window.mindMapRenderer = renderer;
        window.wsClient = wsClient;

        app.start();
        console.log('FreeMind Web application initialized');
    } catch (error) {
        console.error('Failed to initialize application:', error);

        const errorContainer = document.createElement('div');
        errorContainer.style.position = 'fixed';
        errorContainer.style.top = '0';
        errorContainer.style.left = '0';
        errorContainer.style.right = '0';
        errorContainer.style.padding = '20px';
        errorContainer.style.background = '#ffebee';
        errorContainer.style.color = '#b71c1c';
        errorContainer.style.borderBottom = '1px solid #ef9a9a';
        errorContainer.style.fontFamily = 'Arial, sans-serif';
        errorContainer.style.zIndex = '10000';

        errorContainer.innerHTML = '<h2>Application Error</h2>' +
            '<p>' + (error.message || 'An unknown error occurred') + '</p>' +
            '<p>Please check the console for more details.</p>' +
            '<button onclick="location.reload()" style="margin-top: 10px; padding: 5px 10px;">' +
            '    Reload Page' +
            '</button>';

        document.body.prepend(errorContainer);
    }
});

// Main application class
class FreeMindApp {
    constructor() {
        this.mindMapRenderer = mindMapRenderer;
        this.currentMapId = null;
        this.isConnected = false;
        
        // Initialize WebSocket client
        this.wsClient = window.wsClient || new WebSocketClient();
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Initialize with sample data if not connected to a server
        // In a real app, we'd wait for the WebSocket connection
        this.initializeSampleData();
    }

    /**
     * Set up event listeners for the application
     */
    setupEventListeners() {
        // Toolbar buttons
        document.getElementById('newMap').addEventListener('click', () => this.newMap());
        document.getElementById('saveMap').addEventListener('click', () => this.saveMap());
        document.getElementById('openMap').addEventListener('click', () => {
            document.getElementById('fileInput').click();
        });

        // File input handler
        document.getElementById('fileInput').addEventListener('change', (e) => this.handleFileSelect(e));

        // WebSocket event handlers
        this.wsClient
            .on('connected', () => this.handleWebSocketConnected())
            .on('disconnected', () => this.handleWebSocketDisconnected())
            .on('error', (error) => this.handleWebSocketError(error))
            .on('status', (status) => this.updateConnectionStatus(status));
            
        // WebSocket message handlers
        this.wsClient
            .on('welcome', (message) => this.handleWelcomeMessage(message))
            .on('map-data', (message) => this.handleMapData(message))
            .on('node-added', (message) => this.handleNodeAdded(message))
            .on('node-updated', (message) => this.handleNodeUpdated(message))
            .on('node-deleted', (message) => this.handleNodeDeleted(message));
            
        // Node interaction handlers
        this.mindMapRenderer.onNodeClick = (node) => this.handleNodeClick(node);
        this.mindMapRenderer.onNodeUpdate = (node) => this.handleNodeUpdate(node);
        
        // Window resize handler
        window.addEventListener('resize', () => this.handleWindowResize());
    }
    
    /**
     * Handle WebSocket connection established
     */
    handleWebSocketConnected() {
        console.log('WebSocket connected');
        this.isConnected = true;
        this.updateConnectionStatus({ status: 'connected', message: 'Connected to server' });
        
        // Request initial map data if we have a map ID, otherwise create a new map
        if (this.currentMapId) {
            this.loadMap(this.currentMapId);
        } else {
            this.createNewMap();
        }
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
     * Update the connection status UI
     */
    updateConnectionStatus(status) {
        const statusElement = document.getElementById('connection-status');
        if (statusElement) {
            statusElement.textContent = status.message;
            statusElement.className = `status-${status.status}`;
        }
    }
    
    /**
     * Handle welcome message from server
     */
    handleWelcomeMessage(message) {
        console.log('Server welcome:', message);
        // Server might send initial data or configuration
    }
    
    /**
     * Handle incoming map data
     */
    handleMapData(message) {
        console.log('Received map data:', message);
        if (message.data && message.data.nodes) {
            this.currentMapId = message.mapId || this.currentMapId;
            this.mindMapRenderer.setNodes(message.data.nodes);
            this.updateDocumentTitle();
        }
    }
    
    /**
     * Handle new node added by another client
     */
    handleNodeAdded(message) {
        if (message.data) {
            this.mindMapRenderer.addNode(message.data);
        }
    }
    
    /**
     * Handle node updated by another client
     */
    handleNodeUpdated(message) {
        if (message.data && message.data.id) {
            this.mindMapRenderer.updateNode(message.data.id, message.data);
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
        // You can add node editing UI here
    }
    
    /**
     * Handle node update from the renderer
     */
    handleNodeUpdate(node) {
        if (!this.isConnected) return;
        
        this.wsClient.send({
            type: 'NODE_UPDATED',
            mapId: this.currentMapId,
            data: node
        });
    }
    
    /**
     * Handle window resize
     */
    handleWindowResize() {
        this.mindMapRenderer.onResize();
    }

    /**
     * Initialize with sample data for demonstration purposes
     */
    initializeSampleData() {
        // Only use sample data if not connected to a server
        if (this.isConnected) return;
        
        console.log('Initializing with sample data');
        
        const sampleNodes = [
            {
                id: '1',
                text: 'Central Topic',
                x: 400,
                y: 300,
                width: 140,
                height: 50,
                isRoot: true,
                color: '#4a90e2',
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            },
            {
                id: '2',
                text: 'Main Topic 1',
                parentId: '1',
                x: 200,
                y: 450,
                width: 120,
                height: 40,
                color: '#50c878',
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            },
            {
                id: '3',
                text: 'Main Topic 2',
                parentId: '1',
                x: 500,
                y: 450,
                width: 120,
                height: 40,
                color: '#ff7f50',
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            },
            {
                id: '4',
                text: 'Sub Topic 1',
                parentId: '2',
                x: 100,
                y: 550,
                width: 100,
                height: 35,
                color: '#ffd700',
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            },
            {
                id: '5',
                text: 'Sub Topic 2',
                parentId: '2',
                x: 220,
                y: 550,
                width: 100,
                height: 35,
                color: '#ff69b4',
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            }
        ];

        this.mindMapRenderer.setNodes(sampleNodes);
        this.updateDocumentTitle('Sample Mind Map');
    }
    
    /**
     * Create a new mind map
     */
    createNewMap() {
        if (this.isConnected) {
            this.wsClient.send({
                type: 'CREATE_MAP',
                data: {
                    title: 'New Mind Map',
                    description: 'Created ' + new Date().toLocaleString()
                }
            });
        } else {
            // If not connected, just clear the current map
            this.mindMapRenderer.clear();
            this.currentMapId = null;
            this.initializeSampleData();
        }
    }
    
    /**
     * Load a mind map by ID
     */
    loadMap(mapId) {
        if (!mapId) return;
        
        if (this.isConnected) {
            this.wsClient.send({
                type: 'GET_MAP',
                mapId: mapId
            });
        }
    }
    
    /**
     * Save the current mind map
     */
    saveMap() {
        if (!this.isConnected) {
            alert('Cannot save: Not connected to server');
            return;
        }
        
        const nodes = this.mindMapRenderer.getNodes();
        
        this.wsClient.send({
            type: 'SAVE_MAP',
            mapId: this.currentMapId,
            data: {
                nodes: nodes,
                updatedAt: new Date().toISOString()
            }
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
            this.mindMapRenderer.setNodes([
                {
                    id: '1',
                    text: 'Central Topic',
                    x: 400,
                    y: 300,
                    width: 140,
                    height: 50,
                    isRoot: true
                }
            ]);
        }
    }

    saveMap() {
        // In a real app, this would send the current map to the server
        console.log('Saving map...');
        // For now, just show a message
        alert('Map saved successfully!');
    }

    handleFileSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                // In a real app, this would parse the FreeMind .mm file format
                // For now, we'll just log the file content
                console.log('File content:', e.target.result);
                alert('File loaded successfully! (Not actually parsed in this demo)');
            } catch (error) {
                console.error('Error parsing file:', error);
                alert('Error loading file: ' + error.message);
            }
        };
        reader.readAsText(file);
    }
}

// Initialize the application when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    try {
        // Initialize the mind map renderer
        const canvas = document.getElementById('mindmap-canvas');
        if (!canvas) {
            throw new Error('Mind map canvas element not found');
        }
        
        // Create the renderer
        const renderer = new MindMapRenderer(canvas);
        
        // Initialize the application
        const app = new FreeMindApp(renderer);
        
        // Expose app and renderer globally for debugging
        window.app = app;
        window.mindMapRenderer = renderer;
        
        console.log('FreeMind Web application initialized');
    } catch (error) {
        console.error('Failed to initialize application:', error);
        
        // Show error message to user
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

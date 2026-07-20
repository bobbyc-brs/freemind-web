class MindMapRenderer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.scale = 1;
        this.offset = { x: 0, y: 0 };
        this.isDragging = false;
        this.lastMousePos = { x: 0, y: 0 };
        this.nodes = [];
        this.selectedNode = null;
        this.hoveredNode = null;
        
        this.setupEventListeners();
        this.resizeCanvas();
        window.addEventListener('resize', () => this.resizeCanvas());
    }
    
    setupEventListeners() {
        // Panning
        this.canvas.addEventListener('mousedown', (e) => {
            if (e.button === 1 || (e.button === 0 && e.ctrlKey)) { // Middle mouse or Ctrl+Left
                this.isDragging = true;
                this.lastMousePos = this.getMousePos(e);
                this.canvas.style.cursor = 'grabbing';
                e.preventDefault();
            }
        });
        
        document.addEventListener('mousemove', (e) => {
            if (this.isDragging) {
                const mousePos = this.getMousePos(e);
                this.offset.x += (mousePos.x - this.lastMousePos.x);
                this.offset.y += (mousePos.y - this.lastMousePos.y);
                this.lastMousePos = mousePos;
                this.render();
                e.preventDefault();
            }
        });
        
        document.addEventListener('mouseup', () => {
            this.isDragging = false;
            this.canvas.style.cursor = 'default';
        });
        
        // Zooming
        this.canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const mousePos = this.getMousePos(e);
            const delta = -Math.sign(e.deltaY) * 0.1;
            this.zoom(mousePos.x, mousePos.y, delta);
        });
        
        // Node interaction
        this.canvas.addEventListener('click', (e) => {
            const mousePos = this.getMousePos(e);
            this.handleNodeClick(mousePos.x, mousePos.y);
        });
        
        this.canvas.addEventListener('mousemove', (e) => {
            const mousePos = this.getMousePos(e);
            this.handleNodeHover(mousePos.x, mousePos.y);
        });
    }
    
    getMousePos(e) {
        const rect = this.canvas.getBoundingClientRect();
        return {
            x: (e.clientX - rect.left - this.offset.x) / this.scale,
            y: (e.clientY - rect.top - this.offset.y) / this.scale
        };
    }
    
    resizeCanvas() {
        this.canvas.width = this.canvas.offsetWidth;
        this.canvas.height = this.canvas.offsetHeight;
        this.render();
    }
    
    zoom(x, y, delta) {
        const newScale = Math.min(Math.max(0.1, this.scale + delta), 5);
        const ratio = 1 - newScale / this.scale;
        
        this.offset.x += (x * ratio);
        this.offset.y += (y * ratio);
        this.scale = newScale;
        
        this.render();
    }
    
    setNodes(nodes) {
        this.nodes = nodes;
        this.render();
    }
    
    addNode(node) {
        this.nodes.push(node);
        this.render();
    }
    
    updateNode(id, updates) {
        const node = this.nodes.find(n => n.id === id);
        if (node) {
            Object.assign(node, updates);
            this.render();
        }
    }
    
    handleNodeClick(x, y) {
        // Find if a node was clicked
        for (const node of this.nodes) {
            if (this.isPointInNode(x, y, node)) {
                this.selectedNode = node;
                this.render();
                // Emit event or call callback
                if (this.onNodeClick) {
                    this.onNodeClick(node);
                }
                return;
            }
        }
        this.selectedNode = null;
        this.render();
    }
    
    handleNodeHover(x, y) {
        let foundHover = false;
        for (const node of this.nodes) {
            if (this.isPointInNode(x, y, node)) {
                this.hoveredNode = node;
                this.canvas.style.cursor = 'pointer';
                foundHover = true;
                break;
            }
        }
        
        if (!foundHover && this.hoveredNode) {
            this.hoveredNode = null;
            this.canvas.style.cursor = 'default';
        }
    }
    
    isPointInNode(x, y, node) {
        // Simple bounding box check
        return x >= node.x && x <= node.x + node.width &&
               y >= node.y && y <= node.y + node.height;
    }
    
    render() {
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Apply transformations
        this.ctx.save();
        this.ctx.translate(this.offset.x, this.offset.y);
        this.ctx.scale(this.scale, this.scale);
        
        // Draw connections first (behind nodes)
        this.drawConnections();
        
        // Then draw nodes
        for (const node of this.nodes) {
            this.drawNode(node);
        }
        
        this.ctx.restore();
    }
    
    drawConnections() {
        this.ctx.strokeStyle = '#999';
        this.ctx.lineWidth = 2;
        
        for (const node of this.nodes) {
            if (node.parentId) {
                const parent = this.nodes.find(n => n.id === node.parentId);
                if (parent) {
                    this.drawConnection(parent, node);
                }
            }
        }
    }
    
    drawConnection(node1, node2) {
        const x1 = node1.x + node1.width / 2;
        const y1 = node1.y + node1.height / 2;
        const x2 = node2.x + node2.width / 2;
        const y2 = node2.y + node2.height / 2;
        
        this.ctx.beginPath();
        this.ctx.moveTo(x1, y1);
        this.ctx.bezierCurveTo(x1, y2, x2, y1, x2, y2);
        this.ctx.stroke();
    }
    
    drawNode(node) {
        const isSelected = this.selectedNode && this.selectedNode.id === node.id;
        const isHovered = this.hoveredNode && this.hoveredNode.id === node.id;
        
        // Draw node background
        this.ctx.fillStyle = isSelected ? '#e1f5fe' : '#ffffff';
        this.ctx.strokeStyle = isSelected ? '#2196f3' : isHovered ? '#64b5f6' : '#90caf9';
        this.ctx.lineWidth = isSelected ? 2 : 1;
        
        this.ctx.beginPath();
        this.roundRect(
            this.ctx,
            node.x,
            node.y,
            node.width || 120,
            node.height || 40,
            5
        );
        this.ctx.fill();
        this.ctx.stroke();
        
        // Draw node text
        this.ctx.fillStyle = '#000000';
        this.ctx.font = '14px Arial';
        this.ctx.textBaseline = 'middle';
        this.ctx.textAlign = 'center';
        this.ctx.fillText(
            node.text || 'New Node',
            node.x + (node.width || 120) / 2,
            node.y + (node.height || 40) / 2
        );
    }
    
    roundRect(ctx, x, y, width, height, radius) {
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + width - radius, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
        ctx.lineTo(x + width, y + height - radius);
        ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
        ctx.lineTo(x + radius, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
    }
}

// Create a global instance
const mindMapRenderer = new MindMapRenderer('mindmap-canvas');

// Example usage:
// mindMapRenderer.setNodes([
//     { id: '1', text: 'Root', x: 200, y: 200, width: 120, height: 40 },
//     { id: '2', text: 'Child 1', parentId: '1', x: 100, y: 300, width: 100, height: 35 },
//     { id: '3', text: 'Child 2', parentId: '1', x: 250, y: 300, width: 100, height: 35 }
// ]);

// frontend/js/GraphBoardRenderer.js

class GraphBoardRenderer {
    constructor(boardProperties, app) {
        this.props = {
            defaultNodeSize: 20,
            defaultNodeColor: 0xCCCCCC,
            defaultEdgeColor: 0xAAAAAA,
            defaultEdgeThickness: 2,
            labelStyle: new PIXI.TextStyle({
                fontFamily: 'Arial',
                fontSize: 12,
                fill: 0x000000,
                align: 'center'
            }),
            ...boardProperties
        };
        this.app = app;
        this.container = new PIXI.Container();
        this.container.interactive = true;

        this.edgeGraphics = new PIXI.Graphics();
        this.nodeGraphics = new PIXI.Graphics();
        this.overlayLayer = new PIXI.Container(); // For highlights and icons on nodes
        this.labelContainer = new PIXI.Container();

        this.container.addChild(this.edgeGraphics);
        this.container.addChild(this.nodeGraphics);
        this.container.addChild(this.overlayLayer); // Add overlay layer
        this.container.addChild(this.labelContainer);
        this.app.stage.addChild(this.container); // Add main container to stage

        this.nodeObjects = {};
    }

    drawGraph() {
        this.nodeGraphics.clear();
        this.edgeGraphics.clear();
        this.labelContainer.removeChildren();
        // this.overlayLayer.removeChildren(); // Overlays cleared by drawOverlays if called by GameRenderer
        this.nodeObjects = {};

        if (this.props.edges) {
            this.props.edges.forEach(edge => {
                const fromNode = this.props.nodes.find(n => n.id === edge.from);
                const toNode = this.props.nodes.find(n => n.id === edge.to);
                if (fromNode && toNode) {
                    this.edgeGraphics.lineStyle(edge.thickness || this.props.defaultEdgeThickness, edge.color || this.props.defaultEdgeColor, 1);
                    this.edgeGraphics.moveTo(fromNode.x, fromNode.y);
                    this.edgeGraphics.lineTo(toNode.x, toNode.y);
                }
            });
        }

        if (this.props.nodes) {
            this.props.nodes.forEach(node => {
                const size = node.size || this.props.defaultNodeSize;
                const color = node.color || this.props.defaultNodeColor;
                this.nodeGraphics.beginFill(color);
                this.nodeGraphics.lineStyle(1, 0x000000);
                this.nodeGraphics.drawCircle(node.x, node.y, size / 2);
                this.nodeGraphics.endFill();

                this.nodeObjects[node.id] = {
                    x: node.x,
                    y: node.y,
                    size: size,
                    id: node.id
                };

                if (node.label) {
                    const text = new PIXI.Text(node.label, this.props.labelStyle);
                    text.anchor.set(0.5);
                    text.x = node.x;
                    text.y = node.y + size / 2 + 8;
                    this.labelContainer.addChild(text);
                }
            });
        }
    }

    drawOverlays(overlaysData) {
        this.overlayLayer.removeChildren(); // Clear previous overlays
        if (!overlaysData) return;

        overlaysData.forEach(overlay => {
            const color = overlay.color || 0x00FF00; // Default green highlight
            const alpha = overlay.alpha || 0.3;

            overlay.sites.forEach(nodeId => { // siteKey for graph is the node ID
                const node = this.nodeObjects[nodeId];
                if (!node) return;

                const iconSize = overlay.size || node.size * 0.8; // Icon size relative to node size

                if (overlay.type === "highlight_sites") {
                    const highlight = new PIXI.Graphics();
                    highlight.beginFill(color, alpha);
                    // Draw a slightly larger circle for highlight
                    highlight.drawCircle(node.x, node.y, (node.size / 2) + 5); // 5px padding
                    highlight.endFill();
                    this.overlayLayer.addChild(highlight);
                } else if (overlay.type === "icon_on_sites" && overlay.imageUrl) {
                     try {
                        const texture = PIXI.Texture.from(overlay.imageUrl);
                        const icon = new PIXI.Sprite(texture);
                        icon.anchor.set(0.5);
                        icon.width = iconSize;
                        icon.height = iconSize;
                        icon.position.set(node.x, node.y);
                        icon.alpha = overlay.alpha || 1.0;
                        this.overlayLayer.addChild(icon);
                    } catch (e) { console.error("Error loading overlay icon for graph node:", overlay.imageUrl, e); }
                }
            });
        });
    }

    getContainer() {
        return this.container;
    }

    getNodePosition(nodeId) {
        const node = this.nodeObjects[nodeId];
        return node ? { x: node.x, y: node.y } : null;
    }

    getCellCenter(nodeId) { // Alias for GameRenderer consistency
        return this.getNodePosition(nodeId);
    }

    pixelToGrid(pixelX, pixelY) {
        for (const nodeId in this.nodeObjects) {
            const node = this.nodeObjects[nodeId];
            const dx = pixelX - node.x;
            const dy = pixelY - node.y;
            const distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < (node.size / 2) * (node.size / 2)) {
                return { type: "graph", nodeId: node.id };
            }
        }
        return null;
    }
}
window.GraphBoardRenderer = GraphBoardRenderer;

// frontend/js/HexBoardRenderer.js

class HexBoardRenderer {
    constructor(boardProperties, app) {
        this.props = {
            hexRadius: 30,
            lineWidth: 2,
            strokeColor: 0x000000,
            fillColor: 0xCCCCCC, // Default hex fill
            rows: 5,
            cols: 5,
            orientation: 'pointy-top',
            ...boardProperties
        };
        this.app = app;
        this.graphics = new PIXI.Graphics(); // For hex grid lines and fills
        this.overlayLayer = new PIXI.Container(); // For highlights, icons
        this.container = new PIXI.Container();
        this.container.interactive = true;

        this.container.addChild(this.graphics);
        this.container.addChild(this.overlayLayer);
        this.app.stage.addChild(this.container);

        this.hexWidth = this.props.orientation === 'pointy-top' ? Math.sqrt(3) * this.props.hexRadius : 2 * this.props.hexRadius;
        this.hexHeight = this.props.orientation === 'pointy-top' ? 2 * this.props.hexRadius : Math.sqrt(3) * this.props.hexRadius;
    }

    drawHex(centerX, centerY, fillColor) {
        const currentFill = fillColor !== undefined ? fillColor : this.props.fillColor;
        this.graphics.beginFill(currentFill);
        this.graphics.lineStyle(this.props.lineWidth, this.props.strokeColor, 1);

        const points = [];
        for (let i = 0; i < 6; i++) {
            let angle;
            if (this.props.orientation === 'pointy-top') {
                angle = (Math.PI / 3) * i + (Math.PI / 6);
            } else {
                angle = (Math.PI / 3) * i;
            }
            points.push(new PIXI.Point(
                centerX + this.props.hexRadius * Math.cos(angle),
                centerY + this.props.hexRadius * Math.sin(angle)
            ));
        }
        this.graphics.drawPolygon(points);
        this.graphics.endFill();
    }

    axialToPixel(q, r) {
        let x, y;
        const radius = this.props.hexRadius;
        if (this.props.orientation === 'pointy-top') {
            x = radius * (Math.sqrt(3) * q  +  Math.sqrt(3)/2 * r);
            y = radius * (3./2 * r);
        } else {
            x = radius * (3./2 * q);
            y = radius * (Math.sqrt(3)/2 * q  +  Math.sqrt(3) * r);
        }
        // This basic conversion places 0,0 of axial at 0,0 pixel.
        // An offset is needed to center the grid on the canvas.
        // For now, GameRenderer will handle overall positioning of this container.
        // Or, apply a default offset here based on canvas size:
        // const offsetX = this.app.screen.width / 2;
        // const offsetY = this.app.screen.height / 2;
        // return { x: x + offsetX, y: y + offsetY };
        return { x: x, y: y }; // Return relative to container origin
    }

    drawGrid() {
        this.graphics.clear();
        const qMin = -Math.floor(this.props.cols / 2);
        const qMax = Math.ceil(this.props.cols / 2);
        const rMin = -Math.floor(this.props.rows / 2);
        const rMax = Math.ceil(this.props.rows / 2);

        for (let q = qMin; q < qMax; q++) {
            for (let r = rMin; r < rMax; r++) {
                 if (this.props.layout === 'hexagonal' && Math.abs(q + r) > Math.max(Math.abs(qMin), Math.abs(rMin))) { // Simple hexagonal clip
                    continue;
                 }
                const {x, y} = this.axialToPixel(q, r);
                // Example alternating fill for hexes
                const fillColor = (q % 2 === 0 && r % 2 === 0) || (q % 2 !== 0 && r % 2 !== 0) ? this.props.fillColor : (this.props.fillColorAlternate || 0xE0E0E0);
                this.drawHex(x, y, fillColor);
            }
        }
        // Position the container itself if needed, e.g., to center the drawn grid
        // This might be better done in GameRenderer after knowing the full grid bounds
    }

    drawOverlays(overlaysData) {
        this.overlayLayer.removeChildren();
        if (!overlaysData) return;

        overlaysData.forEach(overlay => {
            const color = overlay.color || 0x00FF00;
            const alpha = overlay.alpha || 0.3;
            const iconSize = overlay.size || this.props.hexRadius * 1.5; // Icon size relative to hex radius

            overlay.sites.forEach(siteKey => { // siteKey for hex is "q,r" e.g. "0,0", "1,-1"
                const parts = siteKey.split(',');
                if (parts.length !== 2) return;
                const q = parseInt(parts[0], 10);
                const r = parseInt(parts[1], 10);

                const hexCenter = this.axialToPixel(q, r);

                if (overlay.type === "highlight_sites") {
                    const highlightGraphics = new PIXI.Graphics();
                    // Draw a hex highlight shape
                    const points = [];
                    for (let i = 0; i < 6; i++) {
                        let angle = (this.props.orientation === 'pointy-top') ?
                                    (Math.PI / 3) * i + (Math.PI / 6) : (Math.PI / 3) * i;
                        points.push(new PIXI.Point(
                            hexCenter.x + this.props.hexRadius * Math.cos(angle),
                            hexCenter.y + this.props.hexRadius * Math.sin(angle)
                        ));
                    }
                    highlightGraphics.beginFill(color, alpha);
                    highlightGraphics.drawPolygon(points);
                    highlightGraphics.endFill();
                    this.overlayLayer.addChild(highlightGraphics);
                } else if (overlay.type === "icon_on_sites" && overlay.imageUrl) {
                    try {
                        const texture = PIXI.Texture.from(overlay.imageUrl);
                        const icon = new PIXI.Sprite(texture);
                        icon.anchor.set(0.5);
                        icon.width = iconSize;
                        icon.height = iconSize;
                        icon.position.set(hexCenter.x, hexCenter.y);
                        icon.alpha = overlay.alpha || 1.0;
                        this.overlayLayer.addChild(icon);
                    } catch (e) { console.error("Error loading overlay icon for hex:", overlay.imageUrl, e); }
                }
            });
        });
    }

    pixelToGrid(pixelX, pixelY) {
        // Convert pixel to axial coordinates (approximate for now)
        const x = pixelX; // Assuming pixelX, pixelY are already local to the container
        const y = pixelY;
        let q, r;
        const radius = this.props.hexRadius;
        if (this.props.orientation === 'pointy-top') {
            q = (Math.sqrt(3)/3 * x  -  1./3 * y) / radius;
            r = (2./3 * y) / radius;
        } else { // flat-top
            q = (2./3 * x) / radius;
            r = (-1./3 * x  +  Math.sqrt(3)/3 * y) / radius;
        }
        // TODO: Implement proper hex rounding (e.g. using cube coordinates)
        // For now, simple rounding, may not be perfectly accurate for edge clicks
        const cq = Math.round(q);
        const cr = Math.round(r);
        // const cs = Math.round(-q-r); // if using cube for rounding
        // Need to convert back from cube to axial if using that method for rounding
        return { type: "hex", q: cq, r: cr };
    }

    getCellCenter(q, r) {
        return this.axialToPixel(q,r);
    }

    getContainer() {
        return this.container;
    }
}
window.HexBoardRenderer = HexBoardRenderer;

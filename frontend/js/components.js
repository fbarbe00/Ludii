// frontend/js/components.js

/**
 * Represents and renders a generic grid-based game board.
 */
class BoardRenderer {
    constructor(boardProperties, app) {
        this.props = { ...boardProperties };
        this.app = app;
        this.graphics = new PIXI.Graphics(); // For grid lines, cell fills
        this.overlayLayer = new PIXI.Container(); // For highlights, icons on cells
        this.container = new PIXI.Container();
        this.container.interactive = true;

        this.container.addChild(this.graphics);
        this.container.addChild(this.overlayLayer); // Overlay on top of grid graphics
        this.app.stage.addChild(this.container);
    }

    drawGrid() {
        this.graphics.clear();
        const strokeColor = this.props.strokeColor !== undefined ? this.props.strokeColor : 0x000000;
        const lineWidth = this.props.lineWidth !== undefined ? this.props.lineWidth : 2;
        this.graphics.lineStyle(lineWidth, strokeColor, 1);

        const totalWidth = this.props.cols * this.props.cellWidth;
        const totalHeight = this.props.rows * this.props.cellHeight;

        if (this.props.cellFillEven !== undefined || this.props.cellFillOdd !== undefined) {
            for (let r = 0; r < this.props.rows; r++) {
                for (let c = 0; c < this.props.cols; c++) {
                    const x = c * this.props.cellWidth;
                    const y = r * this.props.cellHeight;
                    const isEven = (r + c) % 2 === 0;
                    const fillColor = isEven ? this.props.cellFillEven : this.props.cellFillOdd;
                    if (fillColor !== undefined) {
                        this.graphics.beginFill(fillColor);
                        this.graphics.drawRect(x, y, this.props.cellWidth, this.props.cellHeight);
                        this.graphics.endFill();
                    }
                }
            }
        }

        this.graphics.lineStyle(lineWidth, strokeColor, 1);
        for (let i = 0; i <= this.props.cols; i++) {
            this.graphics.moveTo(i * this.props.cellWidth, 0);
            this.graphics.lineTo(i * this.props.cellWidth, totalHeight);
        }
        for (let i = 0; i <= this.props.rows; i++) {
            this.graphics.moveTo(0, i * this.props.cellHeight);
            this.graphics.lineTo(totalWidth, i * this.props.cellHeight);
        }
    }

    drawOverlays(overlaysData) {
        this.overlayLayer.removeChildren(); // Clear previous overlays

        if (!overlaysData) return;

        overlaysData.forEach(overlay => {
            const color = overlay.color || 0x00FF00; // Default green highlight
            const alpha = overlay.alpha || 0.3;
            const size = overlay.size || Math.min(this.props.cellWidth, this.props.cellHeight) * 0.8;

            overlay.sites.forEach(siteKey => { // siteKey for grid is "row,col" e.g. "1,2"
                const parts = siteKey.split(',');
                if (parts.length !== 2) return;
                const r = parseInt(parts[0], 10);
                const c = parseInt(parts[1], 10);

                if (r < 0 || r >= this.props.rows || c < 0 || c >= this.props.cols) return;

                const cellPos = this.getCellCenter(r,c);

                if (overlay.type === "highlight_sites") {
                    const highlight = new PIXI.Graphics();
                    highlight.beginFill(color, alpha);
                    // Draw a rectangle covering the cell
                    highlight.drawRect(c * this.props.cellWidth, r * this.props.cellHeight, this.props.cellWidth, this.props.cellHeight);
                    highlight.endFill();
                    this.overlayLayer.addChild(highlight);
                } else if (overlay.type === "icon_on_sites" && overlay.imageUrl) {
                    try {
                        const texture = PIXI.Texture.from(overlay.imageUrl);
                        const icon = new PIXI.Sprite(texture);
                        icon.anchor.set(0.5);
                        icon.width = size;
                        icon.height = size;
                        icon.position.set(cellPos.x, cellPos.y);
                        icon.alpha = overlay.alpha || 1.0;
                        this.overlayLayer.addChild(icon);
                    } catch (e) { console.error("Error loading overlay icon:", overlay.imageUrl, e); }
                }
            });
        });
    }


    pixelToGrid(x, y) {
        const totalWidth = this.props.cols * this.props.cellWidth;
        const totalHeight = this.props.rows * this.props.cellHeight;
        if (x < 0 || y < 0 || x > totalWidth || y > totalHeight) {
            return null;
        }
        const col = Math.floor(x / this.props.cellWidth);
        const row = Math.floor(y / this.props.cellHeight);
        if (col >= this.props.cols || row >= this.props.rows) return null;
        return { type: "grid", row, col };
    }

    getCellCenter(row, col) {
        return {
            x: col * this.props.cellWidth + this.props.cellWidth / 2,
            y: row * this.props.cellHeight + this.props.cellHeight / 2
        };
    }

    getContainer() {
        return this.container;
    }

    updateProperties(newProperties) {
        this.props = { ...this.props, ...newProperties };
        this.drawGrid();
    }
}

class PieceRenderer {
    constructor(pieceData, cellWidth, cellHeight, app) {
        this.pieceData = pieceData;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.app = app;
        this.sprite = null;
    }

    draw(posX, posY) {
        const displayProps = this.pieceData.display || {};

        if (this.sprite) {
           this.sprite.destroy();
        }

        if (displayProps.imageUrl) {
            try {
                const texture = PIXI.Texture.from(displayProps.imageUrl);
                this.sprite = new PIXI.Sprite(texture);
                this.sprite.anchor.set(0.5);
                const pieceRenderWidth = displayProps.width || this.cellWidth * 0.8;
                const pieceRenderHeight = displayProps.height || this.cellHeight * 0.8;
                this.sprite.width = pieceRenderWidth;
                this.sprite.height = pieceRenderHeight;

                if (displayProps.scale) {
                    this.sprite.scale.set(displayProps.scale);
                }
            } catch (e) {
                console.error("Error loading piece texture:", displayProps.imageUrl, e);
                this._drawShapeOrText(displayProps, posX, posY);
            }
        } else {
            this._drawShapeOrText(displayProps, posX, posY);
        }

        if (this.sprite) {
            this.sprite.position.set(posX, posY);
            return this.sprite;
        }
        return null;
    }

    _drawShapeOrText(displayProps, centerX, centerY) {
        const graphics = new PIXI.Graphics();
        const minCellDim = Math.min(this.cellWidth, this.cellHeight);
        const sizeFactor = displayProps.size || 0.7;
        const size = sizeFactor * minCellDim / 2;
        const color = displayProps.color !== undefined ? displayProps.color : 0x000000;

        this.sprite = graphics;
        this.sprite.position.set(centerX, centerY);

        graphics.beginFill(color);
        if (displayProps.shape === 'circle') {
            graphics.drawCircle(0, 0, size);
        } else if (displayProps.shape === 'rect') {
            const rectWidth = displayProps.width || size * 2;
            const rectHeight = displayProps.height || size * 2;
            graphics.drawRect(-rectWidth / 2, -rectHeight / 2, rectWidth, rectHeight);
        } else {
            const pieceText = this.pieceData.text || (this.pieceData.type ? String(this.pieceData.type).substring(0,1) : "?");
            const fontSize = displayProps.fontSize || Math.max(10, minCellDim * 0.5 * sizeFactor);
            const textStyle = new PIXI.TextStyle({
                fontFamily: 'Arial',
                fontSize: fontSize,
                fill: color,
                align: 'center',
                fontWeight: 'bold'
            });
            if(this.sprite === graphics) this.sprite.destroy();

            this.sprite = new PIXI.Text(pieceText, textStyle);
            this.sprite.anchor.set(0.5);
            return;
        }
        graphics.endFill();
    }
}

window.BoardRenderer = BoardRenderer;
window.PieceRenderer = PieceRenderer;

// Note: HexBoardRenderer and GraphBoardRenderer will be in separate files
// For this step, only BoardRenderer is modified for overlays.
// If HexBoardRenderer and GraphBoardRenderer were in this file, they'd get similar overlayLayer and drawOverlays methods.

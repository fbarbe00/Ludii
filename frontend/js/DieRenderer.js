// frontend/js/DieRenderer.js

class DieRenderer {
    /**
     * @param {object} dieData - e.g., { id: "d1", value: 6, type: "standard_d6", size: 40 }
     * @param {object} properties - from gameState.pieceProperties[dieData.type] or gameState.diceProperties[dieData.type]
     *                              e.g., { faceColor: 0xFFFFFF, pipColor: 0x000000, strokeColor: 0x000000 }
     * @param {PIXI.Application} app
     */
    constructor(dieData, properties, app) {
        this.dieData = dieData;
        this.props = properties || {};
        this.app = app;
        this.container = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.container.addChild(this.graphics);
    }

    /**
     * @param {number} x - X position of the die's top-left corner
     * @param {number} y - Y position of the die's top-left corner
     */
    draw(x, y) {
        this.container.position.set(x, y);
        this.graphics.clear();

        const size = this.dieData.size || 40;
        const faceColor = this.props.faceColor || 0xFFFFFF;
        const pipColor = this.props.pipColor || 0x000000;
        const strokeColor = this.props.strokeColor || 0x000000;
        const cornerRadius = size * 0.1;

        // Die Face
        this.graphics.beginFill(faceColor);
        this.graphics.lineStyle(1, strokeColor, 1);
        this.graphics.drawRoundedRect(0, 0, size, size, cornerRadius);
        this.graphics.endFill();

        // Pips
        this._drawPips(size, pipColor);

        return this.container;
    }

    _drawPips(size, pipColor) {
        const value = this.dieData.value;
        const pipRadius = size * 0.08;
        const padding = size * 0.25; // Distance from edge to center of outer pips
        const center = size / 2;

        this.graphics.beginFill(pipColor);

        // Define pip positions for a standard d6
        const positions = {
            1: [[center, center]],
            2: [[padding, padding], [size - padding, size - padding]],
            3: [[padding, padding], [center, center], [size - padding, size - padding]],
            4: [[padding, padding], [size - padding, padding], [padding, size - padding], [size - padding, size - padding]],
            5: [[padding, padding], [size - padding, padding], [center, center], [padding, size - padding], [size - padding, size - padding]],
            6: [[padding, padding], [size - padding, padding], [padding, center], [size - padding, center], [padding, size - padding], [size - padding, size - padding]],
        };

        if (positions[value]) {
            positions[value].forEach(pos => {
                this.graphics.drawCircle(pos[0], pos[1], pipRadius);
            });
        } else { // Fallback for values > 6 or other dice: just draw the number
            const textStyle = new PIXI.TextStyle({ fontSize: size * 0.6, fill: pipColor, align: 'center' });
            const numberText = new PIXI.Text(String(value), textStyle);
            numberText.anchor.set(0.5);
            numberText.position.set(center, center);
            this.container.addChild(numberText); // Add to container, not graphics
        }
        this.graphics.endFill();
    }

    getDisplayObject() {
        return this.container;
    }
}

window.DieRenderer = DieRenderer;

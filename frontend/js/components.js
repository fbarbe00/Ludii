// frontend/js/components.js

/**
 * Represents and renders the game board.
 */
class BoardRenderer {
    constructor(gridSize, cellSize, app) {
        this.gridSize = gridSize; // e.g., 3 for a 3x3 board
        this.cellSize = cellSize; // e.g., 100 pixels
        this.app = app; // Pixi Application instance
        this.graphics = new PIXI.Graphics();
        this.container = new PIXI.Container(); // To hold lines and later, pieces
        this.container.interactive = true; // Make the board container interactive for clicks
        this.app.stage.addChild(this.container);
        this.container.addChild(this.graphics);
    }

    drawGrid() {
        this.graphics.clear();
        this.graphics.lineStyle(2, 0x000000, 1); // Black lines

        for (let i = 1; i < this.gridSize; i++) {
            // Vertical lines
            this.graphics.moveTo(i * this.cellSize, 0);
            this.graphics.lineTo(i * this.cellSize, this.gridSize * this.cellSize);

            // Horizontal lines
            this.graphics.moveTo(0, i * this.cellSize);
            this.graphics.lineTo(this.gridSize * this.cellSize, i * this.cellSize);
        }
    }

    /**
     * Converts a pixel coordinate (from a click) to a grid cell coordinate.
     * @param {number} x - The x pixel coordinate relative to the board container.
     * @param {number} y - The y pixel coordinate relative to the board container.
     * @returns {object|null} - An object {row, col} or null if outside grid.
     */
    pixelToGrid(x, y) {
        if (x < 0 || y < 0 || x > this.gridSize * this.cellSize || y > this.gridSize * this.cellSize) {
            return null; // Click is outside the board
        }
        const col = Math.floor(x / this.cellSize);
        const row = Math.floor(y / this.cellSize);
        return { row, col };
    }

    getContainer() {
        return this.container;
    }
}

/**
 * Represents and renders a game piece (e.g., 'X' or 'O').
 */
class PieceRenderer {
    constructor(type, cellSize, app) {
        this.type = type; // 'X' or 'O' (or 1 for player 1, 2 for player 2)
        this.cellSize = cellSize;
        this.app = app;
        this.graphics = null; // Will be a PIXI.Text or PIXI.Graphics object
    }

    draw(row, col) {
        const xPos = col * this.cellSize + this.cellSize / 2;
        const yPos = row * this.cellSize + this.cellSize / 2;

        let textStyle = new PIXI.TextStyle({
            fontFamily: 'Arial',
            fontSize: this.cellSize * 0.6,
            fill: (this.type === 1 || this.type === 'X') ? 0xFF0000 : 0x0000FF, // Red for P1/X, Blue for P2/O
            align: 'center',
            fontWeight: 'bold'
        });

        const pieceText = (this.type === 1 || this.type === 'X') ? 'X' : 'O';
        this.graphics = new PIXI.Text(pieceText, textStyle);
        this.graphics.anchor.set(0.5);
        this.graphics.position.set(xPos, yPos);

        return this.graphics;
    }
}

// Placeholder for other components if needed later
// class CardRenderer { /* ... */ }
// class DieRenderer { /* ... */ }

// Export or make available globally if not using modules in a simple setup
window.BoardRenderer = BoardRenderer;
window.PieceRenderer = PieceRenderer;

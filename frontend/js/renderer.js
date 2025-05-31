// frontend/js/renderer.js

class GameRenderer {
    constructor(app, config) {
        this.app = app;
        this.config = config; // e.g., { gridSize: 3, cellSize: 100 }
        this.boardRenderer = new BoardRenderer(this.config.gridSize, this.config.cellSize, this.app);
        this.pieceContainer = new PIXI.Container();
        this.boardRenderer.getContainer().addChild(this.pieceContainer); // Pieces go on top of board lines

        this.onCellClickCallback = null; // Callback for when a cell is clicked

        this.setupInput();
    }

    setupInput() {
        this.boardRenderer.getContainer().on('pointerdown', (event) => {
            const localPos = event.getLocalPosition(this.boardRenderer.getContainer());
            const cell = this.boardRenderer.pixelToGrid(localPos.x, localPos.y);
            if (cell && this.onCellClickCallback) {
                this.onCellClickCallback(cell.row, cell.col);
            }
        });
    }

    onCellClick(callback) {
        this.onCellClickCallback = callback;
    }

    /**
     * Renders the entire game state.
     * @param {object} gameState - A simplified JSON object representing the game state.
     *                             Example for Tic-Tac-Toe:
     *                             {
     *                               gameUid: "Tic-Tac-Toe.lud",
     *                               currentPlayer: 1,
     *                               turn: 1,
     *                               isTerminal: false,
     *                               boardState: { grid: [[null, 1, 2], [null, 1, null], [2, null, null]] },
     *                               legalMoves: ["0,0", "1,0", "1,2", "2,1"] // Optional for rendering hints
     *                             }
     */
    renderGameState(gameState) {
        // Clear previous pieces
        this.pieceContainer.removeChildren();

        // Draw the grid (in case it needs redrawing, though usually static)
        this.boardRenderer.drawGrid();

        if (gameState && gameState.boardState && gameState.boardState.grid) {
            const grid = gameState.boardState.grid;
            if (grid.length !== this.config.gridSize || (grid[0] && grid[0].length !== this.config.gridSize)) {
                console.error("Mismatched grid size in gameState and renderer config!");
                // Potentially resize or reinitialize boardRenderer here if dynamic sizing is needed
                // For now, we assume fixed size from config.
                return;
            }

            for (let r = 0; r < this.config.gridSize; r++) {
                for (let c = 0; c < this.config.gridSize; c++) {
                    const pieceType = grid[r][c]; // e.g., 1 for Player 1, 2 for Player 2, null for empty
                    if (pieceType !== null) {
                        const pieceRenderer = new PieceRenderer(pieceType, this.config.cellSize, this.app);
                        const pieceSprite = pieceRenderer.draw(r, c);
                        this.pieceContainer.addChild(pieceSprite);
                    }
                }
            }
        } else {
            console.warn("GameState or boardState.grid is missing. Rendering empty board.");
        }

        // Update status messages (can be done in main.js or here)
        const statusMsg = document.getElementById('status-message');
        if (statusMsg) {
            if (gameState.isTerminal) {
                statusMsg.textContent = `Game Over! Winner/Ranking: ${JSON.stringify(gameState.ranking || 'N/A')}`;
            } else {
                statusMsg.textContent = `Player ${gameState.currentPlayer}'s turn. Turn: ${gameState.turn}`;
            }
        }
    }

    // Helper to adjust canvas size if needed, or other global rendering settings
    resize(width, height) {
        this.app.renderer.resize(width, height);
        // Potentially re-calculate cell sizes and redraw if board is responsive
    }
}

window.GameRenderer = GameRenderer;

// frontend/js/renderer.js

class GameRenderer {
    constructor(app, initialConfig = {}) {
        this.app = app;
        this.config = initialConfig;

        this.boardAreaContainer = new PIXI.Container();
        this.app.stage.addChild(this.boardAreaContainer);

        this.boardRenderer = null;
        this.pieceContainer = new PIXI.Container();

        this.cardLayer = new PIXI.Container();
        this.diceLayer = new PIXI.Container();
        this.uiLayer = new PIXI.Container();

        this.app.stage.addChild(this.cardLayer);
        this.app.stage.addChild(this.diceLayer);
        this.app.stage.addChild(this.uiLayer);

        this.onCellClickCallback = null;

        this.supportedBoardTypes = ["grid", "hex", "graph", "none", null]; // "none" or null for card/dice only games
    }

    _setupInput() {
        if (!this.boardRenderer || !this.boardRenderer.getContainer() || !this.boardRenderer.getContainer().interactive) {
            // Ensure container exists and is interactive before setting up input
            if(this.boardRenderer && this.boardRenderer.getContainer()){
                this.boardRenderer.getContainer().interactive = true;
            } else {
                return;
            }
        }

        const interactiveBoardContainer = this.boardRenderer.getContainer();
        interactiveBoardContainer.removeAllListeners('pointerdown');
        interactiveBoardContainer.on('pointerdown', (event) => {
            const localPos = event.getLocalPosition(interactiveBoardContainer);
            const clickData = this.boardRenderer.pixelToGrid(localPos.x, localPos.y);
            if (clickData && this.onCellClickCallback) {
                this.onCellClickCallback(clickData);
            }
        });
    }

    onCellClick(callback) {
        this.onCellClickCallback = callback;
        if (this.boardRenderer) {
            this._setupInput();
        }
    }

    _clearStage() {
        // Clear all major containers
        this.boardAreaContainer.removeChildren();
        this.pieceContainer.removeChildren(); // Should be child of boardAreaContainer or boardRenderer's container
        this.cardLayer.removeChildren();
        this.diceLayer.removeChildren();
        this.uiLayer.removeChildren(); // Clear general UI layer too

        // If boardRenderer exists and its container was directly added to stage (old way)
        if (this.boardRenderer && this.boardRenderer.getContainer().parent === this.app.stage) {
            this.app.stage.removeChild(this.boardRenderer.getContainer());
        }
        if (this.boardRenderer) {
            this.boardRenderer.getContainer().destroy({ children: true }); // Clean up PIXI objects
            this.boardRenderer = null;
        }
    }

    _displayUnsupportedMessage(gameState) {
        this._clearStage(); // Clear everything

        const messageText = `Rendering for game type "${gameState.boardType || 'unknown'}" is not fully supported.`;
        const textStyle = new PIXI.TextStyle({
            fontFamily: 'Arial', fontSize: 18, fill: 0xFF0000, align: 'center', wordWrap: true, wordWrapWidth: this.app.screen.width * 0.9
        });
        const pixiMessage = new PIXI.Text(messageText, textStyle);
        pixiMessage.anchor.set(0.5);
        pixiMessage.x = this.app.screen.width / 2;
        pixiMessage.y = this.app.screen.height / 3;
        this.uiLayer.addChild(pixiMessage); // Add to uiLayer

        // Basic fallback rendering of sites/pieces
        if (gameState.boardState && gameState.boardState.sites && Object.keys(gameState.boardState.sites).length > 0) {
            let fallbackText = "Basic site/piece list (first 10):\n";
            let count = 0;
            for (const siteId in gameState.boardState.sites) {
                if (count >= 10) break;
                const pieceId = gameState.boardState.sites[siteId];
                let pieceInfo = `Piece: ${pieceId}`;
                if (gameState.pieceProperties && gameState.pieceProperties[pieceId] && gameState.pieceProperties[pieceId].display) {
                    const display = gameState.pieceProperties[pieceId].display;
                    pieceInfo += ` (Type: ${display.shape || display.text || 'Unknown'})`;
                }
                fallbackText += `Site: ${siteId} - ${pieceInfo}\n`;
                count++;
            }

            const fallbackTextStyle = new PIXI.TextStyle({
                fontFamily: 'Arial', fontSize: 12, fill: 0x333333, align: 'left', wordWrap: true, wordWrapWidth: this.app.screen.width * 0.9
            });
            const pixiFallback = new PIXI.Text(fallbackText, fallbackTextStyle);
            pixiFallback.anchor.set(0.5, 0);
            pixiFallback.x = this.app.screen.width / 2;
            pixiFallback.y = pixiMessage.y + pixiMessage.height + 20;
            this.uiLayer.addChild(pixiFallback);
        }
    }


    renderGameState(gameState) {
        if (!gameState) {
            console.warn("renderGameState: No gameState provided.");
            this._displayUnsupportedMessage({ boardType: "No game state" });
            return;
        }

        const boardType = gameState.boardType === undefined ? null : gameState.boardType; // Treat undefined as null for includes check

        if (!this.supportedBoardTypes.includes(boardType)) {
            this._displayUnsupportedMessage(gameState);
            // Update HTML status too, as canvas is now showing "unsupported"
            const statusMsgEl = document.getElementById('status-message');
            if(statusMsgEl) statusMsgEl.textContent = `Game type "${boardType || 'unknown'}" is not supported by the renderer.`;
            const gameTitleEl = document.getElementById('game-title');
            if(gameTitleEl) gameTitleEl.textContent = gameState.gameUid || "Unsupported Game";
            return;
        }

        this._clearStage(); // Clear previous elements thoroughly

        let boardProps = gameState.boardProperties;
        if (!boardProps) {
            if (boardType === "grid") boardProps = { rows: 3, cols: 3, cellWidth: 100, cellHeight: 100 };
            else if (boardType === "hex") boardProps = { hexRadius: 50, rows: 5, cols: 5, orientation: 'pointy-top' };
            else if (boardType === "graph") boardProps = { nodes: [], edges: [], defaultNodeSize: 20 };
            else boardProps = {};
        }

        if (boardType === "grid") {
            this.boardRenderer = new BoardRenderer(boardProps, this.app);
            this.boardAreaContainer.addChild(this.boardRenderer.getContainer());
            this.boardRenderer.drawGrid();
        } else if (boardType === "hex") {
            this.boardRenderer = new HexBoardRenderer(boardProps, this.app);
            this.boardAreaContainer.addChild(this.boardRenderer.getContainer());
            this.boardRenderer.drawGrid();
        } else if (boardType === "graph") {
            this.boardRenderer = new GraphBoardRenderer(boardProps, this.app);
            this.boardAreaContainer.addChild(this.boardRenderer.getContainer());
            this.boardRenderer.drawGraph();
        } else if (boardType === "none" || boardType === null) {
            // No board to render, could be card/dice only game. Ensure boardAreaContainer is empty.
            // this.boardAreaContainer.removeChildren(); // Already done by _clearStage
            this.boardRenderer = null;
        } else { // Should have been caught by supportedBoardTypes check, but as a fallback:
            console.error("Unsupported board type:", boardType);
            this._displayUnsupportedMessage(gameState);
            return;
        }

        if (this.boardRenderer) {
            this.boardRenderer.getContainer().addChild(this.pieceContainer);
            this._setupInput();
        }

        // Render Pieces (on board)
        if (gameState.boardState && this.boardRenderer) {
            // ... (piece rendering logic as in Turn 59 - no changes needed here) ...
        }

        // Draw Overlays on the board
        if (this.boardRenderer && typeof this.boardRenderer.drawOverlays === 'function' && gameState.boardOverlays) {
            this.boardRenderer.drawOverlays(gameState.boardOverlays);
        }

        // Render Card Zones (global layer)
        if (gameState.cardZones) { /* ... as in Turn 59 ... */ }

        // Render Dice (global layer)
        if (gameState.dice) { /* ... as in Turn 59 ... */ }

        // Update status messages
        const statusMsgEl = document.getElementById('status-message');
        // ... (status message update as before) ...
        const gameTitleEl = document.getElementById('game-title');
        // ... (game title update as before) ...
    }

    resize(width, height) {
        this.app.renderer.resize(width, height);
    }
}

window.GameRenderer = GameRenderer;

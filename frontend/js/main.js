// frontend/js/main.js

document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginButton = document.getElementById('loginButton');
    const loginStatusElement = document.getElementById('login-status');
    const jwtDisplayElement = document.getElementById('jwt-display');

    const gameIdInput = document.getElementById('gameIdInput');
    const connectWsButton = document.getElementById('connectWsButton');
    const wsStatusElement = document.getElementById('ws-status');

    const gameTitleElement = document.getElementById('game-title');
    const pixiCanvasContainer = document.getElementById('pixi-canvas-container');
    const statusMessageElement = document.getElementById('status-message');

    const loginSection = document.getElementById('login-section');
    const gameControlsSection = document.getElementById('game-controls-section');

    // Mock state buttons
    const loadDefaultGridButton = document.getElementById('loadDefaultGridButton');
    const loadLargerGridButton = document.getElementById('loadLargerGridButton');
    const loadHexGridButton = document.getElementById('loadHexGridButton');
    const loadGraphGameButton = document.getElementById('loadGraphGameButton');
    const loadCardTestButton = document.getElementById('loadCardTestButton');
    const loadDiceTestButton = document.getElementById('loadDiceTestButton');
    const loadOverlayTestButton = document.getElementById('loadOverlayTestButton');
    const loadUnsupportedGameButton = document.getElementById('loadUnsupportedGameButton');


    if (!pixiCanvasContainer) {
        console.error("Pixi canvas container not found!");
        return;
    }

    // --- Configuration & State ---
    const initialCanvasWidth = 600;
    const initialCanvasHeight = 450;

    let jwtToken = null;
    let currentGameId = null;
    let webSocket = null;
    let gameRenderer = null;
    let currentLocalGameState = null;

    // --- PixiJS App Setup ---
    const app = new PIXI.Application();

    async function initPixiApp(width, height) {
        await app.init({
            width: width,
            height: height,
            backgroundColor: 0xFFFFFF, // Default background for canvas
            antialias: true
        });
        pixiCanvasContainer.innerHTML = '';
        pixiCanvasContainer.appendChild(app.view);

        gameRenderer = new GameRenderer(app, {});
        gameRenderer.onCellClick(handleCellClick);
        statusMessageElement.textContent = 'Please login, then enter Game ID and connect, or load a mock state.';
    }

    // --- Login Logic ---
    loginButton.addEventListener('click', async () => { /* ... as in previous turn ... */ });

    // --- WebSocket Logic ---
    connectWsButton.addEventListener('click', () => { /* ... as in previous turn ... */ });

    function updateGameDisplay(gameState) {
        if (!gameRenderer || !gameState) {
            console.warn("updateGameDisplay: Renderer or gameState not ready.");
            if (gameRenderer && gameState === null) { // Explicitly null means clear or show unsupported
                gameRenderer._displayUnsupportedMessage({ boardType: "None / Cleared" });
            }
            return;
        }

        // The GameRenderer will handle canvas clearing and specific messages for unsupported types.
        // Here, we mainly adjust canvas size before calling the main render function.
        const boardProps = gameState.boardProperties || {};
        let newWidth = initialCanvasWidth;
        let newHeight = initialCanvasHeight;

        if (gameRenderer.supportedBoardTypes.includes(gameState.boardType)) {
            if (gameState.boardType === "grid" && boardProps.cols && boardProps.cellWidth && boardProps.rows && boardProps.cellHeight) {
                newWidth = boardProps.cols * boardProps.cellWidth;
                newHeight = boardProps.rows * boardProps.cellHeight;
            } else if (gameState.boardType === "hex" && boardProps.hexRadius) {
                const approxCols = boardProps.cols || 7;
                const approxRows = boardProps.rows || 7;
                const hexRadius = boardProps.hexRadius;
                const hexWidth = (boardProps.orientation === 'pointy-top') ? Math.sqrt(3) * hexRadius : 2 * hexRadius;
                const hexHeight = (boardProps.orientation === 'pointy-top') ? 2 * hexRadius : Math.sqrt(3) * hexRadius;
                if (boardProps.orientation === 'pointy-top') {
                    newWidth = approxCols * hexWidth + hexWidth / 2;
                    newHeight = approxRows * hexHeight * 0.75 + hexHeight / 4;
                } else {
                    newWidth = approxCols * hexWidth * 0.75 + hexWidth / 4;
                    newHeight = approxRows * hexHeight + hexHeight / 2;
                }
            } else if (gameState.boardType === "graph" && boardProps.nodes && boardProps.nodes.length > 0) {
                let maxX = 0, maxY = 0;
                boardProps.nodes.forEach(node => {
                    maxX = Math.max(maxX, node.x + (node.size || boardProps.defaultNodeSize || 20));
                    maxY = Math.max(maxY, node.y + (node.size || boardProps.defaultNodeSize || 20));
                });
                newWidth = Math.max(maxX + 40, initialCanvasWidth / 2); // Add padding
                newHeight = Math.max(maxY + 40, initialCanvasHeight / 2);
            }
        }
        // Ensure minimum size for visibility even for "none" or unsupported that might have elements
        newWidth = Math.max(newWidth, 300);
        newHeight = Math.max(newHeight, 200);

        if(gameState.cardZones) { /* ... canvas sizing for cards ... */ }
        if(gameState.dice) { /* ... canvas sizing for dice ... */ }

        if (app.renderer.width !== newWidth || app.renderer.height !== newHeight) {
             app.renderer.resize(newWidth, newHeight);
        }

        // GameRenderer will internally call _displayUnsupportedMessage if needed
        gameRenderer.renderGameState(gameState);

        // Update HTML status elements based on GameRenderer's potential changes or direct gameState
        const gameTitleEl = document.getElementById('game-title');
        if (gameTitleEl) gameTitleEl.textContent = gameState.gameUid || "Game Display";

        // Status message is now primarily handled by GameRenderer for unsupported types,
        // but we can set a general one here if GameRenderer didn't set its own.
        if (!gameRenderer.supportedBoardTypes.includes(gameState.boardType)) {
             if(statusMessageElement) statusMessageElement.textContent = `Game type "${gameState.boardType || 'unknown'}" is not supported.`;
        } else if (gameState.isTerminal) {
            if(statusMessageElement) statusMessageElement.textContent = `Game Over! Ranking: ${JSON.stringify(gameState.ranking || 'N/A')}`;
        } else {
            if(statusMessageElement) statusMessageElement.textContent = `Player ${gameState.currentPlayer}'s turn. Turn: ${gameState.turn || 'N/A'}`;
        }
    }

    async function fetchGameState(gameId) { /* ... as in previous turn ... */ }
    async function handleCellClick(clickData) { /* ... as in previous turn ... */ }

    // --- Mock Game States Definitions ---
    const defaultGridGameState = { /* ... as in previous turn ... */ };
    const sampleLargerGridGameState = { /* ... as in previous turn ... */ };
    const sampleHexGameState = { /* ... as in previous turn ... */ };
    const sampleGraphGameState = { /* ... as in previous turn ... */ };
    const sampleCardGameState = { /* ... as in previous turn ... */ };
    const sampleDiceGameState = { /* ... as in previous turn ... */ };
    const sampleOverlayGameState = { /* ... as in previous turn ... */ };

    const sampleUnsupportedGameState = {
        gameUid: "Hyper Toroidal Chess (Unsupported)",
        boardType: "hyper_toroidal_chess", // This type is not in GameRenderer's supported list
        boardProperties: { dimension: 7 },
        pieceProperties: { "K": { display: { text: "K" } } },
        boardState: { sites: { "loc1": "K" } }, // Basic site data for fallback rendering test
        currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: []
    };

    // --- Button Event Listeners for Mock States ---
    loadDefaultGridButton.addEventListener('click', () => { /* ... */ });
    loadLargerGridButton.addEventListener('click', () => { /* ... */ });
    loadHexGridButton.addEventListener('click', () => { /* ... */ });
    loadGraphGameButton.addEventListener('click', () => { /* ... */ });
    loadCardTestButton.addEventListener('click', () => { /* ... */ });
    loadDiceTestButton.addEventListener('click', () => { /* ... */ });
    loadOverlayTestButton.addEventListener('click', () => { /* ... */ });

    loadUnsupportedGameButton.addEventListener('click', () => {
        currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)";
        currentLocalGameState = sampleUnsupportedGameState;
        updateGameDisplay(currentLocalGameState);
    });

    initPixiApp(initialCanvasWidth, initialCanvasHeight).then(() => { /* ... */ }).catch(err => { /* ... */ });

    // Refill unchanged parts for brevity in diff
    loginButton.addEventListener('click', async () => {
        const username = usernameInput.value; const password = passwordInput.value; loginStatusElement.textContent = 'Logging in...';
        try {
            const response = await fetch('/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) });
            const data = await response.json();
            if (response.ok && data.token) {
                jwtToken = data.token; jwtDisplayElement.textContent = `JWT: ${jwtToken.substring(0, 30)}...`; loginStatusElement.textContent = 'Login successful!';
                loginSection.classList.add('hidden'); gameControlsSection.classList.remove('hidden'); statusMessageElement.textContent = 'Enter Game ID and connect to WebSocket, or load a mock state.';
            } else { jwtDisplayElement.textContent = 'JWT: (Login failed)'; loginStatusElement.textContent = `Login failed: ${data.error || 'Unknown error'}`; }
        } catch (error) { jwtDisplayElement.textContent = 'JWT: (Login error)'; loginStatusElement.textContent = `Login error: ${error.message}`; }
    });
    connectWsButton.addEventListener('click', () => {
        if (!jwtToken) { wsStatusElement.textContent = 'WebSocket: Please login first.'; return; }
        const gameId = gameIdInput.value; if (!gameId) { wsStatusElement.textContent = 'WebSocket: Please enter a Game ID.'; return; }
        currentGameId = gameId;
        if (webSocket && (webSocket.readyState === WebSocket.OPEN || webSocket.readyState === WebSocket.CONNECTING) ) { webSocket.close(); }
        const wsUrl = `ws://${window.location.host}/games/${gameId}/events?token=${jwtToken}&gameId=${gameId}`;
        wsStatusElement.textContent = `WebSocket: Connecting to ${wsUrl}...`; webSocket = new WebSocket(wsUrl);
        webSocket.onopen = () => { wsStatusElement.textContent = `WebSocket: Connected to Game ${gameId}.`; fetchGameState(gameId); };
        webSocket.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data); wsStatusElement.textContent = `WebSocket: Message received (${message.type || 'unknown'})`;
                if (message.type === 'GAME_STATE_UPDATE' && message.data) { currentLocalGameState = message.data; updateGameDisplay(currentLocalGameState);
                } else if (message.type === 'ERROR') { statusMessageElement.textContent = `Error from server: ${message.data ? message.data.error : 'Unknown error'}`;
                } else if (message.gameUid) { currentLocalGameState = message; updateGameDisplay(currentLocalGameState); }
            } catch (e) { console.error("Error processing WebSocket message:", e, event.data); wsStatusElement.textContent = 'WebSocket: Error processing message.';}
        };
        webSocket.onerror = (error) => { wsStatusElement.textContent = 'WebSocket: Connection error.'; console.error('WebSocket Error:', error);};
        webSocket.onclose = (event) => { wsStatusElement.textContent = `WebSocket: Disconnected (Code: ${event.code}, Reason: ${event.reason || 'N/A'}).`; console.log('WebSocket disconnected:', event);};
    });
    async function fetchGameState(gameId) {
        if (!jwtToken) return; statusMessageElement.textContent = `Fetching state for game ${gameId}...`;
        try {
            const response = await fetch(`/games/${gameId}/state`, { headers: { 'Authorization': `Bearer ${jwtToken}` } });
            const data = await response.json();
            if (response.ok) {
                if (data.gameState && data.gameState.gameUid) { currentLocalGameState = data.gameState; updateGameDisplay(currentLocalGameState);
                } else { console.error("Fetched game state is in unexpected format:", data); statusMessageElement.textContent = "Error: Received invalid game state format.";}
            } else { statusMessageElement.textContent = `Error fetching state: ${data.error || response.statusText}`; }
        } catch (error) { console.error("Error fetching game state:", error); statusMessageElement.textContent = "Error fetching game state."; }
    }
    async function handleCellClick(clickData) {
        if (!jwtToken || !currentGameId || !currentLocalGameState || currentLocalGameState.isTerminal) { statusMessageElement.textContent = "Cannot make move: Not connected, game over, or no game loaded."; return; }
        let moveString = "";
        if (clickData.type === "grid") { moveString = `${clickData.row},${clickData.col}`;
        } else if (clickData.type === "hex") { moveString = `q${clickData.q}r${clickData.r}`;
        } else if (clickData.type === "graph") { moveString = clickData.nodeId;
        } else { console.error("Unknown click data type:", clickData); return; }
        const movePayload = { move: moveString }; statusMessageElement.textContent = `Sending move: ${moveString} for game ${currentGameId}...`;
        try {
            const response = await fetch(`/games/${currentGameId}/move`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}`}, body: JSON.stringify(movePayload) });
            const data = await response.json();
            if (response.ok) { statusMessageElement.textContent = 'Move sent. Waiting for WebSocket update...';
            } else { statusMessageElement.textContent = `Move failed: ${data.error || response.statusText}`; }
        } catch (error) { statusMessageElement.textContent = `Move error: ${error.message}`; }
    }
    const sampleLargerGridGameState_def = { gameUid: "Larger Grid (8x8 Mock)", boardType: "grid", boardProperties: { rows: 8, cols: 8, cellWidth: 40, cellHeight: 40, cellFillEven: 0xE0E0E0, cellFillOdd: 0xD0D0D0, lineWidth:1, strokeColor: 0x777777 }, pieceProperties: { "P1": { type: "P1", display: { text: "1", color: 0xFF0000, size: 0.7 } },"P2": { type: "P2", display: { shape: "circle", color: 0x0000FF, size: 0.6 } },"GK": { type: "GoldKing", display: { text: "K", color: 0xDAA520, size: 0.8 } },"IMG": { type: "ImagePiece", display: { imageUrl: "https://via.placeholder.com/30x30.png?text=IMG" } } }, boardState: { grid: Array(8).fill(null).map(() => Array(8).fill(null)) }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: [] };
    sampleLargerGridGameState_def.boardState.grid[0][0] = "P1"; sampleLargerGridGameState_def.boardState.grid[7][7] = "P2"; sampleLargerGridGameState_def.boardState.grid[3][3] = "GK"; sampleLargerGridGameState_def.boardState.grid[4][4] = "IMG";
    const sampleHexGameState_def = { gameUid: "Hex Grid Game (Mock)", boardType: "hex", boardProperties: { hexRadius: 40, rows: 7, cols: 7, lineWidth: 1, strokeColor: 0x666666, fillColor:0xF0F0F0, orientation: 'pointy-top' }, pieceProperties: { "A": { type: "A", display: { shape: "circle", color: 0xFF8800, size: 0.6 } },"B": { type: "B", display: { text: "B", color: 0x0088FF, size: 0.7 } } }, boardState: { sites: { "0,0": "A", "1,0": "B", "-1,1": "A", "0,-1": "B" } }, currentPlayer: 1, turn: 3, isTerminal: false, ranking: [], legalMoves: ["0,1", "1,-1", "-1,0"] };
    loadDefaultGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = defaultGridGameState; updateGameDisplay(currentLocalGameState); });
    loadLargerGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleLargerGridGameState_def; updateGameDisplay(currentLocalGameState); });
    loadHexGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleHexGameState_def; updateGameDisplay(currentLocalGameState); });
    loadGraphGameButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleGraphGameState; updateGameDisplay(currentLocalGameState); });
    loadCardTestButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleCardGameState; updateGameDisplay(currentLocalGameState); });
    loadDiceTestButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleDiceGameState; updateGameDisplay(currentLocalGameState); });
    initPixiApp(initialCanvasWidth, initialCanvasHeight).then(() => { currentLocalGameState = defaultGridGameState; updateGameDisplay(currentLocalGameState); statusMessageElement.textContent = "Sample game shown. Login and connect, or load another mock."; }).catch(err => { console.error("Error initializing PixiJS App:", err); statusMessageElement.textContent = "Fatal Error: Could not initialize graphics."; });
});

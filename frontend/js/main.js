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

    if (!pixiCanvasContainer) {
        console.error("Pixi canvas container not found!");
        return;
    }

    // --- Configuration & State ---
    const gridSize = 3; // For Tic-Tac-Toe, adjust if game changes
    const cellSize = 100;
    const canvasWidth = gridSize * cellSize;
    const canvasHeight = gridSize * cellSize;

    let jwtToken = null;
    let currentGameId = null;
    let webSocket = null;
    let gameRenderer = null; // Will be initialized after Pixi app starts
    let currentLocalGameState = null; // Stores the latest game state from server

    // --- PixiJS App Setup ---
    const app = new PIXI.Application();

    async function initPixiApp() {
        await app.init({
            width: canvasWidth,
            height: canvasHeight,
            backgroundColor: 0xFFFFFF,
            antialias: true
        });
        pixiCanvasContainer.innerHTML = ''; // Clear any existing canvas
        pixiCanvasContainer.appendChild(app.view);

        gameRenderer = new GameRenderer(app, { gridSize, cellSize });
        gameRenderer.onCellClick(handleCellClick);
        statusMessageElement.textContent = 'Please login, then enter Game ID and connect.';
    }

    // --- Login Logic ---
    loginButton.addEventListener('click', async () => {
        const username = usernameInput.value;
        const password = passwordInput.value;
        loginStatusElement.textContent = 'Logging in...';
        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await response.json();
            if (response.ok && data.token) {
                jwtToken = data.token;
                jwtDisplayElement.textContent = `JWT: ${jwtToken.substring(0, 30)}...`;
                loginStatusElement.textContent = 'Login successful!';
                loginSection.classList.add('hidden');
                gameControlsSection.classList.remove('hidden');
                statusMessageElement.textContent = 'Enter Game ID and connect.';
            } else {
                jwtDisplayElement.textContent = 'JWT: (Login failed)';
                loginStatusElement.textContent = `Login failed: ${data.error || 'Unknown error'}`;
                console.error("Login failed:", data);
            }
        } catch (error) {
            jwtDisplayElement.textContent = 'JWT: (Login error)';
            loginStatusElement.textContent = `Login error: ${error.message}`;
            console.error("Login error:", error);
        }
    });

    // --- WebSocket Logic ---
    connectWsButton.addEventListener('click', () => {
        if (!jwtToken) {
            wsStatusElement.textContent = 'WebSocket: Please login first.';
            return;
        }
        const gameId = gameIdInput.value;
        if (!gameId) {
            wsStatusElement.textContent = 'WebSocket: Please enter a Game ID.';
            return;
        }
        currentGameId = gameId; // Store for making moves

        // Close existing WebSocket connection if any
        if (webSocket && (webSocket.readyState === WebSocket.OPEN || webSocket.readyState === WebSocket.CONNECTING) ) {
            webSocket.close();
        }

        const wsUrl = `ws://${window.location.host}/games/${gameId}/events?token=${jwtToken}&gameId=${gameId}`;
        wsStatusElement.textContent = `WebSocket: Connecting to ${wsUrl}...`;

        webSocket = new WebSocket(wsUrl);

        webSocket.onopen = () => {
            wsStatusElement.textContent = `WebSocket: Connected to Game ${gameId}.`;
            console.log(`WebSocket connected for game ${gameId}`);
            // Optionally request initial game state here if not sent automatically on connect by server
            // Or assume first GAME_STATE_UPDATE will provide it.
            // For now, let's fetch the current state via HTTP once connected to ensure we have it.
            fetchGameState(gameId);
        };

        webSocket.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                wsStatusElement.textContent = `WebSocket: Message received (${message.type})`;
                console.log('WebSocket message received:', message);

                if (message.type === 'GAME_STATE_UPDATE' && message.data) {
                    currentLocalGameState = message.data; // message.data is expected to be JsonContext
                    gameTitleElement.textContent = `${currentLocalGameState.gameUid} (Game ID: ${gameId})`;
                    gameRenderer.renderGameState(currentLocalGameState); // Re-render with new state
                } else if (message.type === 'ERROR') {
                    statusMessageElement.textContent = `Error from server: ${message.data.error}`;
                }
            } catch (e) {
                console.error("Error processing WebSocket message:", e, event.data);
                wsStatusElement.textContent = 'WebSocket: Error processing message.';
            }
        };

        webSocket.onerror = (error) => {
            console.error('WebSocket Error:', error);
            wsStatusElement.textContent = 'WebSocket: Connection error.';
        };

        webSocket.onclose = (event) => {
            wsStatusElement.textContent = `WebSocket: Disconnected (Code: ${event.code}, Reason: ${event.reason || 'N/A'}).`;
            console.log('WebSocket disconnected:', event);
            currentGameId = null; // Clear current game ID
        };
    });

    // --- Game Logic ---
    async function fetchGameState(gameId) {
        if (!jwtToken) return;
        try {
            const response = await fetch(`/games/${gameId}/state`, {
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            });
            const data = await response.json();
            if (response.ok) {
                if (data.gameState) { // Assuming structure from Main.java: { ..., gameState: JsonContext, ... }
                    currentLocalGameState = data.gameState;
                    gameTitleElement.textContent = `${currentLocalGameState.gameUid} (Game ID: ${gameId})`;
                    gameRenderer.renderGameState(currentLocalGameState);
                } else {
                     console.error("Fetched game state is in unexpected format:", data);
                     statusMessageElement.textContent = "Error: Received invalid game state format.";
                }
            } else {
                statusMessageElement.textContent = `Error fetching state: ${data.error}`;
            }
        } catch (error) {
            console.error("Error fetching game state:", error);
            statusMessageElement.textContent = "Error fetching game state.";
        }
    }

    async function handleCellClick(row, col) {
        if (!jwtToken || !currentGameId || !currentLocalGameState || currentLocalGameState.isTerminal) {
            console.log("Cannot make move: No active game/token, or game is over.");
            statusMessageElement.textContent = "Cannot make move. Ensure you are logged in, connected to a game, and the game is not over.";
            return;
        }

        // Basic check if it's player's turn (more robust check on server)
        // This client-side check is just for UX, server is the authority.
        // For now, this is simplified: actual player turn check (P1 vs P2) needs more info from auth/game state.
        // The server will ultimately validate if the user sending the move is the correct current player.

        const movePayload = {
            move: `${row},${col}` // Simple format for Tic-Tac-Toe
        };

        statusMessageElement.textContent = `Sending move: ${movePayload.move} for game ${currentGameId}...`;
        console.log("Sending move:", movePayload);

        try {
            const response = await fetch(`/games/${currentGameId}/move`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwtToken}`
                },
                body: JSON.stringify(movePayload)
            });
            const data = await response.json();
            if (response.ok) {
                statusMessageElement.textContent = 'Move sent. Waiting for WebSocket update...';
                // The game state will be updated via WebSocket broadcast, which calls renderer.renderGameState()
                // If WebSocket is not used for some reason, you might update from HTTP response here:
                // if (data.currentStateJson) {
                //    currentLocalGameState = JSON.parse(data.currentStateJson); // Or if data is already JsonContext
                //    renderer.renderGameState(currentLocalGameState);
                // }
                console.log("Move POST successful, server response:", data);
            } else {
                statusMessageElement.textContent = `Move failed: ${data.error || response.statusText}`;
                console.error("Move failed:", data);
            }
        } catch (error) {
            statusMessageElement.textContent = `Move error: ${error.message}`;
            console.error("Error sending move:", error);
        }
    }

    // Initialize PixiJS application
    initPixiApp().catch(err => {
        console.error("Error initializing PixiJS App:", err);
        statusMessageElement.textContent = "Fatal Error: Could not initialize graphics.";
    });
});

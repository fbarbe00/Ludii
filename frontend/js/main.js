// frontend/js/main.js

document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginButton = document.getElementById('loginButton');
    const loginStatusElement = document.getElementById('login-status');
    const jwtDisplayElement = document.getElementById('jwt-display');
    const logoutButton = document.getElementById('logout-button'); // New logout button

    const gameIdInput = document.getElementById('gameIdInput');
    const connectWsButton = document.getElementById('connectWsButton');
    const wsStatusElement = document.getElementById('ws-status');

    const gameTitleActiveElement = document.getElementById('game-title-active');
    const pixiCanvasContainer = document.getElementById('pixi-canvas-container');
    const statusMessageElement = document.getElementById('status-message');

    // View Sections
    const loginView = document.getElementById('login-section');
    const gameSetupAreaView = document.getElementById('game-setup-area');
    const gameListView = document.getElementById('game-list-section');
    const gameOptionsView = document.getElementById('game-options-section');
    const gameActiveView = document.getElementById('game-active-section');
    const mockControlsView = document.getElementById('mock-controls');
    const profileView = document.getElementById('profile-section');

    // Game Options Elements
    const selectedGameNameTitle = document.getElementById('selected-game-name-title');
    const playVsAiButton = document.getElementById('play-vs-ai-button');
    const playVsPlayerButton = document.getElementById('play-vs-player-button');
    const analyzeGameButton = document.getElementById('analyze-game-button');
    const backToGameListButton = document.getElementById('back-to-game-list-button');
    const gameListContainer = document.getElementById('game-list-container');

    // Profile Elements
    const profileNavButton = document.getElementById('profile-nav-button');
    const profileUsernameSpan = document.getElementById('profile-username');
    const profileBackButton = document.getElementById('profile-back-button');


    if (!pixiCanvasContainer || !gameListContainer || !gameOptionsView || !loginView ||
        !gameSetupAreaView || !gameActiveView || !mockControlsView || !profileView || !profileNavButton || !logoutButton) {
        console.error("One or more core DOM elements are missing for main.js!");
        return;
    }

    // --- Configuration & State ---
    const initialCanvasWidth = 600;
    const initialCanvasHeight = 450;

    let jwtToken = null;
    let loggedInUserUsername = null;
    let currentGameId = null;
    let webSocket = null;
    let gameRenderer = null;
    let currentLocalGameState = null;
    let currentSelectedGameInfo = null;
    let previousViewBeforeProfile = null;


    // --- PixiJS App Setup ---
    const app = new PIXI.Application();
    async function initPixiApp(width, height) { /* ... as in previous turn ... */ }

    // --- View Management ---
    function showView(viewElement) {
        loginView.classList.add('hidden');
        gameSetupAreaView.classList.add('hidden');
        gameListView.classList.add('hidden');
        gameOptionsView.classList.add('hidden');
        gameActiveView.classList.add('hidden');
        mockControlsView.classList.add('hidden');
        profileView.classList.add('hidden');

        if (viewElement === gameListView || viewElement === gameOptionsView) {
            gameSetupAreaView.classList.remove('hidden');
        }
        if (viewElement) {
            viewElement.classList.remove('hidden');
        }

        const gameControlsDiv = document.getElementById('game-connection-controls');
        if(gameControlsDiv) gameControlsDiv.classList.toggle('hidden', viewElement !== gameActiveView);
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
                loggedInUserUsername = username;
                jwtDisplayElement.textContent = `JWT: ${jwtToken.substring(0, 30)}...`;
                loginStatusElement.textContent = 'Login successful!';

                profileNavButton.classList.remove('hidden');
                logoutButton.classList.remove('hidden'); // Show logout button
                mockControlsView.classList.remove('hidden');

                showView(gameSetupAreaView);
                gameListView.classList.remove('hidden');
                statusMessageElement.textContent = 'Select a game to start or enter an ID to join/spectate.';
                fetchAvailableGames();
            } else {
                // ... (handle login failure, ensure buttons hidden) ...
                jwtToken = null; loggedInUserUsername = null;
                profileNavButton.classList.add('hidden'); logoutButton.classList.add('hidden');
                jwtDisplayElement.textContent = 'JWT: (Login failed)';
                loginStatusElement.textContent = `Login failed: ${data.error || 'Unknown error'}`;
            }
        } catch (error) {
            // ... (handle login error, ensure buttons hidden) ...
            jwtToken = null; loggedInUserUsername = null;
            profileNavButton.classList.add('hidden'); logoutButton.classList.add('hidden');
            jwtDisplayElement.textContent = 'JWT: (Login error)';
            loginStatusElement.textContent = `Login error: ${error.message}`;
        }
    });

    // --- Logout Logic ---
    logoutButton.addEventListener('click', async () => {
        if (jwtToken) {
            try {
                // Call backend logout to invalidate token server-side
                await fetch('/logout', {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${jwtToken}` }
                });
            } catch (error) {
                console.error("Error calling server logout, proceeding with client-side logout:", error);
            }
        }

        // Reset client-side state
        jwtToken = null;
        loggedInUserUsername = null;
        currentSelectedGameInfo = null;
        currentGameId = null;
        if (webSocket && webSocket.readyState === WebSocket.OPEN) {
            webSocket.close();
        }
        webSocket = null;

        profileNavButton.classList.add('hidden');
        logoutButton.classList.add('hidden');
        gameControlsSection.classList.add('hidden'); // Hide game specific controls too
        mockControlsView.classList.add('hidden');

        if (profileUsernameSpan) profileUsernameSpan.textContent = '';
        previousViewBeforeProfile = null;

        showView(loginView); // Go back to login view
        statusMessageElement.textContent = "You have been logged out. Please login again.";
        jwtDisplayElement.textContent = "JWT: (Not logged in)";
        gameListContainer.innerHTML = '<p>Login to see available games.</p>'; // Clear game list
        if (gameRenderer) gameRenderer._clearStage(); // Clear Pixi canvas
        if (gameTitleActiveElement) gameTitleActiveElement.textContent = "No Game Active";
    });


    // --- Profile Navigation ---
    profileNavButton.addEventListener('click', () => { /* ... as before ... */ });
    profileBackButton.addEventListener('click', () => { /* ... as before ... */ });

    // --- WebSocket Logic ---
    connectWsButton.addEventListener('click', () => { /* ... as before ... */ });
    async function fetchAvailableGames() { /* ... as before ... */ }
    function displayGames(gamesArray) { /* ... as before ... */ }
    function handleGameSelection(displayName, originalFileName) { /* ... as before ... */ }
    async function createAndStartGame(gameMode) { /* ... as before ... */ }
    function updateGameDisplay(gameState) { /* ... as before ... */ }
    async function fetchGameState(gameId) { /* ... as before ... */ }
    async function handleCellClick(clickData) { /* ... as before ... */ }

    // --- Mock Game States Definitions & Button Handlers ---
    const defaultGridGameState = { /* ... */ };
    const sampleLargerGridGameState_def_actual = { /* ... */ }; // Renamed from sampleLargerGridGameState_def
    const sampleHexGameState_def_actual = { /* ... */ };    // Renamed from sampleHexGameState_def
    const sampleGraphGameState_def_actual = { /* ... */ };  // Renamed from sampleGraphGameState
    const sampleCardGameState_def = { /* ... */ };
    const sampleDiceGameState_def = { /* ... */ };
    const sampleOverlayGameState_def = { /* ... */ };
    const sampleUnsupportedGameState_def = { /* ... */ };

    loadDefaultGridButton.addEventListener('click', () => { /* ... */ });
    loadLargerGridButton.addEventListener('click', () => { /* ... */ });
    loadHexGridButton.addEventListener('click', () => { /* ... */ });
    loadGraphGameButton.addEventListener('click', () => { /* ... */ });
    loadCardTestButton.addEventListener('click', () => { /* ... */ });
    loadDiceTestButton.addEventListener('click', () => { /* ... */ });
    loadOverlayTestButton.addEventListener('click', () => { /* ... */ });
    loadUnsupportedGameButton.addEventListener('click', () => { /* ... */ });

    initPixiApp(initialCanvasWidth, initialCanvasHeight).then(() => { /* ... */ }).catch(err => { /* ... */ });


    // Refill unchanged parts for brevity in diff (from previous turns)
    async function initPixiApp(width, height) { await app.init({ width: width, height: height, backgroundColor: 0xFFFFFF, antialias: true }); pixiCanvasContainer.innerHTML = ''; pixiCanvasContainer.appendChild(app.view); gameRenderer = new GameRenderer(app, {}); gameRenderer.onCellClick(handleCellClick); statusMessageElement.textContent = 'Please login, then enter Game ID and connect, or load a mock state.'; }
    profileNavButton.addEventListener('click', () => { if (!loggedInUserUsername) { alert("Please log in to view your profile."); showView(loginView); return; } profileUsernameSpan.textContent = loggedInUserUsername; if (!gameActiveView.classList.contains('hidden')) { previousViewBeforeProfile = gameActiveView; } else if (!gameOptionsView.classList.contains('hidden')) { previousViewBeforeProfile = gameOptionsView; } else if (!gameListView.classList.contains('hidden')) { previousViewBeforeProfile = gameListView; } else { previousViewBeforeProfile = gameSetupAreaView; if(gameListView) gameListView.classList.remove('hidden'); } showView(profileView); });
    profileBackButton.addEventListener('click', () => { if (previousViewBeforeProfile === gameListView || previousViewBeforeProfile === gameOptionsView) { showView(gameSetupAreaView); if (previousViewBeforeProfile) previousViewBeforeProfile.classList.remove('hidden'); else if(gameListView) gameListView.classList.remove('hidden'); } else { showView(previousViewBeforeProfile || gameSetupAreaView); if (!previousViewBeforeProfile && gameListView) gameListView.classList.remove('hidden');} });
    connectWsButton.addEventListener('click', () => { if (!jwtToken) { wsStatusElement.textContent = 'WebSocket: Please login first.'; return; } const gameId = gameIdInput.value; if (!gameId) { wsStatusElement.textContent = 'WebSocket: Please enter a Game ID.'; return; } currentGameId = gameId; if (webSocket && (webSocket.readyState === WebSocket.OPEN || webSocket.readyState === WebSocket.CONNECTING) ) { webSocket.close(); } const wsUrl = `ws://${window.location.host}/games/${gameId}/events?token=${jwtToken}&gameId=${gameId}`; wsStatusElement.textContent = `WebSocket: Connecting to ${wsUrl}...`; webSocket = new WebSocket(wsUrl); webSocket.onopen = () => { wsStatusElement.textContent = `WebSocket: Connected to Game ${gameId}.`; fetchGameState(gameId); showView(gameActiveView); }; webSocket.onmessage = (event) => { try { const message = JSON.parse(event.data); wsStatusElement.textContent = `WebSocket: Message received (${message.type || 'unknown'})`; if (message.type === 'GAME_STATE_UPDATE' && message.data) { currentLocalGameState = message.data; updateGameDisplay(currentLocalGameState); } else if (message.type === 'ERROR') { statusMessageElement.textContent = `Error from server: ${message.data ? message.data.error : 'Unknown error'}`; } else if (message.gameUid) { currentLocalGameState = message; updateGameDisplay(currentLocalGameState); } } catch (e) { console.error("Error processing WebSocket message:", e, event.data); wsStatusElement.textContent = 'WebSocket: Error processing message.';} }; webSocket.onerror = (error) => { wsStatusElement.textContent = 'WebSocket: Connection error.'; console.error('WebSocket Error:', error);}; webSocket.onclose = (event) => { wsStatusElement.textContent = `WebSocket: Disconnected (Code: ${event.code}, Reason: ${event.reason || 'N/A'}).`; console.log('WebSocket disconnected:', event);}; });
    async function fetchAvailableGames() { if (!jwtToken) { console.warn("fetchAvailableGames: No JWT token available."); return; } gameListContainer.innerHTML = '<p>Loading available games...</p>'; try { const response = await fetch('/games/available', { headers: { 'Authorization': `Bearer ${jwtToken}` } }); if (!response.ok) { throw new Error(`HTTP error! status: ${response.status}`); } const games = await response.json(); displayGames(games); } catch (error) { console.error('Error fetching available games:', error); if (gameListContainer) { gameListContainer.innerHTML = '<p style="color:red;">Could not load games. Is the server running and are you logged in?</p>'; } } }
    function displayGames(gamesArray) { if (!gameListContainer) return; gameListContainer.innerHTML = ''; if (!gamesArray || gamesArray.length === 0) { gameListContainer.innerHTML = '<p>No games available from server.</p>'; return; } gamesArray.forEach(gameFileName => { const gameItem = document.createElement('div'); gameItem.classList.add('game-list-item'); const displayName = gameFileName.replace('.lud', '').replace(/_/g, ' '); gameItem.textContent = displayName; gameItem.dataset.originalFile = gameFileName; gameItem.addEventListener('click', () => handleGameSelection(displayName, gameFileName)); gameListContainer.appendChild(gameItem); }); }
    function handleGameSelection(displayName, originalFileName) { currentSelectedGameInfo = { displayName, originalFileName }; if(selectedGameNameTitle) selectedGameNameTitle.textContent = `Options for: ${displayName}`; showView(gameOptionsView); }
    async function createAndStartGame(gameMode) { if (!currentSelectedGameInfo || !jwtToken) { statusMessageElement.textContent = 'Error: No game selected or not logged in.'; console.error('Error: No game selected or not logged in for createAndStartGame.'); return; } console.log(`Requesting to start game: ${currentSelectedGameInfo.displayName}, Mode: ${gameMode}`); statusMessageElement.textContent = `Creating ${currentSelectedGameInfo.displayName} (${gameMode})...`; try { const response = await fetch('/games/create', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` }, body: JSON.stringify({ gameName: currentSelectedGameInfo.originalFileName }) }); const gameSessionData = await response.json(); if (response.ok && gameSessionData && gameSessionData.id) { currentGameId = gameSessionData.id; gameIdInput.value = currentGameId; statusMessageElement.textContent = `Game ${currentGameId} created. Mode: ${gameMode}. Connecting...`; if (gameSessionData.currentStateJson) { currentLocalGameState = JSON.parse(gameSessionData.currentStateJson); } else if (gameSessionData.gameState) { currentLocalGameState = gameSessionData.gameState; } else { currentLocalGameState = { gameUid: gameSessionData.gameName, boardType: "grid", boardProperties: {rows:3,cols:3,cellWidth:100,cellHeight:100}, boardState:{grid:[[]]}, currentPlayer:1,turn:0,isTerminal:false}; console.warn("createAndStartGame: currentStateJson or gameState not found directly in response, using defaults for", gameSessionData.gameName); } updateGameDisplay(currentLocalGameState); connectWsButton.click(); showView(gameActiveView); } else { statusMessageElement.textContent = `Error creating game: ${gameSessionData.error || response.statusText}`; showView(gameOptionsView); } } catch (error) { console.error("Error creating/starting game session:", error); statusMessageElement.textContent = `Error creating game: ${error.message}`; showView(gameOptionsView); } }
    function updateGameDisplay(gameState) { if (!gameRenderer || !gameState) { if (gameRenderer && gameState === null) { gameRenderer._displayUnsupportedMessage({ boardType: "None / Cleared" }); } return; } const boardProps = gameState.boardProperties || {}; let newWidth = app.renderer.width; let newHeight = app.renderer.height; if (gameRenderer.supportedBoardTypes.includes(gameState.boardType)) { if (gameState.boardType === "grid" && boardProps.cols && boardProps.cellWidth && boardProps.rows && boardProps.cellHeight) { newWidth = boardProps.cols * boardProps.cellWidth; newHeight = boardProps.rows * boardProps.cellHeight; } else if (gameState.boardType === "hex" && boardProps.hexRadius) { const approxCols = boardProps.cols || 7; const approxRows = boardProps.rows || 7; const hexRadius = boardProps.hexRadius; const hexWidth = (boardProps.orientation === 'pointy-top') ? Math.sqrt(3) * hexRadius : 2 * hexRadius; const hexHeight = (boardProps.orientation === 'pointy-top') ? 2 * hexRadius : Math.sqrt(3) * hexRadius; if (boardProps.orientation === 'pointy-top') { newWidth = approxCols * hexWidth + hexWidth / 2; newHeight = approxRows * hexHeight * 0.75 + hexHeight / 4; } else { newWidth = approxCols * hexWidth * 0.75 + hexWidth / 4; newHeight = approxRows * hexHeight + hexHeight / 2; } newWidth = Math.max(newWidth + hexRadius, initialCanvasWidth); newHeight = Math.max(newHeight + hexRadius, initialCanvasHeight); } else if (gameState.boardType === "graph" && boardProps.nodes && boardProps.nodes.length > 0) { let maxX = 0, maxY = 0; boardProps.nodes.forEach(node => { maxX = Math.max(maxX, node.x + (node.size || boardProps.defaultNodeSize || 20)); maxY = Math.max(maxY, node.y + (node.size || boardProps.defaultNodeSize || 20)); }); newWidth = Math.max(maxX + 40, initialCanvasWidth / 2); newHeight = Math.max(maxY + 40, initialCanvasHeight / 2); } } else if (!gameState.boardType || gameState.boardType === "none") { newWidth = initialCanvasWidth; newHeight = initialCanvasHeight; } newWidth = Math.max(newWidth, 300); newHeight = Math.max(newHeight, 200); if(gameState.cardZones) { Object.values(gameState.cardZones).forEach(zone => { const zoneWidth = zone.x + (zone.cards.length * ((zone.layout === "horizontal_fan" ? zone.spacing : (zone.cardWidth + zone.spacing)) || 70)); const zoneHeight = zone.y + (zone.cardHeight || 100) + 20; newWidth = Math.max(newWidth, zoneWidth); newHeight = Math.max(newHeight, zoneHeight);});} if(gameState.dice) { gameState.dice.forEach(die => { newWidth = Math.max(newWidth, die.x + die.size + 20); newHeight = Math.max(newHeight, die.y + die.size + 20); });} if (app.renderer.width !== newWidth || app.renderer.height !== newHeight) { app.renderer.resize(newWidth, newHeight); } gameRenderer.renderGameState(gameState); const gameTitleEl = document.getElementById('game-title-active'); if (gameTitleEl) gameTitleEl.textContent = gameState.gameUid || "Game Display"; if (!gameRenderer.supportedBoardTypes.includes(gameState.boardType)) { if(statusMessageElement) statusMessageElement.textContent = `Game type "${gameState.boardType || 'unknown'}" is not supported.`; } else if (gameState.isTerminal) { if(statusMessageElement) statusMessageElement.textContent = `Game Over! Ranking: ${JSON.stringify(gameState.ranking || 'N/A')}`; } else { if(statusMessageElement) statusMessageElement.textContent = `Player ${gameState.currentPlayer}'s turn. Turn: ${gameState.turn || 'N/A'}`; } }
    async function handleCellClick(clickData) { if (!jwtToken || !currentGameId || !currentLocalGameState || currentLocalGameState.isTerminal) { statusMessageElement.textContent = "Cannot make move: Not connected, game over, or no game loaded."; return; } let moveString = ""; if (clickData.type === "grid") { moveString = `${clickData.row},${clickData.col}`; } else if (clickData.type === "hex") { moveString = `q${clickData.q}r${clickData.r}`; } else if (clickData.type === "graph") { moveString = clickData.nodeId; } else { console.error("Unknown click data type:", clickData); return; } const movePayload = { move: moveString }; statusMessageElement.textContent = `Sending move: ${moveString} for game ${currentGameId}...`; try { const response = await fetch(`/games/${currentGameId}/move`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}`}, body: JSON.stringify(movePayload) }); const data = await response.json(); if (response.ok) { statusMessageElement.textContent = 'Move sent. Waiting for WebSocket update...'; } else { statusMessageElement.textContent = `Move failed: ${data.error || response.statusText}`; } } catch (error) { statusMessageElement.textContent = `Move error: ${error.message}`; } }
    const defaultGridGameState_def = { gameUid: "Tic-Tac-Toe (Mock)", boardType: "grid", boardProperties: { rows: 3, cols: 3, cellWidth: 100, cellHeight: 100, strokeColor: 0x333333, lineWidth:3 }, pieceProperties: { "1": { type: "P1", display: { text: "X", color: 0xFF0000, size: 0.7 } }, "2": { type: "P2", display: { text: "O", color: 0x0000FF, size: 0.7 } } }, boardState: { grid: [[null, "1", null], ["2", "1", null], [null, null, "2"]] }, currentPlayer: 1, turn: 5, isTerminal: false, ranking: [], legalMoves: ["0,0", "0,2", "2,0", "2,1"] };
    const sampleLargerGridGameState_def_actual = { gameUid: "Larger Grid (8x8 Mock)", boardType: "grid", boardProperties: { rows: 8, cols: 8, cellWidth: 40, cellHeight: 40, cellFillEven: 0xE0E0E0, cellFillOdd: 0xD0D0D0, lineWidth:1, strokeColor: 0x777777 }, pieceProperties: { "P1": { type: "P1", display: { text: "1", color: 0xFF0000, size: 0.7 } },"P2": { type: "P2", display: { shape: "circle", color: 0x0000FF, size: 0.6 } },"GK": { type: "GoldKing", display: { text: "K", color: 0xDAA520, size: 0.8 } },"IMG": { type: "ImagePiece", display: { imageUrl: "https://via.placeholder.com/30x30.png?text=IMG" } } }, boardState: { grid: Array(8).fill(null).map(() => Array(8).fill(null)) }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: [] }; sampleLargerGridGameState_def_actual.boardState.grid[0][0] = "P1"; sampleLargerGridGameState_def_actual.boardState.grid[7][7] = "P2"; sampleLargerGridGameState_def_actual.boardState.grid[3][3] = "GK"; sampleLargerGridGameState_def_actual.boardState.grid[4][4] = "IMG";
    const sampleHexGameState_def_actual = { gameUid: "Hex Grid Game (Mock)", boardType: "hex", boardProperties: { hexRadius: 40, rows: 7, cols: 7, lineWidth: 1, strokeColor: 0x666666, fillColor:0xF0F0F0, orientation: 'pointy-top' }, pieceProperties: { "A": { type: "A", display: { shape: "circle", color: 0xFF8800, size: 0.6 } },"B": { type: "B", display: { text: "B", color: 0x0088FF, size: 0.7 } } }, boardState: { sites: { "0,0": "A", "1,0": "B", "-1,1": "A", "0,-1": "B" } }, currentPlayer: 1, turn: 3, isTerminal: false, ranking: [], legalMoves: ["0,1", "1,-1", "-1,0"] };
    const sampleGraphGameState_def_actual = { gameUid: "Simple Graph Game (Mock)", boardType: "graph", boardProperties: { nodes: [ { id: "n1", label: "N1", x: 100, y: 100, size: 30, color: 0xAAAAFF }, { id: "n2", label: "N2", x: 250, y: 80, size: 30, color: 0xAAAAFF }, { id: "n3", label: "N3", x: 150, y: 200, size: 40, color: 0xFFAA99 }, { id: "n4", label: "N4", x: 300, y: 220, size: 30, color: 0xAAAAFF } ], edges: [ { id: "e1", from: "n1", to: "n2", thickness: 3, color: 0x888888 }, { id: "e2", from: "n1", to: "n3", thickness: 2, color: 0x777777 }, { id: "e3", from: "n2", to: "n3", thickness: 2, color: 0x777777 }, { id: "e4", from: "n3", to: "n4", thickness: 3, color: 0x888888 } ], defaultNodeSize: 25, defaultNodeColor: 0xCCCCCC, defaultEdgeColor: 0x999999, defaultEdgeThickness: 2 }, pieceProperties: { "P1": { type: "P1", display: { shape: "circle", color: 0x00AA00, size: 0.5 } }, "P2": { type: "P2", display: { text: "@", color: 0xAA00AA, size: 0.6 } } }, boardState: { sites: { "n1": "P1", "n4": "P2" } }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: ["n2", "n3"] };
    const sampleCardGameState_def = { gameUid: "Card Game (Mock)", boardType: "none", boardProperties: {}, cardZones: { "playerHand": { x: 50, y: 250, layout: "horizontal_fan", cardWidth: 70, cardHeight: 100, spacing: 30, cards: [ {id:"c1", faceUp:true, type:"std", value:"KH"}, {id:"c2", faceUp:true, type:"std", value:"AS"}, {id:"c3", faceUp:true, type:"std", value:"10C"}, {id:"c4", faceUp:false, type:"std", value:"card_back"} ] }, "deck": { x: 450, y: 50, layout: "stack", cardWidth: 70, cardHeight: 100, spacing: 2, cards: Array(10).fill(null).map((_,i) => ({id:`dk${i}`, faceUp:false, type:"std", value:"card_back"})) } }, pieceProperties: { "KH": { display: { text: "K♥", color: 0xFF0000, cardColor: 0xFFFFFF, size: 0.4 } }, "AS": { display: { text: "A♠", color: 0x000000, cardColor: 0xFFFFFF, size: 0.4 } }, "10C": { display: { text: "10♣", color: 0x000000, cardColor: 0xFFFFFF, size: 0.3 } }, "card_back": { display: { backColor: 0x0000FF, color:0xEEEEEE, text:"?", size:0.5 } } }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: [] };
    const sampleDiceGameState_def = { gameUid: "Dice Roll (Mock)", boardType: "none", boardProperties: {}, dice: [ {id: "d1", value: 6, type: "standard_d6", x: 100, y: 100, size: 50}, {id: "d2", value: 3, type: "standard_d6", x: 180, y: 100, size: 50} ], pieceProperties: { "standard_d6": { display: { faceColor: 0xF0F0F0, pipColor: 0x000000, strokeColor: 0x333333 } } }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: [] };
    const sampleOverlayGameState_def = { gameUid: "Overlay Test (Grid)", boardType: "grid", boardProperties: { rows: 5, cols: 5, cellWidth: 60, cellHeight: 60, cellFillEven: 0xFFFFFF, cellFillOdd: 0xEEEEEE }, pieceProperties: { "P1": { type: "P1", display: { shape: "circle", color: 0xAA0000, size: 0.7 } }, "P2": { type: "P2", display: { shape: "rect", color: 0x0000AA, size: 0.7 } } }, boardState: { grid: [ ["P1", null, null, null, "P2"], [null, null, null, null, null], [null, null, "P1", null, null], [null, null, null, null, null], ["P2", null, null, null, "P1"] ] }, boardOverlays: [ { type: "highlight_sites", sites: ["0,0", "2,2", "4,4"], color: 0x00FF00, alpha: 0.4 }, { type: "highlight_sites", sites: ["0,4", "4,0"], color: 0xFF00FF, alpha: 0.25 }, { type: "icon_on_sites", sites: ["1,1", "3,3"], imageUrl: "https://via.placeholder.com/20x20.png?text=!", size: 25, alpha: 0.9 } ], currentPlayer: 1, turn: 10, isTerminal: false, ranking: [], legalMoves: ["1,0", "1,2", "1,3", "1,4"] };
    const sampleUnsupportedGameState_def = { gameUid: "Hyper Toroidal Chess (Unsupported)", boardType: "hyper_toroidal_chess", boardProperties: { dimension: 7 }, pieceProperties: { "K": { display: { text: "K" } } }, boardState: { sites: { "loc1": "K" } }, currentPlayer: 1, turn: 1, isTerminal: false, ranking: [], legalMoves: [] };

    function setupMockButtonListeners() {
        loadDefaultGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = defaultGridGameState_def; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadLargerGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleLargerGridGameState_def_actual; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadHexGridButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleHexGameState_def_actual; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadGraphGameButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleGraphGameState_def_actual; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadCardTestButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleCardGameState_def; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadDiceTestButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleDiceGameState_def; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadOverlayTestButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleOverlayGameState_def; updateGameDisplay(currentLocalGameState); showView(gameActiveView); });
        loadUnsupportedGameButton.addEventListener('click', () => { currentGameId = null; if(webSocket) webSocket.close(); wsStatusElement.textContent="WebSocket: Disconnected (mock loaded)"; currentLocalGameState = sampleUnsupportedGameState_def; updateGameDisplay(currentLocalGameState); /* GameRenderer handles unsupported msg & view */ });
    }
    setupMockButtonListeners();
    initPixiApp(initialCanvasWidth, initialCanvasHeight).then(() => { showView(loginView); statusMessageElement.textContent = "Please login."; }).catch(err => { console.error("Error initializing PixiJS App:", err); statusMessageElement.textContent = "Fatal Error: Could not initialize graphics."; });
});

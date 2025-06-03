package com.ludii.LudiiServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import spark.Request;
import spark.Response;
import spark.Spark;

import static spark.Spark.*;

public class Main {

    private static final String JWT_SECRET = "your-very-strong-and-secret-jwt-key"; // TODO: Use a strong, environment-specific secret
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static AuthService authService;
    private static DatabaseService databaseService;
    private static LudiiGameService ludiiGameService;
    private static Gson gson;


    public static void main(String[] args) {
        databaseService = new DatabaseService(); // Initializes DB and creates tables if not exists
        authService = new AuthService(databaseService, JWT_SECRET);
        ludiiGameService = new LudiiGameService();
        gson = new Gson();

        // Configure Spark port (optional)
        port(8080);

        // WebSocket endpoint for game events
        // IMPORTANT: WebSocket mapping MUST be done before any HTTP route mapping or filters.
        webSocket("/games/:gameId/events", com.ludii.LudiiServer.GameEventsWebSocketHandler.class);

        // --- API Endpoints ---

        // POST /register
        // Body: {"username": "user", "password": "password"}
        // Response: {"message": "User registered successfully", "userId": 1} or error
        post("/register", (req, res) -> {
            res.type(CONTENT_TYPE_JSON);
            try {
                UserRegistrationRequest registrationRequest = gson.fromJson(req.body(), UserRegistrationRequest.class);
                if (registrationRequest == null || registrationRequest.getUsername() == null || registrationRequest.getPassword() == null) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("Username and password are required."));
                }

                User newUser = authService.registerUser(registrationRequest.getUsername(), registrationRequest.getPassword());
                if (newUser != null) {
                    res.status(201); // Created
                    return gson.toJson(new RegistrationSuccessResponse("User registered successfully", newUser.getId()));
                } else {
                    res.status(409); // Conflict - User already exists or other registration error
                    return gson.toJson(new ErrorResponse("Registration failed. User might already exist or input is invalid."));
                }
            } catch (JsonSyntaxException e) {
                res.status(400); // Bad Request
                return gson.toJson(new ErrorResponse("Invalid JSON format."));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500); // Internal Server Error
                return gson.toJson(new ErrorResponse("An unexpected error occurred."));
            }
        });

        // POST /login
        // Body: {"username": "user", "password": "password"}
        // Response: {"token": "jwt_token_here"} or error
        post("/login", (req, res) -> {
            res.type(CONTENT_TYPE_JSON);
            try {
                UserLoginRequest loginRequest = gson.fromJson(req.body(), UserLoginRequest.class);
                if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("Username and password are required."));
                }

                String token = authService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());
                if (token != null) {
                    return gson.toJson(new TokenResponse(token));
                } else {
                    res.status(401); // Unauthorized
                    return gson.toJson(new ErrorResponse("Login failed. Invalid username or password."));
                }
            } catch (JsonSyntaxException e) {
                res.status(400); // Bad Request
                return gson.toJson(new ErrorResponse("Invalid JSON format."));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500); // Internal Server Error
                return gson.toJson(new ErrorResponse("An unexpected error occurred during login."));
            }
        });

        // POST /logout
        // Header: "Authorization: Bearer <token>"
        // Response: {"message": "Logged out successfully"} or error
        post("/logout", Main::handleLogout);

        // GET /protected_resource (Example of a protected route)
        // Header: "Authorization: Bearer <token>"
        // Response: {"message": "Welcome, <username>! This is a protected resource."} or error
        get("/protected_resource", Main::handleProtectedResource);

        // --- Game Management Endpoints ---

        // GET /games/available
        // Response: ["GameName1.lud", "GameName2.lud", ...]
        get("/games/available", Main::handleListAvailableGames, gson::toJson);

        // GET /games/open
        // Response: List of open GameSession objects (those waiting for player 2)
        // Requires authentication
        get("/games/open", Main::handleListOpenGameSessions, gson::toJson);

        // POST /games/create
        // Header: "Authorization: Bearer <token>"
        // Body: {"gameName": "Tic-Tac-Toe.lud"}
        // Response: {GameSession object} or error
        post("/games/create", Main::handleCreateGameSession);

        // POST /games/:id/join
        // Header: "Authorization: Bearer <token>"
        // Param: id (game session ID)
        // Response: {GameSession object} or error
        post("/games/:id/join", Main::handleJoinGameSession);

        // GET /games/:id/state
        // Header: "Authorization: Bearer <token>" (optional, or enforce player is part of game)
        // Param: id (game session ID)
        // Response: { "gameStateJson": "..." } or error
        get("/games/:id/state", Main::handleGetGameState);

        // POST /games/:id/move
        // Header: "Authorization: Bearer <token>"
        // Body: {"move": "moveStringDetails"} (e.g., "0,0" for Tic-Tac-Toe)
        // Response: {GameSession object with updated state} or error
        post("/games/:id/move", Main::handlePlayerMove);

        // POST /games/:id/ai-move
        // Header: "Authorization: Bearer <token>" (ensure it's current player's turn or allow for AI testing)
        // Response: {GameSession object with updated state after AI move} or error
        post("/games/:id/ai-move", Main::handleAIMove);

        // Filter to log requests (optional)
        before((req, res) -> {
            System.out.println("Received API call: " + req.requestMethod() + " " + req.uri());
            // Note: For POST requests with JSON body, req.body() consumes the stream.
            // If you need to log the body and also use it in the handler, you'll need to cache it.
            // For simplicity, not logging body here.
        });

        // Exception handling for unhandled exceptions
        exception(Exception.class, (exception, request, response) -> {
            response.type(CONTENT_TYPE_JSON);
            response.status(500);
            exception.printStackTrace(); // Log the exception
            response.body(gson.toJson(new ErrorResponse("An unexpected internal server error occurred.")));
        });

        System.out.println("Server started on port 8080. JWT Secret: " + JWT_SECRET.substring(0,10) + "..."); // Be careful logging secrets

        // Crucial: Initialize Spark and then set AuthService for WebSocket Handler
        // Spark.init() is effectively called by the first route mapping.
        // awaitInitialization() ensures the server is started before we try to use its components.
        Spark.awaitInitialization();
        GameEventsWebSocketHandler.setAuthService(authService);
    }

    // Removed custom init() method, logic moved to end of main()


    // --- Auth Handler Methods ---
    private static Object handleRegister(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        try {
            UserRegistrationRequest registrationRequest = gson.fromJson(req.body(), UserRegistrationRequest.class);
            if (registrationRequest == null || registrationRequest.getUsername() == null || registrationRequest.getPassword() == null) {
                res.status(400); return gson.toJson(new ErrorResponse("Username and password are required."));
            }
            User newUser = authService.registerUser(registrationRequest.getUsername(), registrationRequest.getPassword());
            if (newUser != null) {
                res.status(201); return gson.toJson(new RegistrationSuccessResponse("User registered successfully", newUser.getId()));
            } else {
                res.status(409); return gson.toJson(new ErrorResponse("Registration failed. User might already exist or input is invalid."));
            }
        } catch (JsonSyntaxException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid JSON format."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred."));
        }
    }

    private static Object handleLogin(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        try {
            UserLoginRequest loginRequest = gson.fromJson(req.body(), UserLoginRequest.class);
            if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                res.status(400); return gson.toJson(new ErrorResponse("Username and password are required."));
            }
            String token = authService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());
            if (token != null) {
                return gson.toJson(new TokenResponse(token));
            } else {
                res.status(401); return gson.toJson(new ErrorResponse("Login failed. Invalid username or password."));
            }
        } catch (JsonSyntaxException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid JSON format."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred during login."));
        }
    }

    private static Object handleLogout(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        if (token == null) {
            res.status(401); return gson.toJson(new ErrorResponse("Logout failed. Authorization token is missing."));
        }
        if (authService.logoutUser(token)) {
            return gson.toJson(new MessageResponse("Logged out successfully."));
        } else {
            res.status(400); return gson.toJson(new ErrorResponse("Logout failed. Token may be invalid or already logged out."));
        }
    }

    private static Object handleProtectedResource(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        if (token == null) {
            res.status(401); return gson.toJson(new ErrorResponse("Access denied. Authorization token is missing."));
        }
        if (authService.validateToken(token)) {
            String username = authService.getUsernameFromToken(token);
            return gson.toJson(new MessageResponse("Welcome, " + username + "! This is a protected resource."));
        } else {
            res.status(401); return gson.toJson(new ErrorResponse("Access denied. Invalid or expired token."));
        }
    }

    // --- Game Handler Methods ---

    private static Object handleListAvailableGames(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        return ludiiGameService.listAvailableGames(); // Already returns List<String>, Gson will handle
    }

    private static Object handleListOpenGameSessions(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        if (token == null || !authService.validateToken(token)) {
            res.status(401);
            return gson.toJson(new ErrorResponse("Unauthorized: Valid token required to list open games."));
        }
        return databaseService.getOpenGameSessions(); // Returns List<GameSession>
    }


    private static Object handleCreateGameSession(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        if (token == null || !authService.validateToken(token)) {
            res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: Valid token required to create a game."));
        }

        try {
            CreateGameRequest createRequest = gson.fromJson(req.body(), CreateGameRequest.class);
            if (createRequest == null || createRequest.getGameName() == null || createRequest.getGameName().trim().isEmpty()) {
                res.status(400); return gson.toJson(new ErrorResponse("Game name is required."));
            }

            String gameName = createRequest.getGameName();
            // Step 1: Load the Ludii Game object
            Object gameObj = ludiiGameService.loadGame(gameName); // Returns Game, but use Object if imports are conditional
            if (gameObj == null) {
                 res.status(404); return gson.toJson(new ErrorResponse("Ludii game could not be loaded: " + gameName));
            }
            ludii.game.Game game = (ludii.game.Game) gameObj; // Cast to Ludii Game object

            // Step 2: Initialize state using the Game object and get JsonContext string
            String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(game);
            if (initialStateJson == null) {
                res.status(500); return gson.toJson(new ErrorResponse("Failed to initialize game state for: " + gameName));
            }

            User currentUser = authService.decodeTokenAndGetUser(token);
            if (currentUser == null) {
                 res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: User not found for token."));
            }

            // Store gameName or a more complete game identifier if needed. For now, gameName is used.
            // The ludii_game_string column in DB will store gameName, assuming GameLoader can reload from it.
            GameSession newSession = databaseService.createGameSession(gameName, gameName, initialStateJson, currentUser.getId());
            if (newSession != null) {
                res.status(201); return gson.toJson(newSession);
            } else {
                res.status(500); return gson.toJson(new ErrorResponse("Failed to create game session in database."));
            }
        } catch (JsonSyntaxException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid JSON format for create game request."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred creating game."));
        }
    }

    private static Object handleJoinGameSession(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
         if (token == null || !authService.validateToken(token)) {
            res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: Valid token required to join a game."));
        }

        try {
            int gameId = Integer.parseInt(req.params(":id"));
            User currentUser = authService.decodeTokenAndGetUser(token);
             if (currentUser == null) {
                 res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: User not found for token."));
            }

            GameSession session = databaseService.findGameSessionById(gameId);
            if (session == null) {
                res.status(404); return gson.toJson(new ErrorResponse("Game session not found."));
            }
            if (session.getPlayer1Id() == currentUser.getId()) {
                res.status(400); return gson.toJson(new ErrorResponse("Cannot join your own game as player 2."));
            }
            if (session.getPlayer2Id() != null) {
                res.status(409); return gson.toJson(new ErrorResponse("Game session is already full."));
            }
            if (!"WAITING_FOR_PLAYER".equals(session.getStatus())) {
                 res.status(409); return gson.toJson(new ErrorResponse("Game is not waiting for players. Current status: " + session.getStatus()));
            }


            boolean joined = databaseService.joinGameSession(gameId, currentUser.getId());
            if (joined) {
                GameSession updatedSession = databaseService.findGameSessionById(gameId); // Fetch updated session
                return gson.toJson(updatedSession);
            } else {
                res.status(500); // Or 409 if it's a conflict like already joined / not joinable
                return gson.toJson(new ErrorResponse("Failed to join game session. It might be already full or you are the creator."));
            }
        } catch (NumberFormatException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid game ID format."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred joining game."));
        }
    }

    private static Object handleGetGameState(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        User currentUser = null;
        if (token != null && authService.validateToken(token)) {
            currentUser = authService.decodeTokenAndGetUser(token);
        }

        try {
            int gameId = Integer.parseInt(req.params(":id"));
            GameSession session = databaseService.findGameSessionById(gameId);

            if (session == null) {
                res.status(404); return gson.toJson(new ErrorResponse("Game session not found."));
            }

            // Optional: Add logic here to restrict access only to players involved, if desired.
            // For now, if a user is authenticated and part of the game, or if no auth, show state.
            // If token is provided but user is not part of game, could deny.
            // For simplicity, current logic: if session exists, return its state.
            // Consider enhancing this if game states should be private.
             if (currentUser != null) { // If authenticated, check if player is part of the game
                 boolean isPlayerInGame = (session.getPlayer1Id() == currentUser.getId()) ||
                                          (session.getPlayer2Id() != null && session.getPlayer2Id() == currentUser.getId());
                 if (!isPlayerInGame) {
                     // Optionally, allow spectators for public games or implement specific spectator logic
                     // For now, if authenticated but not a player, could restrict or allow.
                     // Let's assume for now that if authenticated, must be a player to see state.
                     // res.status(403); return gson.toJson(new ErrorResponse("Forbidden: You are not a player in this game."));
                 }
             } else {
                 // If not authenticated at all, decide if public viewing is allowed.
                 // For now, let's allow it if no token provided.
             }


            // Return only essential state info, not the full Ludii game string unless necessary
            JsonObject gameStateResponse = new JsonObject();
            gameStateResponse.addProperty("gameId", session.getId());
            gameStateResponse.addProperty("gameName", session.getGameName());
            // Deserialize to JsonContext to ensure it's valid, then re-serialize for the response
            JsonContext currentJsonContext = ludiiGameService.deserializeJsonContext(session.getCurrentStateJson());
            gameStateResponse.add("gameState", gson.toJsonTree(currentJsonContext)); // Embed JsonContext as an object
            gameStateResponse.addProperty("status", session.getStatus());
            gameStateResponse.addProperty("player1Id", session.getPlayer1Id());
            if (session.getPlayer2Id() != null) {
                 gameStateResponse.addProperty("player2Id", session.getPlayer2Id());
            }
             return gson.toJson(gameStateResponse);

        } catch (NumberFormatException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid game ID format."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred fetching game state."));
        }
    }

    private static Object handlePlayerMove(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        if (token == null || !authService.validateToken(token)) {
            res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: Valid token required to make a move."));
        }

        try {
            int gameId = Integer.parseInt(req.params(":id"));
            PlayerMoveRequest moveRequest = gson.fromJson(req.body(), PlayerMoveRequest.class);
            if (moveRequest == null || moveRequest.getMove() == null || moveRequest.getMove().trim().isEmpty()) {
                res.status(400); return gson.toJson(new ErrorResponse("Move details are required."));
            }

            User currentUser = authService.decodeTokenAndGetUser(token);
            if (currentUser == null) {
                 res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: User not found for token."));
            }

            GameSession session = databaseService.findGameSessionById(gameId);
            if (session == null) {
                res.status(404); return gson.toJson(new ErrorResponse("Game session not found."));
            }
            if (!"IN_PROGRESS".equals(session.getStatus())) {
                res.status(400); return gson.toJson(new ErrorResponse("Game is not currently in progress. Status: " + session.getStatus()));
            }

            JsonContext currentJsonContext = ludiiGameService.deserializeJsonContext(session.getCurrentStateJson());
            if (currentJsonContext == null) {
                res.status(500); return gson.toJson(new ErrorResponse("Could not parse current game state."));
            }

            int playerMakingMoveId = -1;
            if(currentUser.getId() == session.getPlayer1Id()) playerMakingMoveId = 1;
            else if(session.getPlayer2Id() != null && currentUser.getId() == session.getPlayer2Id()) playerMakingMoveId = 2;
            else {
                res.status(403); return gson.toJson(new ErrorResponse("Forbidden: You are not a player in this game."));
            }

            // Server-side validation of whose turn it is, is implicitly handled by LudiiGameService.applyMove
            // if it checks context.state().mover() against playerMakingMoveId which it should.
            // The recreateLudiiContext will set the mover, and applyMoveLogic will verify it.

            String newJsonStateString = ludiiGameService.applyMove(
                    session.getGameName(),
                    session.getCurrentStateJson(),
                    moveRequest.getMove(),
                    playerMakingMoveId
            );

            if (newJsonStateString == null) {
                res.status(400); return gson.toJson(new ErrorResponse("Invalid move or failed to apply: " + moveRequest.getMove()));
            }

            JsonContext updatedParsedJsonContext = ludiiGameService.deserializeJsonContext(newJsonStateString);
            String newStatus = (updatedParsedJsonContext != null && updatedParsedJsonContext.isTerminal()) ? "FINISHED" : "IN_PROGRESS";

            boolean dbUpdated = databaseService.updateGameSessionState(gameId, newJsonStateString, newStatus);

            if (dbUpdated) {
                GameSession updatedSession = databaseService.findGameSessionById(gameId); // Fetch the final state
                // Broadcast the update
                JsonContext broadcastContext = ludiiGameService.deserializeJsonContext(updatedSession.getCurrentStateJson());
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", "GAME_STATE_UPDATE");
                payload.put("data", broadcastContext);
                GameEventsWebSocketHandler.broadcastGameStateUpdate(gameId, payload);
                return gson.toJson(updatedSession);
            } else {
                res.status(500); return gson.toJson(new ErrorResponse("Failed to update game state in database."));
            }

        } catch (NumberFormatException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid game ID format."));
        } catch (JsonSyntaxException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid JSON format for move request."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred making a move."));
        }
    }

    private static Object handleAIMove(Request req, Response res) {
        res.type(CONTENT_TYPE_JSON);
        String token = getTokenFromHeader(req);
        // AI move might be triggered by current player or could be a general 'poke AI' endpoint.
        // For simplicity, let's require a valid token, but not strictly enforce it's the AI's turn.
        if (token == null || !authService.validateToken(token)) {
            res.status(401); return gson.toJson(new ErrorResponse("Unauthorized: Valid token required to request AI move."));
        }

        try {
            int gameId = Integer.parseInt(req.params(":id"));
            GameSession session = databaseService.findGameSessionById(gameId);
            if (session == null) {
                res.status(404); return gson.toJson(new ErrorResponse("Game session not found."));
            }
            if (!"IN_PROGRESS".equals(session.getStatus())) {
                res.status(400); return gson.toJson(new ErrorResponse("Game is not currently in progress. Status: " + session.getStatus()));
            }

            JsonContext currentJsonContext = ludiiGameService.deserializeJsonContext(session.getCurrentStateJson());
            if (currentJsonContext == null) {
                res.status(500); return gson.toJson(new ErrorResponse("Could not parse current game state."));
            }

            // Here, we could check if the authenticated user is the one whose turn it is,
            // or if the game settings allow AI to play for a specific player.
            // For now, let's assume AI plays for the current player in context.
            int aiPlayerNumber = currentJsonContext.getCurrentPlayer(); // This is P1 or P2

            String aiMoveString = ludiiGameService.generateAIMove(session.getGameName(), session.getCurrentStateJson());
            if (aiMoveString == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("AI could not determine a move (game might be over or no legal moves)."));
            }

            System.out.println("AI for player " + aiPlayerNumber + " intends to play move: " + aiMoveString);

            // Apply the AI's move
            String newJsonStateStringAfterAIMove = ludiiGameService.applyMove(
                    session.getGameName(),
                    session.getCurrentStateJson(), // current state before AI move
                    aiMoveString,
                    aiPlayerNumber // The AI plays as the current player
            );

            if (newJsonStateStringAfterAIMove == null) {
                res.status(500); return gson.toJson(new ErrorResponse("AI made an invalid move or failed to apply: " + aiMoveString));
            }

            JsonContext updatedParsedJsonContext = ludiiGameService.deserializeJsonContext(newJsonStateStringAfterAIMove);
            String newStatus = (updatedParsedJsonContext != null && updatedParsedJsonContext.isTerminal()) ? "FINISHED" : "IN_PROGRESS";

            boolean dbUpdated = databaseService.updateGameSessionState(gameId, newJsonStateStringAfterAIMove, newStatus);

            if (dbUpdated) {
                GameSession updatedSession = databaseService.findGameSessionById(gameId);
                JsonContext broadcastContext = ludiiGameService.deserializeJsonContext(updatedSession.getCurrentStateJson());
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", "GAME_STATE_UPDATE");
                payload.put("data", broadcastContext);
                GameEventsWebSocketHandler.broadcastGameStateUpdate(gameId, payload);
                return gson.toJson(updatedSession);
            } else {
                res.status(500); return gson.toJson(new ErrorResponse("Failed to update game state after AI move."));
            }

        } catch (NumberFormatException e) {
            res.status(400); return gson.toJson(new ErrorResponse("Invalid game ID format for AI move."));
        } catch (Exception e) {
            e.printStackTrace(); res.status(500); return gson.toJson(new ErrorResponse("An unexpected error occurred during AI move."));
        }
    }


    // --- Utility and POJOs ---

    private static String getTokenFromHeader(Request req) {
        String authHeader = req.headers("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // --- Auth Request/Response POJOs ---

    static class UserRegistrationRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    static class UserLoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    static class RegistrationSuccessResponse {
        String message;
        int userId;
        public RegistrationSuccessResponse(String message, int userId) {this.message = message; this.userId = userId;}
    }

    static class TokenResponse {
        String token;
        public TokenResponse(String token) { this.token = token; }
    }

    static class MessageResponse {
        String message;
        public MessageResponse(String message) { this.message = message; }
    }

    static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    // --- Game Request/Response POJOs ---
    static class CreateGameRequest {
        private String gameName;
        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }
    }
    // PlayerMoveRequest is in its own file: PlayerMoveRequest.java
}

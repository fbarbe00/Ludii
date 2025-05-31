package com.example.auth;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@WebSocket
public class GameEventsWebSocketHandler {

    // Map: Game ID -> Set of Sessions for that game
    private static final Map<Integer, Set<Session>> gameSubscribers = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson(); // For sending JSON messages

    // To validate tokens, we need access to AuthService. This is tricky with static handlers.
    // For now, we'll make it static or pass it in if possible, though this is not ideal for clean architecture.
    // A better way would involve a WebSocket server setup that allows dependency injection or context passing.
    // SparkJava's simple WebSocket setup makes this a bit direct.
    private static AuthService authService;

    public static void setAuthService(AuthService service) {
        authService = service;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        // Extract gameId and token from query parameters
        // Example path: /games/{gameId}/events?token=YOUR_JWT_TOKEN
        String gameIdStr = session.getUpgradeRequest().getParameterMap().get("gameId").get(0);
        String token = session.getUpgradeRequest().getParameterMap().get("token").get(0);

        if (gameIdStr == null || token == null) {
            System.out.println("WebSocket Connect: Missing gameId or token in query params. Closing session.");
            session.close(4000, "Missing gameId or token");
            return;
        }

        Integer gameId;
        try {
            gameId = Integer.parseInt(gameIdStr);
        } catch (NumberFormatException e) {
            System.out.println("WebSocket Connect: Invalid gameId format. Closing session.");
            session.close(4000, "Invalid gameId format");
            return;
        }

        if (authService == null) {
            System.err.println("WebSocket Connect: AuthService not set. Cannot validate token. Closing session.");
            session.close(5000, "Server configuration error: AuthService not available.");
            return;
        }

        User user = authService.decodeTokenAndGetUser(token);
        if (user == null) {
            System.out.println("WebSocket Connect: Invalid or expired token for game " + gameId + ". Closing session.");
            session.close(4001, "Invalid or expired token");
            return;
        }

        // Check if user is actually part of this game (optional, but good for security)
        // This would require DatabaseService access here or a method in AuthService/GameService
        // For now, a valid token for any user is enough to subscribe to a game's events.
        // TODO: Enhance security: ensure user is a player in gameId.

        gameSubscribers.computeIfAbsent(gameId, k -> new CopyOnWriteArraySet<>()).add(session);
        System.out.println("WebSocket Connect: Session " + session.hashCode() + " (User: " + user.getUsername() + ") connected to game " + gameId);

        // Optionally, send a welcome message or current game state upon connection
        // GameSession game = Main.databaseService.findGameSessionById(gameId); // Assuming Main.databaseService is accessible
        // if (game != null && game.getCurrentStateJson() != null) {
        //     JsonContext ctx = Main.ludiiGameService.deserializeJsonContext(game.getCurrentStateJson());
        //     session.getRemote().sendString(gson.toJson(Map.of("type", "gameState", "data", ctx)));
        // }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        // Attempt to remove session from all game subscriptions it might be in
        gameSubscribers.forEach((gameId, sessions) -> {
            if (sessions.remove(session)) {
                System.out.println("WebSocket Close: Session " + session.hashCode() + " disconnected from game " + gameId + ". Reason: " + reason + " (Code: " + statusCode + ")");
            }
        });
        // If a game has no more subscribers, remove the gameId entry to free memory
        gameSubscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket Error for session " + session.hashCode() + ": " + error.getMessage());
        error.printStackTrace(System.err);
        // onClose will typically be called subsequently by Jetty
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        // This server-side WebSocket is primarily for broadcasting updates from the server to clients.
        // We might not expect messages from clients other than perhaps an initial auth message or pings.
        // If client messages were expected (e.g., chat, or moves over WebSocket), handle them here.
        System.out.println("WebSocket Message from " + session.hashCode() + ": " + message);
        // Example: if client sends a ping
        if ("PING".equalsIgnoreCase(message)) {
            try {
                session.getRemote().sendString("PONG");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Broadcasts a message (typically game state update) to all subscribers of a specific game.
     * @param gameId The ID of the game whose state has changed.
     * @param messagePayload The object to be serialized to JSON and sent (e.g., JsonContext or a wrapper).
     */
    public static void broadcastGameStateUpdate(int gameId, Object messagePayload) {
        Set<Session> sessions = gameSubscribers.getOrDefault(gameId, Collections.emptySet());
        if (sessions.isEmpty()) {
            System.out.println("Broadcast: No clients subscribed to game " + gameId);
            return;
        }

        String jsonMessage = gson.toJson(messagePayload);
        System.out.println("Broadcasting to " + sessions.size() + " clients for game " + gameId + ": " + jsonMessage.substring(0, Math.min(jsonMessage.length(), 100)) + "...");

        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getRemote().sendString(jsonMessage);
                } catch (IOException e) {
                    System.err.println("Error broadcasting to session " + session.hashCode() + ": " + e.getMessage());
                    // Optionally remove session if send fails repeatedly, or rely on onClose
                }
            } else {
                // Optionally remove closed sessions if encountered here, though onClose should handle it
                sessions.remove(session);
            }
        }
    }
}

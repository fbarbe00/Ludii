package com.ludii.LudiiServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ludii.game.Game;
import ludii.game.GameLoader;
import ludii.game.Context;
import ludii.game.Trial;
import ludii.game.State;
import ludii.game.board.Board;
import ludii.game.data.IContainerState;
import ludii.game.moves.Move;
// import ludii.game.player.Player; // Not directly used yet but good for context
// import ludii.game.common.LudiiLogs; // For listing games, if possible
import ludii.ai.AIFactory;
import ludii.ai.AI;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
// import java.util.stream.Collectors; // Not used in current listAvailableGames

public class LudiiGameService {

    private final Gson gson;

    public LudiiGameService() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.setPrettyPrinting().create();
    }

    public List<String> listAvailableGames() {
        // Using hardcoded list as resource scanning is environment-dependent
        return Arrays.asList(
                "Amazons.lud", "Arimaa.lud", "Breakthrough.lud", "Chess.lud", "Go.lud",
                "Hex.lud", "Havannah.lud", "LinesOfAction.lud", "Reversi.lud",
                "Tic-Tac-Toe.lud", "Yavalath.lud", "Connect4.lud"
        );
    }

    public Game loadGame(String gameNameOrPath) {
        try {
            return GameLoader.loadGameFromName(gameNameOrPath, null);
        } catch (Exception e) {
            System.err.println("Error loading Ludii game '" + gameNameOrPath + "': " + e.getMessage());
            // e.printStackTrace(); // Optionally print stack trace for more debug info
            return null;
        }
    }

    /**
     * Converts a Ludii Game Context object to our JsonContext DTO.
     */
    private JsonContext convertLudiiContextToJsonContext(Context ludiiContext, Game game) {
        if (ludiiContext == null || game == null) return null;

        JsonContext jsonContext = new JsonContext();
        jsonContext.setGameUid(game.name());

        State state = ludiiContext.state();
        jsonContext.setCurrentPlayer(state.mover());
        jsonContext.setTurn(state.turn().getCount());
        jsonContext.setTerminal(state.terminal());
        jsonContext.setRanking(state.ranking());

        Map<String, Object> boardStateMap = new HashMap<>();
        IContainerState containerState = state.containerStates()[0]; // Assuming main board is the first container

        if (containerState instanceof Board) { // Check if it's a Board instance
            Board board = (Board) containerState;
            int rows = board.height();
            int cols = board.width();
            List<List<Integer>> grid = new ArrayList<>(rows);
            for (int r = 0; r < rows; ++r) {
                List<Integer> rowList = new ArrayList<>(cols);
                for (int c = 0; c < cols; ++c) {
                    rowList.add(board.get(r, c)); // piece owner (0 for empty, 1 for P1, 2 for P2 etc.)
                }
                grid.add(rowList);
            }
            boardStateMap.put("grid", grid);
            // Add more specific board info if needed, e.g. board.gridToString() for debugging
        } else {
            boardStateMap.put("representation", "Non-grid or complex board type: " + containerState.getClass().getSimpleName());
            boardStateMap.put("rawStateString", state.toString()); // For debugging non-standard boards
        }
        jsonContext.setBoardState(boardStateMap);

        List<String> legalMovesStr = new ArrayList<>();
        if (!state.terminal()) {
            ludii.game.common.FastArrayList<Move> ludiiMoves = game.moves(ludiiContext).moves();
            if (ludiiMoves != null) {
                for (Move move : ludiiMoves) {
                    if (move != null) {
                        legalMovesStr.add(move.toString());
                    }
                }
            }
        }
        jsonContext.setLegalMoves(legalMovesStr);
        return jsonContext;
    }

    public String initializeGameAndGetInitialStateJson(Game game) {
        if (game == null) {
            System.err.println("Cannot initialize game: Game object is null.");
            return null;
        }
        Context context = new Context(game, new Trial(game));
        game.start(context);
        JsonContext jsonContext = convertLudiiContextToJsonContext(context, game);
        return gson.toJson(jsonContext);
    }

    public String serializeJsonContext(JsonContext jsonContext) {
        if (jsonContext == null) return null;
        return gson.toJson(jsonContext);
    }

    public JsonContext deserializeJsonContext(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) return null;
        try {
            return gson.fromJson(jsonString, JsonContext.class);
        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("Error deserializing JsonContext: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reconstructs a Ludii Context from a JsonContext DTO and a Game object.
     * Focuses on restoring essential state for simpler games like Tic-Tac-Toe.
     * This is a complex operation and may not perfectly restore all game types or states.
     */
    public Context recreateLudiiContext(JsonContext jsonContext, Game game) {
        if (jsonContext == null || game == null) {
            System.err.println("Cannot recreate context: jsonContext or game is null.");
            return null;
        }

        Context context = new Context(game, new Trial(game));
        game.start(context); // Start with a clean, initial context

        // Restore board state (example for Tic-Tac-Toe like grid)
        Map<String, Object> boardStateMap = jsonContext.getBoardState();
        if (boardStateMap != null && boardStateMap.containsKey("grid")) {
            Object gridObj = boardStateMap.get("grid");
            if (gridObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<Object>> grid = (List<List<Object>>) gridObj;
                IContainerState containerState = context.state().containerStates()[0];
                if (containerState instanceof Board) {
                    Board board = (Board) containerState;
                    for (int r = 0; r < grid.size(); ++r) {
                        List<Object> row = grid.get(r);
                        for (int c = 0; c < row.size(); ++c) {
                            Object cellValue = row.get(c);
                            if (cellValue instanceof Number) { // Ludii uses int for player pieces
                                int pieceOwner = ((Number) cellValue).intValue();
                                if (pieceOwner != 0) { // 0 is empty
                                    // This is simplified: assumes piece type is implicitly known or default.
                                    // For games with multiple piece types per player, this needs enhancement.
                                    // Ludii's board.set() directly sets the piece owner.
                                    // Finding the correct Component ID is more robust.
                                    // For Tic-Tac-Toe, player 1 is 'X' (often component ID 1), player 2 is 'O' (component ID 2).
                                    // We assume player IDs in JsonContext (1, 2) map directly to piece states on board.
                                    board.set(r, c, pieceOwner);
                                } else {
                                    board.set(r, c, 0); // Clear cell if it was 0 or null
                                }
                            } else if (cellValue == null) {
                                board.set(r,c,0); // Clear cell
                            }
                        }
                    }
                }
            }
        }

        // Restore current player
        context.state().setMover(jsonContext.getCurrentPlayer());

        // Restore turn count (be cautious, direct turn setting might be tricky)
        // Ludii's Turn object can be complex. This is a simplification.
        // It's often better if the game naturally reaches this turn count via move replay if possible.
        if (context.state().turn().getCount() != jsonContext.getTurn()) {
             // This might not be directly settable or might have side effects.
             // For now, we assume it's informational or the game will align.
             // A more robust way would be to find a Ludii API to set turn if available and safe.
             System.out.println("Note: Turn count in JsonContext (" + jsonContext.getTurn() +
                                ") vs Ludii Context (" + context.state().turn().getCount() +
                                "). Direct turn setting is complex; relying on move application to adjust.");
        }

        // If game was terminal, reflect that. (Moves can't be made on terminal state anyway)
        if (jsonContext.isTerminal()) {
            context.state().setTerminal(true);
            if (jsonContext.getRanking() != null) {
                 // context.state().setRanking(jsonContext.getRanking()); // setRanking might not be public/direct
                 // Usually ranking is a result of the game ending.
            }
        }

        return context;
    }

    private boolean applyMoveLogic(Game game, Context context, String clientMoveString, int playerID) {
        if (context.state().mover() != playerID) {
            System.err.println("ApplyMoveLogic: Player " + playerID + " attempted to move, but it's player " + context.state().mover() + "'s turn.");
            return false;
        }

        ludii.game.common.FastArrayList<Move> legalMoves = game.moves(context).moves();
        Move matchedMove = null;
        for (Move move : legalMoves) {
            if (move.toString().equals(clientMoveString)) {
                matchedMove = move;
                break;
            }
        }

        if (matchedMove != null) {
            game.apply(context, matchedMove);
            return true;
        } else {
            System.err.println("ApplyMoveLogic: Move '" + clientMoveString + "' not found in legal moves for player " + playerID);
            System.err.println("Legal moves were: " + legalMoves.toString());
            return false;
        }
    }

    public String applyMove(String gameName, String currentJsonState, String clientMoveString, int playerMakingMoveId) {
        Game game = loadGame(gameName);
        if (game == null) return null; // Error already logged by loadGame

        JsonContext currentJsonContext = deserializeJsonContext(currentJsonState);
        if (currentJsonContext == null) return null; // Error already logged

        Context ludiiContext = recreateLudiiContext(currentJsonContext, game);
        if (ludiiContext == null) return null;

        if (ludiiContext.state().terminal()){
            System.err.println("Cannot apply move: Game is already terminal.");
            return null; // Or return current state as no move can be made
        }

        boolean success = applyMoveLogic(game, ludiiContext, clientMoveString, playerMakingMoveId);
        if (success) {
            JsonContext newJsonContext = convertLudiiContextToJsonContext(ludiiContext, game);
            return serializeJsonContext(newJsonContext);
        }
        return null; // Move was invalid or failed
    }

    private String generateAIMoveLogic(Game game, Context context) {
        if (context.state().terminal()) return null;

        int aiPlayerNum = context.state().mover();
        // Using a simple default AI (e.g., UCT with short thinking time)
        // The options string can be complex. For UCT: {"Thinking Time": {"value": 1.0}} for 1s.
        // A simpler AI like "Random" might not need options: "{\"AI\": {\"algorithm\": \"Random\"}}"
        String aiOptions = String.format("{\"AI\": {\"algorithm\": \"UCT\", \"Thinking Time\": {\"value\": 0.1}}}"); // 0.1s

        AI ludiiAI = null;
        try {
            // Parameters for createAI: Game game, int playerID, String optionsString, int maxSeconds, int maxIterations, int maxDepth, String playoutType
            ludiiAI = AIFactory.createAI(game, aiPlayerNum, aiOptions, 1, -1, -1, null);
            if (ludiiAI == null) {
                 System.err.println("Failed to create AI for player " + aiPlayerNum);
                 // Fallback to random if UCT fails to init for some reason (e.g. options string)
                 ludiiAI = AIFactory.createAI(game, aiPlayerNum, "{\"AI\": {\"algorithm\": \"Random\"}}", 1, -1, -1, null);
            }

            if (ludiiAI != null) {
                Move aiMove = ludiiAI.selectAction(game, context, 0.1, -1, -1, null); // Max thinking time 0.1s
                if (aiMove != null) {
                    return aiMove.toString();
                } else {
                    System.err.println("AI for player " + aiPlayerNum + " returned null move.");
                }
            }
        } catch (Exception e) {
            System.err.println("Exception during AI move generation for player " + aiPlayerNum + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ludiiAI != null) {
                ludiiAI.closeAI();
            }
        }
        return null;
    }

    public String generateAIMove(String gameName, String currentJsonState) {
        Game game = loadGame(gameName);
        if (game == null) return null;

        JsonContext currentJsonContext = deserializeJsonContext(currentJsonState);
        if (currentJsonContext == null) return null;

        Context ludiiContext = recreateLudiiContext(currentJsonContext, game);
        if (ludiiContext == null || ludiiContext.state().terminal()) {
            System.err.println("Cannot generate AI move: context is null or game is terminal.");
            return null;
        }

        return generateAIMoveLogic(game, ludiiContext);
    }
}

package com.example.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Type;


import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class LudiiGameServiceTest {

    private LudiiGameService ludiiGameService;
    private Gson gson;

    @Before
    public void setUp() {
        ludiiGameService = new LudiiGameService();
        gson = new Gson();
    }

    @Test
    public void testListAvailableGames() {
        List<String> games = ludiiGameService.listAvailableGames();
        assertNotNull(games);
        assertFalse(games.isEmpty());
        assertTrue(games.contains("Tic-Tac-Toe.lud"));
        assertTrue(games.contains("Chess.lud"));
    }

    @Test
    public void testGetLudiiGameString_TicTacToe() {
        String gameString = ludiiGameService.getLudiiGameString("Tic-Tac-Toe.lud");
        assertNotNull(gameString);
        assertTrue(gameString.contains("(game \"Tic-Tac-Toe\""));
    }

    @Test
    public void testGetLudiiGameString_Chess() {
        String gameString = ludiiGameService.getLudiiGameString("Chess.lud");
        assertNotNull(gameString);
        assertTrue(gameString.contains("(game \"Chess\""));
    }

    @Test
    public void testGetLudiiGameString_UnknownGame() {
        String gameString = ludiiGameService.getLudiiGameString("UnknownGame.lud");
        assertNull(gameString);
    }

    @Test
    public void testInitializeGameAndGetInitialStateJson_TicTacToe() {
        String gameName = "Tic-Tac-Toe.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        assertNotNull(initialStateJson);

        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class);
        assertEquals(gameName, context.getGameUid());
        assertEquals(1, context.getCurrentPlayer());
        assertEquals(1, context.getTurn());
        assertFalse(context.isTerminal());
        assertNotNull(context.getBoardState());
        assertTrue(context.getBoardState().containsKey("grid"));
        assertNotNull(context.getLegalMoves());
        assertFalse(context.getLegalMoves().isEmpty());
    }

    @Test
    public void testInitializeGameAndGetInitialStateJson_Chess() {
        String gameName = "Chess.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        assertNotNull(initialStateJson);

        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class);
        assertEquals(gameName, context.getGameUid());
        assertEquals(1, context.getCurrentPlayer());
        // ... other basic checks for chess initial state structure
        assertNotNull(context.getBoardState().get("pieces"));
    }

    @Test
    public void testInitializeGameAndGetInitialStateJson_NullGameString() {
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson("SomeGame.lud", null);
        assertNull(initialStateJson);
    }


    @Test
    public void testSerializeAndDeserializeJsonContext() {
        JsonContext originalContext = new JsonContext();
        originalContext.setGameUid("TestGame.lud");
        originalContext.setCurrentPlayer(1);
        originalContext.setTurn(5);
        originalContext.setTerminal(false);
        Map<String, Object> board = new java.util.HashMap<>();
        board.put("cell", "value");
        originalContext.setBoardState(board);
        originalContext.setLegalMoves(java.util.Arrays.asList("move1", "move2"));

        String jsonString = ludiiGameService.serializeJsonContext(originalContext);
        assertNotNull(jsonString);

        JsonContext deserializedContext = ludiiGameService.deserializeJsonContext(jsonString);
        assertNotNull(deserializedContext);
        assertEquals(originalContext.getGameUid(), deserializedContext.getGameUid());
        assertEquals(originalContext.getCurrentPlayer(), deserializedContext.getCurrentPlayer());
        assertEquals(originalContext.getTurn(), deserializedContext.getTurn());
        assertEquals(originalContext.isTerminal(), deserializedContext.isTerminal());
        assertEquals(originalContext.getBoardState(), deserializedContext.getBoardState());
        assertEquals(originalContext.getLegalMoves(), deserializedContext.getLegalMoves());
    }

    @Test
    public void testDeserializeJsonContext_InvalidJson() {
        JsonContext context = ludiiGameService.deserializeJsonContext("this is not json");
        assertNull(context);
    }

    @Test
    public void testDeserializeJsonContext_NullOrEmpty() {
        assertNull(ludiiGameService.deserializeJsonContext(null));
        assertNull(ludiiGameService.deserializeJsonContext(""));
        assertNull(ludiiGameService.deserializeJsonContext("  "));
    }


    @Test
    public void testApplyMove_TicTacToe_ValidMove() {
        String gameName = "Tic-Tac-Toe.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class);

        assertNotNull(context);
        assertEquals(1, context.getCurrentPlayer());

        JsonContext updatedContext = ludiiGameService.applyMove(context, "0,0", 1);
        assertNotNull(updatedContext);
        assertEquals(2, updatedContext.getCurrentPlayer()); // Player should switch
        assertEquals(2, updatedContext.getTurn());          // Turn should increment

        Type mapType = new TypeToken<Map<String, List<List<String>>>>() {}.getType();
        Map<String, List<List<String>>> boardState = gson.fromJson(gson.toJson(updatedContext.getBoardState()), mapType);
        assertEquals("P1", boardState.get("grid").get(0).get(0)); // Check if piece was placed
        assertFalse(updatedContext.getLegalMoves().contains("0,0"));
    }

    @Test
    public void testApplyMove_TicTacToe_InvalidMove_WrongPlayer() {
        String gameName = "Tic-Tac-Toe.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class); // P1's turn

        JsonContext updatedContext = ludiiGameService.applyMove(context, "0,0", 2); // P2 tries to move
        assertNull(updatedContext); // Should be invalid
    }

    @Test
    public void testApplyMove_TicTacToe_InvalidMove_CellOccupied() {
        String gameName = "Tic-Tac-Toe.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class);

        // P1 makes a move
        context = ludiiGameService.applyMove(context, "0,0", 1);
        assertNotNull(context);

        // P2's turn, tries to move to occupied cell 0,0
        JsonContext updatedContext = ludiiGameService.applyMove(context, "0,0", 2);
        assertNull(updatedContext);
    }


    @Test
    public void testGenerateAIMove_TicTacToe() {
        String gameName = "Tic-Tac-Toe.lud";
        String ludString = ludiiGameService.getLudiiGameString(gameName);
        String initialStateJson = ludiiGameService.initializeGameAndGetInitialStateJson(gameName, ludString);
        JsonContext context = gson.fromJson(initialStateJson, JsonContext.class);

        String aiMove = ludiiGameService.generateAIMove(context);
        assertNotNull(aiMove);
        assertTrue(context.getLegalMoves().contains(aiMove)); // AI move should be from legal moves
    }

    @Test
    public void testGenerateAIMove_NoLegalMoves() {
        JsonContext context = new JsonContext();
        context.setGameUid("TestGame.lud");
        context.setTerminal(false);
        context.setLegalMoves(java.util.Collections.emptyList());

        String aiMove = ludiiGameService.generateAIMove(context);
        assertNull(aiMove);
    }

    @Test
    public void testGenerateAIMove_TerminalGame() {
        JsonContext context = new JsonContext();
        context.setGameUid("TestGame.lud");
        context.setTerminal(true);
        context.setLegalMoves(java.util.Arrays.asList("0,0"));

        String aiMove = ludiiGameService.generateAIMove(context);
        assertNull(aiMove);
    }
}

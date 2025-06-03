package com.ludii.LudiiServer;

import java.util.List;
import java.util.Map;

/**
 * A simplified POJO to represent the game state (context) that will be serialized to JSON.
 * This replaces the direct serialization of Ludii's Context object for now.
 */
public class JsonContext {
    private String gameUid; // Unique identifier for the game type, e.g., from .lud file
    private int currentPlayer; // Who is to move
    private int turn;
    private boolean isTerminal;
    private double[] ranking; // Player rankings if game is over
    private Map<String, Object> boardState; // Simplified representation, e.g., a map or custom object
    private List<String> legalMoves; // Optional: list of legal moves in a simplified format

    // Constructors
    public JsonContext() {}

    public JsonContext(String gameUid, int currentPlayer, int turn, boolean isTerminal, double[] ranking, Map<String, Object> boardState) {
        this.gameUid = gameUid;
        this.currentPlayer = currentPlayer;
        this.turn = turn;
        this.isTerminal = isTerminal;
        this.ranking = ranking;
        this.boardState = boardState;
    }

    // Getters and Setters
    public String getGameUid() {
        return gameUid;
    }

    public void setGameUid(String gameUid) {
        this.gameUid = gameUid;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public void setTerminal(boolean terminal) {
        isTerminal = terminal;
    }

    public double[] getRanking() {
        return ranking;
    }

    public void setRanking(double[] ranking) {
        this.ranking = ranking;
    }

    public Map<String, Object> getBoardState() {
        return boardState;
    }

    public void setBoardState(Map<String, Object> boardState) {
        this.boardState = boardState;
    }

    public List<String> getLegalMoves() {
        return legalMoves;
    }

    public void setLegalMoves(List<String> legalMoves) {
        this.legalMoves = legalMoves;
    }
}

package manager.api;

import java.util.List;
import java.util.Objects;

/**
 * A Data Transfer Object (DTO) that represents the state of a game at a
 * specific point in time. This object is immutable and provides a snapshot
 * of the game that can be safely passed to the UI.
 */
public class GameState {

    private final Object board; // Using Object for flexibility, could be a 2D array, a string, etc.
    private final int currentPlayer;
    private final GameStatus status;
    private final List<MoveInfo> moveHistory;
    private final double evaluation; // For the evaluation bar

    public enum GameStatus {
        IN_PROGRESS,
        WIN,
        LOSS,
        DRAW
    }

    public GameState(Object board, int currentPlayer, GameStatus status, List<MoveInfo> moveHistory, double evaluation) {
        this.board = board;
        this.currentPlayer = currentPlayer;
        this.status = status;
        this.moveHistory = moveHistory;
        this.evaluation = evaluation;
    }

    public Object getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GameStatus getStatus() {
        return status;
    }

    public List<MoveInfo> getMoveHistory() {
        return moveHistory;
    }

    public double getEvaluation() {
        return evaluation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        return currentPlayer == gameState.currentPlayer &&
                Double.compare(gameState.evaluation, evaluation) == 0 &&
                Objects.equals(board, gameState.board) &&
                status == gameState.status &&
                Objects.equals(moveHistory, gameState.moveHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, currentPlayer, status, moveHistory, evaluation);
    }

    @Override
    public String toString() {
        return "GameState{" +
                "board=" + board +
                ", currentPlayer=" + currentPlayer +
                ", status=" + status +
                ", moveHistory=" + moveHistory +
                ", evaluation=" + evaluation +
                '}';
    }
}
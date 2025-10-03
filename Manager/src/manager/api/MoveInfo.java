package manager.api;

import java.util.Objects;

/**
 * A Data Transfer Object (DTO) that represents a move in a game.
 */
public class MoveInfo {

    private final String move;
    private final int player;

    public MoveInfo(String move, int player) {
        this.move = move;
        this.player = player;
    }

    public String getMove() {
        return move;
    }

    public int getPlayer() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveInfo moveInfo = (MoveInfo) o;
        return player == moveInfo.player &&
                Objects.equals(move, moveInfo.move);
    }

    @Override
    public int hashCode() {
        return Objects.hash(move, player);
    }

    @Override
    public String toString() {
        return "MoveInfo{" +
                "move='" + move + '\'' +
                ", player=" + player +
                '}';
    }
}
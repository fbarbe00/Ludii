package com.example.auth;

public class PlayerMoveRequest {
    private String move;
    // Optional: could include things like game version/hash if needed for validation

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }
}

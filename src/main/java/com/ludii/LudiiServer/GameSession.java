package com.ludii.LudiiServer;

import java.sql.Timestamp;

public class GameSession {
    private int id;
    private String gameName;
    private String ludiiGameString; // Could be very large, represents the .lud file content
    private String currentStateJson; // JSON representation of game state
    private int player1Id;
    private Integer player2Id; // Nullable
    private String status; // e.g., WAITING_FOR_PLAYER, IN_PROGRESS, FINISHED
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors

    public GameSession(String gameName, String ludiiGameString, String currentStateJson, int player1Id, String status) {
        this.gameName = gameName;
        this.ludiiGameString = ludiiGameString;
        this.currentStateJson = currentStateJson;
        this.player1Id = player1Id;
        this.status = status;
    }

    public GameSession(int id, String gameName, String ludiiGameString, String currentStateJson, int player1Id, Integer player2Id, String status, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.gameName = gameName;
        this.ludiiGameString = ludiiGameString;
        this.currentStateJson = currentStateJson;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getLudiiGameString() {
        return ludiiGameString;
    }

    public void setLudiiGameString(String ludiiGameString) {
        this.ludiiGameString = ludiiGameString;
    }

    public String getCurrentStateJson() {
        return currentStateJson;
    }

    public void setCurrentStateJson(String currentStateJson) {
        this.currentStateJson = currentStateJson;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(int player1Id) {
        this.player1Id = player1Id;
    }

    public Integer getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(Integer player2Id) {
        this.player2Id = player2Id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "GameSession{" +
               "id=" + id +
               ", gameName='" + gameName + '\'' +
               ", player1Id=" + player1Id +
               ", player2Id=" + player2Id +
               ", status='" + status + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}

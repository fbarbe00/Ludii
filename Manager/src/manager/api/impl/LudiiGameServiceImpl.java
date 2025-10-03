package manager.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import manager.api.GameInfo;
import manager.api.GameObserver;
import manager.api.GameState;
import manager.api.LudiiGameService;
import manager.api.MoveInfo;

/**
 * A concrete implementation of the {@link LudiiGameService}. This class
 * encapsulates Ludii's core game logic, providing a clean, headless API
 * for UIs to consume.
 */
public class LudiiGameServiceImpl implements LudiiGameService {

    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();
    private final manager.Manager manager;
    private final manager.Referee referee;

    /**
     * A private inner class that provides a "headless" implementation of the
     * PlayerInterface. This is used to satisfy the requirements of the Manager
     * class without coupling the service to the Swing UI.
     */
    private class HeadlessPlayerInterface implements manager.PlayerInterface {
        @Override public org.json.JSONObject getNameFromJar() { return null; }
        @Override public org.json.JSONObject getNameFromJson() { return null; }
        @Override public org.json.JSONObject getNameFromAiDef() { return null; }
        @Override public void loadGameFromName(String name, List<String> options, boolean debug) {}
        @Override public void addTextToStatusPanel(String text) { System.out.println("Status: " + text); }
        @Override public void addTextToAnalysisPanel(String text) { System.out.println("Analysis: " + text); }
        @Override public void selectAnalysisTab() {}
        @Override public void repaint() {}
        @Override public void reportForfeit(int playerForfeitNumber) {}
        @Override public void reportTimeout(int playerForfeitNumber) {}
        @Override public void reportDrawAgreed() {}
        @Override public void updateFrameTitle(boolean alsoUpdateMenu) {}
        @Override public void updateTabs(other.context.Context context) {}
        @Override public void restartGame() {}
        @Override public void repaintTimerForPlayer(int playerId) {}
        @Override public void setTemporaryMessage(String text) {}
        @Override public void refreshNetworkDialog() {}
        @Override public void postMoveUpdates(other.move.Move move, boolean noAnimation) {
            onGameModelUpdate();
        }
    }

    public LudiiGameServiceImpl() {
        this.manager = new manager.Manager(new HeadlessPlayerInterface());
        this.referee = this.manager.ref();
    }

    @Override
    public List<GameInfo> listAvailableGames() {
        List<GameInfo> gameInfos = new ArrayList<>();
        String[] gameFiles = main.FileHandling.listGames();

        for (String gameFile : gameFiles) {
            try {
                String ludPath = other.GameLoader.getFilePath(gameFile);
                if (ludPath == null) continue;

                java.io.InputStream in = other.GameLoader.class.getResourceAsStream(ludPath);
                if (in == null) continue;

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                String ludString = sb.toString();

                game.Game game = compiler.Compiler.compile(ludString, false);
                if (game != null) {
                    String name = game.name();
                    String description = game.description().getHeader("About");
                    if (description == null) {
                        description = "A Ludii game.";
                    }
                    int minPlayers = game.players().min();
                    int maxPlayers = game.players().max();
                    String iconPath = "/ludii-logo-100x100.png";

                    gameInfos.add(new GameInfo(name, description, minPlayers, maxPlayers, iconPath, ludPath));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return gameInfos;
    }

    @Override
    public void startNewGame(GameInfo gameInfo) {
        try {
            String ludPath = gameInfo.getFilePath();

            if (ludPath == null) {
                notifyError("Game file path is missing for: " + gameInfo.getName());
                return;
            }

            java.io.InputStream in = other.GameLoader.class.getResourceAsStream(ludPath);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            String ludString = sb.toString();

            game.Game game = compiler.Compiler.compile(ludString, false);
            if (game == null) {
                notifyError("Failed to compile game: " + gameInfo.getName());
                return;
            }

            this.referee.setGame(this.manager, game);
            GameState initialState = createGameStateFromContext();
            notifyObservers(initialState);
        } catch (Exception e) {
            e.printStackTrace();
            notifyError("Error starting game: " + e.getMessage());
        }
    }

    private GameState createGameStateFromContext() {
        other.context.Context context = this.referee.context();
        if (context == null) return null;

        Object board = context.board().toString();
        int currentPlayer = context.state().mover();

        GameState.GameStatus status = GameState.GameStatus.IN_PROGRESS;
        if (context.trial().over()) {
            double[] ranking = context.trial().ranking();
            if (ranking.length > 1 && ranking[0] > ranking[1]) {
                status = GameState.GameStatus.WIN;
            } else if (ranking.length > 1 && ranking[0] < ranking[1]) {
                status = GameState.GameStatus.LOSS;
            } else {
                status = GameState.GameStatus.DRAW;
            }
        }

        List<MoveInfo> moveHistory = new ArrayList<>();
        for (other.move.Move m : context.trial().generateCompleteMovesList()) {
            moveHistory.add(new MoveInfo(m.toString(), m.mover()));
        }

        double evaluation = 0.0;
        if (manager.aiSelected()[currentPlayer] != null && manager.aiSelected()[currentPlayer].ai() != null) {
            evaluation = manager.aiSelected()[currentPlayer].ai().estimateValue();
        }

        return new GameState(board, currentPlayer, status, moveHistory, evaluation);
    }

    @Override
    public void makeMove(MoveInfo moveInfo) {
        if (referee.context() == null) {
            notifyError("No game in progress.");
            return;
        }

        other.context.Context context = referee.context();
        game.moves.Moves legalMoves = context.game().moves(context);
        other.move.Move move_to_apply = null;

        // This is a simplification. A real implementation would need a more robust
        // way to match the MoveInfo DTO to a real Move object. For now, we'll
        // just match by string representation.
        for (other.move.Move legalMove : legalMoves.moves()) {
            if (legalMove.toString().equals(moveInfo.getMove())) {
                move_to_apply = legalMove;
                break;
            }
        }

        if (move_to_apply != null) {
            referee.applyHumanMoveToGame(manager, move_to_apply);
        } else {
             notifyError("Invalid or illegal move: " + moveInfo.getMove());
        }
    }

    @Override
    public void requestAIMove() {
        if (referee.context() == null) {
            notifyError("No game in progress.");
            return;
        }
        referee.nextMove(manager, false);
    }

    private void onGameModelUpdate() {
        GameState newState = createGameStateFromContext();
        if (newState != null) {
            notifyObservers(newState);
        }
    }

    @Override
    public void closeGame() {
        System.out.println("Closing game.");
        observers.clear();
    }

    @Override
    public void addObserver(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(GameState newState) {
        for (GameObserver observer : observers) {
            observer.onGameStateUpdate(newState);
        }
    }

    private void notifyError(String errorMessage) {
        for (GameObserver observer : observers) {
            observer.onError(errorMessage);
        }
    }
}
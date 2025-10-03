package manager.api;

import java.util.List;

/**
 * The main service for interacting with the Ludii game engine. This interface
 * provides a clean, UI-agnostic API for listing games, managing game sessions,
 * and handling player actions. It is designed to be completely independent
 * of any specific UI framework.
 */
public interface LudiiGameService {

    /**
     * Retrieves a list of all games available in Ludii.
     *
     * @return A list of {@link GameInfo} objects, each representing a game.
     */
    List<GameInfo> listAvailableGames();

    /**
     * Starts a new game session for the specified game.
     *
     * @param gameInfo The game to start.
     */
    void startNewGame(GameInfo gameInfo);

    /**
     * Submits a move for the current player in the active game session.
     *
     * @param move The move to be made.
     */
    void makeMove(MoveInfo move);

    /**
     * Requests the AI to make a move for the current player.
     */
    void requestAIMove();

    /**
     * Closes the current game session and releases any associated resources.
     */
    void closeGame();

    /**
     * Adds an observer to the game service.
     * @param observer The observer to add.
     */
    void addObserver(GameObserver observer);

    /**
     * Removes an observer from the game service.
     * @param observer The observer to remove.
     */
    void removeObserver(GameObserver observer);
}
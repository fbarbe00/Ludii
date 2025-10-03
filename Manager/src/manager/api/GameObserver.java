package manager.api;

/**
 * An interface for observing game events. UI components can implement this
 * interface to receive notifications from the {@link LudiiGameService}
 * when the state of a game changes.
 */
public interface GameObserver {

    /**
     * Called when the game state has been updated. This can be triggered by
     * a new move, the start of a game, or any other event that changes the
     * state of the game.
     *
     * @param newState The new, immutable state of the game.
     */
    void onGameStateUpdate(GameState newState);

    /**
     * Called when a game-related error occurs.
     *
     * @param errorMessage A description of the error.
     */
    void onError(String errorMessage);
}
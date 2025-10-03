# Agent Development Guide for Ludii

This document provides guidance for AI agents working on the Ludii codebase. It outlines the new architecture and best practices to follow.

## New UI/API Architecture

The application has been refactored to follow a modern, API-first architecture. This separates the core game logic from the user interface, making the codebase more modular, maintainable, and ready for future web-based frontends.

### Key Components

*   **`manager.api.LudiiGameService`**: This is the central, headless service for all game-related interactions. It provides a clean, UI-agnostic API for listing games, managing game sessions, and handling player actions. **All new UI components should interact with the game logic through this service.**

*   **`manager.api.GameObserver`**: This interface uses the Observer design pattern to allow UI components to subscribe to game events. The `LudiiGameService` notifies observers when the game state changes, ensuring that the UI is always in sync with the core logic without direct coupling.

*   **Data Transfer Objects (DTOs)**: The `manager.api` package contains several DTOs (`GameInfo`, `GameState`, `MoveInfo`) that are used to transfer data between the `LudiiGameService` and the UI. These are simple, immutable objects that provide a stable data contract.

### UI Development Guidelines

*   **New UI components should be placed in the `PlayerDesktop/src/app` directory**, in appropriate sub-packages (e.g., `library`, `details`, `game`).
*   UI components **must not** directly access the `Manager` or `Referee` classes. All interactions with the game logic must go through the `LudiiGameService`.
*   When a UI component needs to react to changes in the game state, it should implement the `GameObserver` interface and register itself with the `LudiiGameService`.

## Building the Application

The application can be built using the Ant script located in the `PlayerDesktop` directory:

```bash
cd PlayerDesktop
ant export_jar_public
```

This will create a runnable `Ludii.jar` file in the `PlayerDesktop/build` directory.
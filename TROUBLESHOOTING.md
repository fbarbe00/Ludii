# Troubleshooting Guide - Ludii Game Server

This guide provides tips for common issues encountered during the setup, build, or running of the Ludii Game Server and its frontend.

## General Tips

*   **Check Server Console Logs:** The Spark Java backend prints log messages to the standard output (the console where you ran the `java -jar ...` or `mvn exec:java ...` command). These logs often contain detailed error messages and stack traces.
*   **Check Browser Developer Console:** For frontend issues, open your browser's developer console (usually by pressing F12). Look for errors in the "Console" tab and inspect network requests in the "Network" tab.
*   **Verify Configurations:** Double-check hardcoded values mentioned in `RUNNING_THE_SERVER.md`, such as database credentials in `DatabaseService.java` and the JWT secret in `AuthService.java`, if you've modified them or your environment differs.

## Build Failures

*   **`error reading /app/lib/ludii-core.jar; zip END header not found`**
    *   **Cause:** This indicates that the `ludii-core.jar` file in the `lib/` directory is either not a valid JAR file, is incomplete (corrupted download), or is a placeholder.
    *   **Solution:** Ensure `ludii-core.jar` in the `lib/` directory is a valid, complete JAR file obtained from an official Ludii source (e.g., [Ludii GitHub Releases](https://github.com/Ludeme/Ludii/releases)). The `pom.xml` uses `systemPath` and expects this file to be physically present and correct. (`args4j.jar` is no longer a direct project dependency).

*   **"Cannot find symbol" or "package ... does not exist" errors for Ludii classes (e.g., `ludii.game.Game`)**
    *   **Cause:** This usually follows the JAR reading error mentioned above. If the Ludii JARs cannot be read, their classes are not available to the compiler. It can also occur if the `pom.xml` dependencies for Ludii JARs (with `systemPath`) are missing, commented out, or point to the wrong location.
    *   **Solution:** Verify the Ludii JARs in `lib/` are valid and that the `pom.xml` correctly references them.

*   **"Cannot find symbol" for project's own classes (e.g., `com.ludii.LudiiServer.User`)**
    *   **Cause:** This might indicate an issue with your IDE's project setup if it's not recognizing the package structure, or if a refactoring of package names was incomplete (e.g., `package` statement correct, but an import statement still points to an old package).
    *   **Solution:** Ensure all `package` statements at the top of your Java files are `package com.ludii.LudiiServer;`. Ensure any import statements for classes within this project use the correct new package name if they were referencing an old one. `mvn clean package` should recompile everything; if errors persist, check file contents carefully.

## Server Fails to Start

*   **`Address already in use` or `Failed to bind to /0.0.0.0:8080`**
    *   **Cause:** Port `8080` (or the configured port) is already being used by another application.
    *   **Solution:** Stop the other application using the port, or configure a different port in `com.ludii.LudiiServer.Main.java` by changing `Spark.port(xxxx);` and update client-side calls if necessary.

*   **Database Connection Errors (check server logs for `PSQLException` or similar)**
    *   **Cause:**
        1.  PostgreSQL server is not running or not accessible from where the Java application is running.
        2.  Incorrect database credentials (host, port, DB name, user, password) in `DatabaseService.java`.
        3.  The specified database (`user_auth_db`) or user (`app_user`) does not exist, or the user does not have sufficient privileges on the database.
    *   **Solution:**
        1.  Verify PostgreSQL is running and accessible (e.g., try connecting with `psql` or pgAdmin).
        2.  Double-check the hardcoded credentials in `DatabaseService.java` against your PostgreSQL setup.
        3.  Ensure the database and user are created as per `RUNNING_THE_SERVER.md` and that `app_user` has necessary permissions.

*   **`ClassNotFoundException` for `org.postgresql.Driver`**
    *   **Cause:** The PostgreSQL JDBC driver dependency is missing from the classpath. This shouldn't happen if using `mvn package` as it's declared in `pom.xml`. If running manually without Maven, ensure the driver JAR is included.
    *   **Solution:** Ensure `org.postgresql:postgresql` dependency is correctly listed in `pom.xml` and not commented out. Rebuild with `mvn clean package`.

*   **JWT Secret Issues (e.g., `The Secret cannot be null` during `AuthService` initialization)**
    *   **Cause:** The `JWT_SECRET` constant in `AuthService.java` might be null or too short if modified. The `Algorithm.HMAC256()` requires a non-null secret.
    *   **Solution:** Ensure `JWT_SECRET` in `AuthService.java` is a valid, non-null string of sufficient length.

*   **`IllegalStateException: This must be done before route mapping has begun` (for WebSockets)**
    *   **Cause:** `Spark.webSocket(...)` was called after HTTP routes (`get`, `post`, etc.) or filters (`before`, `after`) were defined.
    *   **Solution:** Ensure `Spark.webSocket(...)` is called immediately after `Spark.port(...)` and before any other route or filter definitions in `Main.java`.

## Login/Registration Issues

*   **"User already exists" during registration:**
    *   **Cause:** The username you are trying to register is already taken in the database.
    *   **Solution:** Choose a different username.

*   **"Login failed. Invalid username or password."**:
    *   **Cause:** Incorrect username or password, or the user does not exist.
    *   **Solution:** Verify credentials. Ensure the user has been registered.

*   **No JWT token received after successful login (or token is null/empty in client):**
    *   **Cause:** Check server logs for any errors during the login process or token generation in `AuthService.java`.
    *   **Solution:** Debug `AuthService.loginUser()` and `AuthService.generateToken()`.

## WebSocket Connection Failures (Client-Side)

*   **"WebSocket: Connection error" or similar in browser console:**
    *   **Cause 1:** Server is not running or not reachable at the specified address/port.
    *   **Cause 2:** The WebSocket endpoint path is incorrect (e.g., `/games/{gameId}/events`).
    *   **Cause 3:** Server-side errors during WebSocket handshake (check server logs). The `GameEventsWebSocketHandler.onConnect` method might be failing due to token validation issues or incorrect `gameId`.
    *   **Cause 4:** Firewall or proxy settings blocking WebSocket connections (less common for localhost development).
    *   **Solution:**
        1.  Verify the server is running and accessible.
        2.  Double-check the WebSocket URL constructed in `frontend/js/main.js`.
        3.  Inspect server logs for any errors related to `GameEventsWebSocketHandler` when a connection is attempted.
        4.  Ensure `gameId` and `token` are correctly passed as query parameters in the WebSocket URL from the client, as the handler expects them.

## Game Logic/Rendering Issues

*   **Incorrect game state displayed or moves not working as expected:**
    *   **Cause:** This could be due to issues in `LudiiGameService.java`'s interaction with the (currently stubbed or conceptual) Ludii core logic, especially in methods like `recreateLudiiContext`, `applyMove`, or `generateAIMove`. Full Ludii integration is complex.
    *   **Solution:** Debug the relevant methods in `LudiiGameService.java` and check the `JsonContext` being generated and sent. If real Ludii JARs are used, step through Ludii's internal game logic.

*   **Visual glitches, missing elements, or errors in browser console related to PixiJS:**
    *   **Cause:** JavaScript errors in `frontend/js/components.js`, `frontend/js/renderer.js`, or `frontend/js/main.js`. The `JsonContext` from the server might not match what the renderers expect (e.g., missing `boardProperties`, incorrect `boardType`, piece IDs not found in `pieceProperties`).
    *   **Solution:** Use browser developer tools to debug JavaScript. Check for console errors. Inspect the `JsonContext` received from the server (e.g., via WebSocket messages in the Network tab or console logs) and verify it matches the structure the renderers are designed for.

*   **"Unsupported game type" message on canvas/HTML:**
    *   **Cause:** The `gameState.boardType` received from the server (or loaded from a mock) is not one of the types recognized by `GameRenderer.js` (e.g., "grid", "hex", "graph", "none").
    *   **Solution:** Implement a new renderer for this board type or correct the `boardType` string if it's a typo.
*   **Unexpected 'Unsupported game type' message:** Ensure the `boardType` field in the `JsonContext` received from the server (or in your mock data) exactly matches one of the supported types defined in `GameRenderer.js` (e.g., 'grid', 'hex', 'graph', 'none'). Check for typos or case sensitivity.

## Log Files

*   **Server-Side:** Logs are printed to the console/standard output where the Java application (`java -jar ...` or `mvn exec:java ...`) was run. There are no separate log files by default.
*   **Client-Side:** Logs are visible in the browser's Developer Console (usually accessible by pressing F12, then look for the "Console" tab).

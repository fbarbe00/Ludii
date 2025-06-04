# Running the Ludii Game Server

## Introduction

This document provides instructions on how to set up, build, and run the Ludii Game Server. The server handles user authentication, game session management, and real-time game event communication using WebSockets. It is designed to interact with a Ludii-based game logic layer and a PixiJS frontend.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK):** Version 8 or later (e.g., OpenJDK 1.8). The project is currently compiled with Java 1.8.
*   **Apache Maven:** Version 3.6 or later, for building the project.
*   **PostgreSQL:** Version 10 or later.
*   **Ludii JARs:**
    *   `ludii-core.jar` (the main Ludii engine JAR).
    *   **Important:** This JAR is **not** included in this repository due to licensing and distribution policies of Ludii. You must obtain it from an official Ludii release ([Ludii GitHub Releases](https://github.com/Ludeme/Ludii/releases)) or by building Ludii from its source code.
    *   Once obtained, create a directory named `lib/` at the root of this project (at the same level as `src/`, `pom.xml`) and place `ludii-core.jar` into it. The `pom.xml` is configured to look for it there using `systemPath`.
    *   (`args4j.jar` was previously listed but is likely not a direct dependency for the server and has been removed from the project's direct requirements.)

## Database Setup

1.  **Install PostgreSQL:** If not already installed, download and install PostgreSQL for your operating system.
2.  **Start PostgreSQL Service:** Ensure the PostgreSQL service is running.
3.  **Create Database and User:**
    *   Connect to PostgreSQL using `psql` or a GUI tool like pgAdmin.
    *   Create the database:
        ```sql
        CREATE DATABASE user_auth_db;
        ```
    *   Create the user and grant privileges:
        ```sql
        CREATE USER app_user WITH PASSWORD 'app_password';
        GRANT ALL PRIVILEGES ON DATABASE user_auth_db TO app_user;
        ALTER USER app_user CREATEDB; -- Optional, if the app needs to create databases, though not currently the case.
        ```
    *   **Note:** The server application (specifically `DatabaseService.java`) will attempt to automatically create the necessary tables (`users`, `game_sessions`) within the `user_auth_db` database if they do not already exist. It uses the hardcoded credentials `app_user` / `app_password`.

## Configuration (Important Hardcoded Values)

Please be aware that for this version of the server:

*   **Database Connection Details:** The JDBC URL, database name, username, and password for PostgreSQL are currently hardcoded in `src/main/java/com/ludii/LudiiServer/DatabaseService.java`.
    *   URL: `jdbc:postgresql://localhost:5432/user_auth_db`
    *   User: `app_user`
    *   Password: `app_password`
    You may need to modify these directly in the source code if your PostgreSQL setup differs.
*   **JWT Secret Key:** The secret key used for signing and verifying JSON Web Tokens (JWTs) is currently hardcoded in `src/main/java/com/ludii/LudiiServer/AuthService.java` (variable `JWT_SECRET`). For production, this **must** be externalized and kept secure.

## Building the Server

1.  **Navigate to Project Root:** Open a terminal or command prompt and navigate to the root directory of the project (where `pom.xml` is located).
2.  **Build with Maven:** Run the following command:
    ```bash
    mvn clean package
    ```
    This command will compile the source code, run tests (if any are not skipped), and package the application into a JAR file. The resulting JAR will be located in the `target/` directory. The name will typically be `user-auth-service-1.0-SNAPSHOT.jar` (based on the artifactId and version in `pom.xml`).

## Running the Server

To run the server, first ensure you have built it using `mvn clean package`. This will create a shaded JAR in the `target/` directory (e.g., `user-auth-service-1.0-SNAPSHOT.jar` - the `artifactId` from `pom.xml` is used by default).

1.  **Ensure Ludii JAR is Present:** Verify that the `lib/` directory exists at the project root and contains the required `ludii-core.jar` file. **Even with a shaded JAR, system-scoped dependencies like `ludii-core.jar` are often not bundled and must be available relative to where the JAR is run, or the classpath needs to be constructed to find them.**
    *   *Primary method (using shaded JAR):*
        ```bash
        java -jar target/user-auth-service-1.0-SNAPSHOT.jar
        ```
        (Note: The JAR filename `user-auth-service-1.0-SNAPSHOT.jar` depends on your project's `<artifactId>` and `<version>`. The `-shaded` suffix is not added by default by `maven-shade-plugin` unless a classifier is specified; it typically replaces the original JAR.)
        If the system-scoped `ludii-core.jar` is not found (e.g., if it's not in a `lib` directory relative to the shaded JAR at runtime, or if not using an absolute path in `systemPath`), you might need to ensure it's discoverable or use the `-cp` method:
        ```bash
        java -cp "target/user-auth-service-1.0-SNAPSHOT.jar:lib/ludii-core.jar" com.ludii.LudiiServer.Main
        ```
        This command explicitly includes `lib/ludii-core.jar` in the classpath. (Note: if other non-shaded system JARs were needed, they'd also go here or use `lib/*`).
    *   *Alternative (Original non-shaded JAR execution, useful for development if shading causes issues):*
        ```bash
        # java -cp "target/user-auth-service-1.0-SNAPSHOT.jar:lib/*" com.ludii.LudiiServer.Main
        ```
        (This line is now commented out as the shaded JAR is preferred, but kept for reference).

2.  **Server Port:** The server is configured to start on port `8080` by default. You should see log output in the console indicating the server has started. Example:
    ```
    [Thread-1] INFO spark.embeddedserver.jetty.EmbeddedJettyServer - == Spark has ignited ...
    [Thread-1] INFO spark.embeddedserver.jetty.EmbeddedJettyServer - >> Listening on 0.0.0.0:8080
    ...
    Server started on port 8080. JWT Secret: your-very-...
    ```

## Accessing the Frontend

The frontend is a set of static HTML, CSS, and JavaScript files located in the `frontend/` directory.

1.  **Direct File Access:**
    *   Navigate to the `frontend/` directory in your file explorer.
    *   Open the `index.html` file directly in a modern web browser (e.g., Chrome, Firefox, Edge). The URL will typically be like `file:///path/to/your/project/frontend/index.html`.
2.  **Using a Local HTTP Server (Recommended for full functionality):**
    *   Some browser security features or JavaScript functionalities (like certain types of API requests, though not strictly an issue for `localhost` backend calls from `file:///` in many cases) work more reliably when files are served over HTTP.
    *   You can use a simple built-in Python HTTP server for this:
        *   Open a terminal/command prompt.
        *   Navigate into the `frontend/` directory: `cd path/to/your/project/frontend/`
        *   Run: `python -m http.server 8000` (for Python 3) or `python -m SimpleHTTPServer 8000` (for Python 2).
        *   Then open `http://localhost:8000` in your web browser.
    *   Other simple HTTP servers (like Node.js `http-server`) can also be used.

The frontend will then connect to the backend server running on `http://localhost:8080`.

# Running the Ludii Game Server

## Introduction

This document provides instructions on how to set up, build, and run the Ludii Game Server. The server handles user authentication, game session management, and real-time game event communication using WebSockets. It is designed to interact with a Ludii-based game logic layer and a PixiJS frontend.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK):** Version 8 or later (e.g., OpenJDK 1.8). The project is currently compiled with Java 1.8.
*   **Apache Maven:** Version 3.6 or later, for building the project.
*   **PostgreSQL:** Version 10 or later.
*   **Ludii JARs:**
    *   `ludii-core.jar` (the main Ludii engine JAR)
    *   `args4j.jar` (a dependency for Ludii's command-line interface, potentially bundled or needed by core)
    *   **Important:** These JARs are **not** included in this repository due to licensing and distribution policies of Ludii. You must obtain them from an official Ludii release ([Ludii GitHub Releases](https://github.com/Ludeme/Ludii/releases)) or by building Ludii from its source code.
    *   Once obtained, create a directory named `lib/` at the root of this project (at the same level as `src/`, `pom.xml`) and place these JAR files into it. The `pom.xml` is configured to look for them there using `systemPath`.

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

1.  **Ensure Ludii JARs are Present:** Verify that the `lib/` directory exists at the project root and contains the required `ludii-core.jar` and `args4j.jar` files. The server will not run correctly without these due to the `systemPath` dependencies in `pom.xml`.
2.  **Execute the JAR:**
    *   Navigate to the project root directory in your terminal.
    *   Run the application using the following command:
        ```bash
        java -cp "target/user-auth-service-1.0-SNAPSHOT.jar:lib/*" com.ludii.LudiiServer.Main
        ```
        *   **Note:** The JAR filename `user-auth-service-1.0-SNAPSHOT.jar` depends on the `<artifactId>` and `<version>` in your `pom.xml`. Adjust the command if these values are different.
        *   This command explicitly includes the `lib/*` directory in the classpath so the system-scoped Ludii JARs can be found.
    *   **Alternative (if using `maven-shade-plugin` or similar to create an uber-JAR):** If the `pom.xml` were configured to build a shaded JAR that includes these local JARs (which it currently is NOT), the command might be simpler:
        `java -jar target/user-auth-service-1.0-SNAPSHOT-shaded.jar`
        However, the current setup relies on the classpath argument.
3.  **Server Port:** The server is configured to start on port `8080` by default (this was changed from Spark's default 4567 in previous steps). You should see log output in the console indicating the server has started. Example:
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

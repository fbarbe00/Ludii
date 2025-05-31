# Deployment Plan: User Auth & Ludii Game Server

This document outlines the plan for deploying the Spark Java backend, PostgreSQL database, and a frontend (served by Nginx).

## 1. Overview

The application consists of three main components:
1.  **Backend**: Spark Java application handling user authentication, game logic, and WebSocket communication.
2.  **Frontend**: Static HTML, CSS, and JavaScript files (using PixiJS for rendering).
3.  **Database**: PostgreSQL database for storing user and game session data.

Docker will be used for containerizing these components, and `docker-compose` will orchestrate them for local development and testing, and can serve as a basis for production deployment.

## 2. Dockerization

### 2.1. Backend (Spark Java Application)

A `Dockerfile` will be created for the Spark Java application.

**`backend/Dockerfile` (Illustrative - place in the Java project root or a `backend` subdirectory if sources are moved)**

```dockerfile
# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jre-slim
# Or openjdk:11-jre-slim if Java 11 was used and compatible

# Set the working directory in the container
WORKDIR /app

# Copy the fat JAR (assuming it's built and placed in target/)
# The JAR should be built by 'mvn clean package' which includes all dependencies.
COPY target/user-auth-service-1.0-SNAPSHOT.jar /app/application.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
# Ensure your Main class is correctly specified in your JAR's manifest,
# or use `java -cp application.jar com.example.auth.Main`
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

**Build Command (from Java project root):**
`docker build -t ludii-game-backend .` (if Dockerfile is in root)
or
`docker build -f backend/Dockerfile -t ludii-game-backend .`

### 2.2. Frontend (Nginx)

The frontend consists of static files. Nginx is a good choice for serving these.

**`frontend/Dockerfile` (Illustrative - place in `frontend/` directory)**

```dockerfile
# Use official nginx image as a base
FROM nginx:alpine

# Copy static assets from the frontend directory to Nginx's default serve directory
COPY . /usr/share/nginx/html

# (Optional) Copy a custom Nginx configuration if needed for proxying
# COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

**`frontend/nginx.conf` (Example for proxying to backend - optional, can be done via docker-compose too)**
This configuration assumes the frontend and backend are accessed on the same domain, with backend API calls proxied.

```nginx
server {
    listen 80;
    server_name localhost; # Or your domain

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files $uri $uri/ /index.html; # For SPAs
    }

    # Proxy API requests to the backend Spark Java service
    location /login {
        proxy_pass http://backend:8080; # 'backend' will be the service name in docker-compose
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /register {
        proxy_pass http://backend:8080;
        # ... same proxy_set_header lines ...
    }
    location /logout {
        proxy_pass http://backend:8080;
        # ... same proxy_set_header lines ...
    }
    location /protected_resource {
        proxy_pass http://backend:8080;
        # ... same proxy_set_header lines ...
    }
    location /games/ { # Trailing slash is important for prefix matches
        proxy_pass http://backend:8080;
        # ... same proxy_set_header lines ...

        # WebSocket proxying (for /games/{gameId}/events)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Add other backend paths as needed

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```
*Note*: For simplicity, the `docker-compose` setup below will handle the port mapping for the backend directly, and frontend can be served on a different port or configured more simply if not using a single entry point with Nginx as a reverse proxy. The Nginx config above is more for a production-like setup.

**Build Command (from `frontend/` directory):**
`docker build -t ludii-game-frontend .`

### 2.3. Database (PostgreSQL)

Use the official PostgreSQL image from Docker Hub. Data persistence will be managed using Docker volumes.

## 3. Docker Compose (`docker-compose.yml`)

This file will orchestrate the services. Place it in the project root.

```yaml
version: '3.8'

services:
  db:
    image: postgres:13-alpine # Or a newer stable version
    container_name: ludii_postgres_db
    environment:
      POSTGRES_USER: app_user      # Must match DatabaseService credentials
      POSTGRES_PASSWORD: app_password # Must match DatabaseService credentials
      POSTGRES_DB: user_auth_db    # Must match DatabaseService DB name
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432" # Expose PostgreSQL port to host (for development/debugging if needed)
    restart: unless-stopped

  backend:
    build:
      context: . # Assuming Dockerfile is in the project root, or specify path (e.g., ./backend)
      dockerfile: Dockerfile # Or backend/Dockerfile
    container_name: ludii_game_backend
    depends_on:
      - db
    ports:
      - "8080:8080" # Expose Spark Java port
    environment:
      # Pass database connection details if they are configurable in Spark Java app
      # (Currently hardcoded in DatabaseService.java, which is okay for this plan)
      # DB_HOST: db
      # DB_PORT: 5432
      # DB_NAME: user_auth_db
      # DB_USER: app_user
      # DB_PASSWORD: app_password
      # JWT_SECRET: "your-super-secret-from-env-or-secrets-manager" # Recommended
      # Ensure the backend waits for DB to be ready (e.g. using a wait-for-it script or retry logic in app)
    restart: unless-stopped

  frontend: # Using Nginx to serve static files
    build:
      context: ./frontend # Path to your frontend directory with its Dockerfile
      dockerfile: Dockerfile
    container_name: ludii_game_frontend
    ports:
      - "80:80" # Serve frontend on port 80 (host)
    # If Nginx is acting as a reverse proxy to backend:
    # depends_on:
    #   - backend
    # And Nginx config would point to 'http://backend:8080'
    # For simpler setup without Nginx as reverse proxy, backend is accessed on its own port (8080)
    restart: unless-stopped

volumes:
  postgres_data: # Defines the persistent volume for PostgreSQL data
```

**To run:**
`docker-compose up --build` (from the directory containing `docker-compose.yml`)

## 4. Cloud Deployment Considerations

### 4.1. General Principles
*   **Environment Variables**: Configure database credentials, JWT secrets, and other sensitive data using environment variables, not hardcoded values.
*   **Logging**: Ensure logs from all containers (backend, Nginx, PostgreSQL) are collected and managed (e.g., CloudWatch, Google Cloud Logging).
*   **Scalability**: Design for statelessness in the backend if possible, to allow horizontal scaling. WebSocket session management might need a distributed solution (e.g., Redis pub/sub) if scaling beyond a single backend instance.
*   **Database as a Service**: Use managed database services (AWS RDS, Google Cloud SQL) instead of running PostgreSQL in a Docker container in production for better reliability, backups, and scaling.
*   **HTTPS**: Terminate SSL/TLS at a load balancer or reverse proxy (like Nginx or a cloud provider's load balancer).

### 4.2. Platform Examples

*   **AWS Elastic Beanstalk**:
    *   Can deploy Docker containers (single or multi-container environments).
    *   Backend: Package Spark Java app as a JAR, then build Docker image and push to ECR. Deploy to Elastic Beanstalk.
    *   Frontend: Serve static files from S3 + CloudFront, or include Nginx container in Elastic Beanstalk multi-container setup.
    *   Database: Use AWS RDS for PostgreSQL.
    *   WebSockets: Elastic Beanstalk's Application Load Balancer (ALB) supports WebSockets.

*   **Google Cloud App Engine (Flex or Standard with services)**:
    *   App Engine Flex: Can deploy Docker containers. Similar to Elastic Beanstalk.
    *   App Engine Standard (Java runtime): Deploy JAR directly. Frontend can be a separate service or served from default service.
    *   Database: Use Google Cloud SQL.
    *   WebSockets: Supported.

*   **Kubernetes (e.g., AWS EKS, Google GKE, Azure AKS)**:
    *   Provides maximum flexibility and scalability.
    *   Define Deployments and Services for backend, frontend (Nginx).
    *   Use a StatefulSet or a managed DB service for PostgreSQL.
    *   Ingress controller for managing external access, SSL, and routing.
    *   Requires more operational expertise.

### 4.3. Build and CI/CD
*   Set up a CI/CD pipeline (e.g., Jenkins, GitLab CI, GitHub Actions).
*   Build Docker images, run tests, and push images to a container registry (Docker Hub, ECR, GCR, ACR).
*   Automate deployment to chosen cloud platform.

## 5. Pre-requisites for Deployment
*   Java 8 (or 11) JDK for building the backend.
*   Maven for building the backend.
*   Docker and Docker Compose installed for local containerization and testing.
*   Access to a container registry if deploying to cloud.
*   Cloud provider account and CLI tools if deploying to cloud.
```

package com.ludii.LudiiServer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthService {

    private final DatabaseService databaseService;
    private final Algorithm jwtAlgorithm;
    private final JWTVerifier jwtVerifier;
    private final String jwtIssuer = "user-auth-service";
    private final long jwtExpirationMs = 3600_000; // 1 hour

    // In-memory set to store invalidated tokens (for logout)
    // In a distributed environment, a distributed cache like Redis would be more appropriate.
    private final Set<String> invalidatedTokens = new HashSet<>();

    public AuthService(DatabaseService databaseService, String jwtSecret) {
        this.databaseService = databaseService;
        this.jwtAlgorithm = Algorithm.HMAC256(jwtSecret);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer(jwtIssuer)
                .build();
    }

    public User registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.err.println("Username and password cannot be empty.");
            return null; // Or throw an IllegalArgumentException
        }
        if (databaseService.findUserByUsername(username) != null) {
            System.err.println("Username already exists: " + username);
            return null; // Or throw a custom exception (e.g., UserAlreadyExistsException)
        }
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        return databaseService.createUser(username, passwordHash);
    }

    public String loginUser(String username, String password) {
        User user = databaseService.findUserByUsername(username);
        if (user == null) {
            System.err.println("Login failed: User not found - " + username);
            return null;
        }
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            return generateToken(user);
        }
        System.err.println("Login failed: Invalid password for user - " + username);
        return null;
    }

    private String generateToken(User user) {
        return JWT.create()
                .withIssuer(jwtIssuer)
                .withSubject(Integer.toString(user.getId()))
                .withClaim("username", user.getUsername())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .withJWTId(UUID.randomUUID().toString()) // Unique token ID
                .sign(jwtAlgorithm);
    }

    public boolean validateToken(String token) {
        if (token == null || invalidatedTokens.contains(token)) {
            return false;
        }
        try {
            jwtVerifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            System.err.println("JWT Verification failed: " + e.getMessage());
            return false;
        }
    }

    public DecodedJWT decodeToken(String token) {
         if (token == null || invalidatedTokens.contains(token)) {
            return null;
        }
        try {
            return jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            System.err.println("JWT Verification failed: " + e.getMessage());
            return null;
        }
    }


    public boolean logoutUser(String token) {
        if (token == null) {
            return false;
        }
        // Add token to the invalidated list.
        // In a real application, you might want to check if the token is valid first.
        // Also, consider the size of this set if many logouts occur.
        // A TTL for invalidated tokens could be useful.
        DecodedJWT decodedJWT = decodeToken(token);
        if(decodedJWT == null) {
            // Token is already invalid or malformed
            return false;
        }
        invalidatedTokens.add(token);
        return true;
    }

    public String getUsernameFromToken(String token) {
        DecodedJWT decodedJWT = decodeToken(token); // Use existing method that checks invalidatedTokens
        if (decodedJWT != null) {
            return decodedJWT.getClaim("username").asString();
        }
        return null;
    }

    public User decodeTokenAndGetUser(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            DecodedJWT decodedJWT = jwtVerifier.verify(token); // Already verified by validateToken, but good to decode again
            String userIdStr = decodedJWT.getSubject();
            String username = decodedJWT.getClaim("username").asString();
            if (userIdStr == null || username == null) {
                return null;
            }
            // We don't have the password hash here, but it's not needed for this context.
            // If we needed the full User object as stored in DB, we'd do a DB lookup by ID.
            // For associating with games, ID and username are usually sufficient from token.
            // However, to keep it consistent, let's fetch from DB.
            User user = databaseService.findUserByUsername(username);
            if (user != null && user.getId() == Integer.parseInt(userIdStr)) {
                return user;
            }
            return null;
        } catch (JWTVerificationException | NumberFormatException e) {
            System.err.println("Error decoding token or fetching user: " + e.getMessage());
            return null;
        }
    }

    // Helper method for testing token generation directly
    // Can be public or package-private if preferred for testing scope
    public String generateTokenForTest(User user) {
        return JWT.create()
                .withIssuer(jwtIssuer)
                .withSubject(Integer.toString(user.getId()))
                .withClaim("username", user.getUsername())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .withJWTId(UUID.randomUUID().toString()) // Unique token ID
                .sign(jwtAlgorithm);
    }
}

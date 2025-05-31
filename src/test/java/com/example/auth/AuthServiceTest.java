package com.example.auth;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock; // Keep @Mock for DatabaseService
import org.mockito.MockitoAnnotations;
// Remove @InjectMocks for authService, will be manually instantiated
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private DatabaseService mockDatabaseService;

    // @InjectMocks // REMOVE THIS ANNOTATION
    private AuthService authService; // Manually instantiated in setUp

    private final String testSecret = "test-jwt-secret-key-minimum-length-for-hs256";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this); // Initializes mockDatabaseService
        // Manually instantiate authService with the mock and the real testSecret
        authService = new AuthService(mockDatabaseService, testSecret);
    }

    @Test
    public void testRegisterUser_Success() {
        String username = "testuser";
        String password = "password123";
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(null);
        // Assume createUser returns a user object with an ID.
        // The actual password hash is generated inside authService.
        when(mockDatabaseService.createUser(eq(username), anyString()))
            .thenAnswer(invocation -> new User(1, username, invocation.getArgument(1)));

        User registeredUser = authService.registerUser(username, password);

        assertNotNull(registeredUser);
        assertEquals(username, registeredUser.getUsername());
        assertTrue(BCrypt.checkpw(password, registeredUser.getPasswordHash()));
        verify(mockDatabaseService).findUserByUsername(username);
        verify(mockDatabaseService).createUser(eq(username), anyString());
    }

    @Test
    public void testRegisterUser_UserAlreadyExists() {
        String username = "existinguser";
        String password = "password123";
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(new User(1, username, "somehash"));

        User registeredUser = authService.registerUser(username, password);

        assertNull(registeredUser);
        verify(mockDatabaseService).findUserByUsername(username);
        verify(mockDatabaseService, never()).createUser(anyString(), anyString());
    }

    @Test
    public void testRegisterUser_EmptyCredentials() {
        User user1 = authService.registerUser("", "password");
        assertNull(user1);
        User user2 = authService.registerUser("user", "");
        assertNull(user2);
        User user3 = authService.registerUser(null, "password");
        assertNull(user3);
        User user4 = authService.registerUser("user", null);
        assertNull(user4);
         verify(mockDatabaseService, never()).createUser(anyString(), anyString());
    }


    @Test
    public void testLoginUser_Success() {
        String username = "testuser";
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User mockUser = new User(1, username, hashedPassword);
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(mockUser);

        String token = authService.loginUser(username, password);

        assertNotNull(token);
        assertTrue(authService.validateToken(token)); // Validates token is structurally sound and not expired
        assertEquals(username, authService.getUsernameFromToken(token)); // Gets username from a valid token
        verify(mockDatabaseService).findUserByUsername(username);
    }

    @Test
    public void testLoginUser_UserNotFound() {
        String username = "nonexistentuser";
        String password = "password123";
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(null);

        String token = authService.loginUser(username, password);

        assertNull(token);
        verify(mockDatabaseService).findUserByUsername(username);
    }

    @Test
    public void testLoginUser_IncorrectPassword() {
        String username = "testuser";
        String correctPassword = "password123";
        String incorrectPassword = "wrongpassword";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());
        User mockUser = new User(1, username, hashedPassword);
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(mockUser);

        String token = authService.loginUser(username, incorrectPassword);

        assertNull(token);
        verify(mockDatabaseService).findUserByUsername(username);
    }

    @Test
    public void testValidateToken_ValidToken() {
        String username = "testuser";
        String password = "password123"; // Define a password to create a valid hash
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); // Create a real hash
        User mockUser = new User(1, username, hashedPassword);

        // When loginUser is called in this test (indirectly via direct token generation for simplicity),
        // it might involve findUserByUsername if we were testing loginUser itself.
        // For generateTokenForTest, we don't need a DB mock here as it directly uses the User object.
        // However, if getUsernameFromToken internally calls decodeTokenAndGetUser which hits DB, we need the mock.
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(mockUser);

        String token = authService.generateTokenForTest(mockUser); // Use helper for direct token generation

        assertNotNull("Token should not be null for validation test", token);
        assertTrue("Token should be valid", authService.validateToken(token));
        assertEquals("Username should be derivable from token", username, authService.getUsernameFromToken(token));
    }


    @Test
    public void testValidateToken_InvalidTokenSignature() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"; // Example token
        AuthService anotherAuthService = new AuthService(mockDatabaseService, "different-secret");
        assertFalse(anotherAuthService.validateToken(token)); // Should fail with original service too if signature is wrong
        assertFalse(authService.validateToken(token + "tamper")); // Tampered token
    }

    @Test
    public void testValidateToken_NullOrEmptyToken() {
        assertFalse(authService.validateToken(null));
        assertFalse(authService.validateToken(""));
    }

    @Test
    public void testLogoutUser_AndTokenInvalidation() {
        String username = "logoutuser";
        User mockUser = new User(2, username, BCrypt.hashpw("password", BCrypt.gensalt()));
        when(mockDatabaseService.findUserByUsername(username)).thenReturn(mockUser);

        String token = authService.generateTokenForTest(mockUser); // Using helper for direct token
        assertNotNull(token);
        assertTrue("Token should be valid before logout", authService.validateToken(token));

        assertTrue("Logout should succeed for a valid token", authService.logoutUser(token));
        assertFalse("Token should be invalid after logout", authService.validateToken(token));
        assertNull("Getting username from logged-out token should fail", authService.getUsernameFromToken(token));
        assertNull("Decoding logged-out token should fail", authService.decodeToken(token));

        // Test logout with already invalidated token
        assertFalse("Logout should fail for an already invalidated token", authService.logoutUser(token));
    }

    @Test
    public void testLogoutUser_NullToken() {
        assertFalse(authService.logoutUser(null));
    }

    @Test
    public void testDecodeTokenAndGetUser_Success() {
        String username = "testuser";
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User dbUser = new User(1, username, hashedPassword);

        when(mockDatabaseService.findUserByUsername(username)).thenReturn(dbUser);

        String token = authService.generateTokenForTest(dbUser); // Generate token using the user object
        assertNotNull(token);

        User decodedUser = authService.decodeTokenAndGetUser(token);
        assertNotNull(decodedUser);
        assertEquals(dbUser.getId(), decodedUser.getId());
        assertEquals(username, decodedUser.getUsername());
    }

    @Test
    public void testDecodeTokenAndGetUser_TokenTamperedOrInvalid() {
        String token = "invalid.tampered.token";
        User decodedUser = authService.decodeTokenAndGetUser(token);
        assertNull(decodedUser);
    }
}

// Add this helper method to AuthService.java for the testValidateToken_ValidToken and testLogoutUser tests to work smoothly
// // Helper method for testing token generation directly
//    public String generateTokenForTest(User user) {
//        return JWT.create()
//                .withIssuer(jwtIssuer)
//                .withSubject(Integer.toString(user.getId()))
//                .withClaim("username", user.getUsername())
//                .withIssuedAt(new Date())
//                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs))
//                .withJWTId(UUID.randomUUID().toString()) // Unique token ID
//                .sign(jwtAlgorithm);
//    }
//
// Or make the original generateToken method public/protected if that's acceptable.
// For now, I will assume such a helper or accessible method exists for focused testing.
// The test testLoginUser_Success() already covers token generation and validation implicitly.
// The specific test testValidateToken_ValidToken ensures validation in isolation.
// The test testLogoutUser_AndTokenInvalidation also uses this helper for a clean token.

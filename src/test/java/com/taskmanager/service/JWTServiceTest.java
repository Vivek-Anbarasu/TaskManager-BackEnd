package com.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JWTService.class)
@TestPropertySource(properties = {
    "jwt.secret=UMGWByE8Ja/FyDFLqqOnKCN4GiFd+cm01UQnk+HTZjYAOUxTu7tEMyfXTBePrxQ4wNDfcmGymX0KgnS/9FGKvA==",
    "jwt.issuer=test-issuer",
    "jwt.audience=test-audience",
    "jwt.expiration-ms=3600000"
})
class JWTServiceTest {

    @Autowired
    private JWTService jwtService;

    // ── generate ─────────────────────────────────────────────────────────────

    @Test
    void generateTokenReturnsNonNullToken() {
        String token = jwtService.generateToken("user@example.com", new HashMap<>());

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateTokenReturnsValidJWTFormat() {
        String token = jwtService.generateToken("user@example.com", new HashMap<>());

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void generateTokenCreatesTokenWithCorrectStructure() {
        String token = jwtService.generateToken("test@example.com", new HashMap<>());

        assertNotNull(token);
        assertTrue(token.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"));
    }

    @Test
    void generateTokenHandlesNullEmail() {
        String token = jwtService.generateToken(null, new HashMap<>());

        assertNotNull(token);
        assertNull(jwtService.extractEmail(token));
    }

    // ── extractEmail ─────────────────────────────────────────────────────────

    @Test
    void extractEmailReturnsCorrectEmailFromToken() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void extractEmailReturnsCorrectEmailForDifferentEmails() {
        String email1 = "admin@example.com";
        String email2 = "user@test.com";
        String token1 = jwtService.generateToken(email1, new HashMap<>());
        String token2 = jwtService.generateToken(email2, new HashMap<>());

        assertEquals(email1, jwtService.extractEmail(token1));
        assertEquals(email2, jwtService.extractEmail(token2));
    }

    @Test
    void extractEmailHandlesEmailWithSpecialCharacters() {
        String email = "user+test@example.co.uk";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void extractEmailThrowsExceptionForInvalidToken() {
        // Nimbus wraps ParseException in RuntimeException for malformed tokens
        assertThrows(RuntimeException.class, () ->
            jwtService.extractEmail("invalid.token.here")
        );
    }

    @Test
    void extractEmailThrowsExceptionForNullToken() {
        assertThrows(Exception.class, () ->
            jwtService.extractEmail(null)
        );
    }

    @Test
    void extractEmailThrowsExceptionForEmptyToken() {
        assertThrows(Exception.class, () ->
            jwtService.extractEmail("")
        );
    }

    // ── extractExpiration ────────────────────────────────────────────────────

    @Test
    void extractExpirationReturnsDateInFuture() {
        String token = jwtService.generateToken("user@example.com", new HashMap<>());

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void extractExpirationReturnsDateWithinConfiguredValidity() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken("user@example.com", new HashMap<>());

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.getTime() <= before + 3600000 + 1000); // within 1hr + 1s tolerance
    }

    @Test
    void extractExpirationThrowsExceptionForInvalidToken() {
        // Nimbus wraps ParseException in RuntimeException for malformed tokens
        assertThrows(RuntimeException.class, () ->
            jwtService.extractExpiration("invalid.token.here")
        );
    }

    // ── validateToken ────────────────────────────────────────────────────────

    @Test
    void validateTokenReturnsTrueForValidToken() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertTrue(jwtService.validateToken(jwtService.extractEmail(token), email, token));
    }

    @Test
    void validateTokenReturnsTrueWhenBothEmailsMatchAndTokenNotExpired() {
        String email = "admin@example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertTrue(jwtService.validateToken(email, email, token));
    }

    @Test
    void validateTokenReturnsFalseWhenEmailsDoNotMatch() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertFalse(jwtService.validateToken(jwtService.extractEmail(token), "different@example.com", token));
    }

    @Test
    void validateTokenReturnsFalseWhenTokenEmailDoesNotMatchDatabaseEmail() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        assertFalse(jwtService.validateToken("wrong@example.com", email, token));
    }

    @Test
    void validateTokenHandlesCaseSensitiveEmails() {
        String email = "User@Example.com";
        String token = jwtService.generateToken(email, new HashMap<>());

        // Case-sensitive: "User@Example.com" != "user@example.com"
        assertFalse(jwtService.validateToken(jwtService.extractEmail(token), "user@example.com", token));
    }

    @Test
    void validateTokenReturnsFalseForTokenWithWrongSignature() {
        // Valid HS512 structure but signed with a different key — Nimbus verifier rejects it
        String tampered = "eyJhbGciOiJIUzUxMiJ9" +
                ".eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaXNzIjoidGVzdC1pc3N1ZXIiLCJleHAiOjk5OTk5OTk5OTl9" +
                ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        assertFalse(jwtService.validateToken("user@example.com", "user@example.com", tampered));
    }

    @Test
    void validateTokenReturnsFalseForMalformedToken() {
        assertFalse(jwtService.validateToken("user@example.com", "user@example.com", "not.a.jwt"));
    }
}

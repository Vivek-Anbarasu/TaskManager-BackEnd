package com.restapp.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JWTService.class)
@TestPropertySource(properties = {
    "jwt.secret=UMGWByE8Ja/FyDFLqqOnKCN4GiFd+cm01UQnk+HTZjYAOUxTu7tEMyfXTBePrxQ4wNDfcmGymX0KgnS/9FGKvA=="
})
class JWTServiceTest {

    @Autowired
    private JWTService jwtService;


    @Test
    void generateTokenReturnsNonNullToken() {
        String token = jwtService.generateToken("user@example.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateTokenReturnsValidJWTFormat() {
        String token = jwtService.generateToken("user@example.com");

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }



    @Test
    void extractEmailReturnsCorrectEmailFromToken() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void extractEmailReturnsCorrectEmailForDifferentEmails() {
        String email1 = "admin@example.com";
        String email2 = "user@test.com";
        String token1 = jwtService.generateToken(email1);
        String token2 = jwtService.generateToken(email2);

        assertEquals(email1, jwtService.extractEmail(token1));
        assertEquals(email2, jwtService.extractEmail(token2));
    }

    @Test
    void extractEmailHandlesEmailWithSpecialCharacters() {
        String email = "user+test@example.co.uk";
        String token = jwtService.generateToken(email);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void extractExpirationReturnsDateInFuture() {
        String token = jwtService.generateToken("user@example.com");

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }



    @Test
    void extractClaimExtractsSubjectClaim() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);

        String subject = jwtService.extractClaim(token, Claims::getSubject);

        assertEquals(email, subject);
    }

    @Test
    void extractClaimExtractsIssuedAtClaim() {
        String token = jwtService.generateToken("user@example.com");

        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date(System.currentTimeMillis() + 1000)));
    }

    @Test
    void validateTokenReturnsTrueForValidToken() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        String tokenEmail = jwtService.extractEmail(token);

        Boolean isValid = jwtService.validateToken(tokenEmail, email, token);

        assertTrue(isValid);
    }

    @Test
    void validateTokenReturnsFalseWhenEmailsDoNotMatch() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        String tokenEmail = jwtService.extractEmail(token);

        Boolean isValid = jwtService.validateToken(tokenEmail, "different@example.com", token);

        assertFalse(isValid);
    }

    @Test
    void validateTokenReturnsFalseWhenTokenEmailDoesNotMatchDatabaseEmail() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);

        Boolean isValid = jwtService.validateToken("wrong@example.com", email, token);

        assertFalse(isValid);
    }

    @Test
    void validateTokenReturnsTrueWhenBothEmailsMatchAndTokenNotExpired() {
        String email = "admin@example.com";
        String token = jwtService.generateToken(email);

        Boolean isValid = jwtService.validateToken(email, email, token);

        assertTrue(isValid);
    }

    @Test
    void extractEmailThrowsExceptionForInvalidToken() {
        assertThrows(MalformedJwtException.class, () ->
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

    @Test
    void extractExpirationThrowsExceptionForInvalidToken() {
        assertThrows(MalformedJwtException.class, () ->
            jwtService.extractExpiration("invalid.token.here")
        );
    }

    @Test
    void generateTokenHandlesNullEmail() {
        String token = jwtService.generateToken(null);

        assertNotNull(token);
        assertNull(jwtService.extractEmail(token));
    }


    @Test
    void extractEmailThrowsExceptionForTokenWithWrongSignature() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        assertThrows(SignatureException.class, () ->
            jwtService.extractEmail(token)
        );
    }

    @Test
    void validateTokenHandlesCaseSensitiveEmails() {
        String email = "User@Example.com";
        String token = jwtService.generateToken(email);
        String tokenEmail = jwtService.extractEmail(token);

        Boolean isValid = jwtService.validateToken(tokenEmail, "user@example.com", token);

        assertFalse(isValid);
    }

    @Test
    void generateTokenCreatesTokenWithCorrectStructure() {
        String email = "test@example.com";
        String token = jwtService.generateToken(email);

        assertNotNull(token);
        assertTrue(token.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"));
    }
}


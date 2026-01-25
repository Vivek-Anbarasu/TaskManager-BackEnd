package com.taskmanager.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JWTService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.expiration-ms}")
    private Long tokenValidity;

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claimsResolver.apply(claims);
    }

    public Boolean validateToken(String tokenEmail, String databaseEmail, String jwtToken) {
        // Parse the token once to get both header and claims
        var parsedToken = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(jwtToken);

        // Verify the algorithm in the token header is HS512
        String algorithm = parsedToken.getHeader().getAlgorithm();
        if (algorithm == null || !algorithm.equals("HS512")) {
            log.warn("Invalid algorithm in JWT token. Expected HS512, but got: {}", algorithm);
            return false;
        }

        // Verify the issuer matches the expected issuer
        String tokenIssuer = parsedToken.getPayload().getIssuer();
        if (tokenIssuer == null || !tokenIssuer.equals(issuer)) {
            log.warn("Invalid issuer in JWT token. Expected: {}, but got: {}", issuer, tokenIssuer);
            return false;
        }

        return (tokenEmail.equals(databaseEmail) && !extractExpiration(jwtToken).before(new Date()));
    }

    public String generateToken(String email, Map<String,String> claims){
        log.debug("Generating JWT token for email={}", email);
        return Jwts.builder().
                issuer(issuer).setAudience(audience).subject(email).claims(claims).
                issuedAt(new Date(System.currentTimeMillis())).expiration(new Date(System.currentTimeMillis()+tokenValidity))
                .signWith(getSigningKey()).compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

package com.restapp.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JWTService {

    @Value("${jwt.secret}")
    private String SECRET;

	private static final long TOKEN_VALIDITY = 1000*60*30; // 30 minutes

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
        return (tokenEmail.equals(databaseEmail) && !extractExpiration(jwtToken).before(new Date()));
    }

    public String generateToken(String email){
        Map<String,Object> claims = new HashMap<>();
        log.debug("Generating JWT token for email={}", email);
        return Jwts.builder().claims(claims).subject(email).issuedAt(new Date(System.currentTimeMillis())).expiration(new Date(System.currentTimeMillis()+TOKEN_VALIDITY))
                .signWith(getSigningKey()).compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

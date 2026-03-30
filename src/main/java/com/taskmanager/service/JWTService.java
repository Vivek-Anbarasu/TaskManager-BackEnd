package com.taskmanager.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JWTService {

    private static final String HMAC_SHA512 = "HmacSHA512";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.expiration-ms}")
    private Long tokenValidity;

    private SecretKey signingKey;
    private MACSigner signer;
    private MACVerifier verifier;

    @PostConstruct
    private void init() throws JOSEException {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = new SecretKeySpec(keyBytes, HMAC_SHA512);
        this.signer   = new MACSigner(signingKey);
        this.verifier = new MACVerifier(signingKey);
        log.debug("JWT signing key initialized");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private JWTClaimsSet parseClaims(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet();
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    // ── extractors ───────────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).getClaim("role");
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpirationTime();
    }

    // ── validate ─────────────────────────────────────────────────────────────

    public Boolean validateToken(String tokenEmail, String databaseEmail, String jwtToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);

            // Verify HMAC signature
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return false;
            }

            // Enforce HS512 algorithm
            String algorithm = signedJWT.getHeader().getAlgorithm().getName();
            if (!JWSAlgorithm.HS512.getName().equals(algorithm)) {
                log.warn("Invalid algorithm in JWT token. Expected HS512, but got: {}", algorithm);
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Verify issuer
            if (!issuer.equals(claims.getIssuer())) {
                log.warn("Invalid issuer in JWT token. Expected: {}, but got: {}", issuer, claims.getIssuer());
                return false;
            }

            Date expiration = claims.getExpirationTime();
            return tokenEmail.equals(databaseEmail) && expiration != null && !expiration.before(new Date());

        } catch (ParseException | JOSEException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    // ── generate ─────────────────────────────────────────────────────────────

    public String generateToken(String email, Map<String, String> claims) {
        log.debug("Generating JWT token for email={}", email);
        try {
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject(email)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + tokenValidity));

            claims.forEach(claimsBuilder::claim);

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claimsBuilder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    // ── secret generator (utility) ───────────────────────────────────────────

    /**
     * Generates a cryptographically secure Base64-encoded HmacSHA512 secret.
     * Run once to produce a value for the {@code jwt.secret} property.
     */
    public static String generateSecret() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(HMAC_SHA512);
        keyGen.init(512);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}

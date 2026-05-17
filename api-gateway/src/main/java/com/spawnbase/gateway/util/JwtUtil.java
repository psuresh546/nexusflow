package com.spawnbase.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT validation utility.
 *
 * Validates:
 * 1. Signature — was this token signed with our secret?
 * 2. Expiration — has the token expired?
 * 3. Claims — does it contain required fields?
 *
 * Does NOT issue tokens — that's an auth service's job.
 * This gateway only validates and enforces.
 *
 * Algorithm: HMAC-SHA256 (HS256)
 * Industry standard for symmetric JWT signing.
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(
            @Value("${jwt.secret}") String secret) {
        // Key must be at least 256 bits for HS256
        byte[] keyBytes = secret.getBytes(
                StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate a JWT token string.
     * Returns true if valid, false otherwise.
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT validation error: {}",
                    e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from a valid token.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the subject (user ID) from a token.
     */
    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract roles from a token.
     * Roles are stored as a list under the "roles" claim.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = getClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Generate a token for testing purposes.
     * In production, tokens are issued by an auth service.
     */
    public String generateToken(
            String subject,
            List<String> roles,
            long expirationMs) {

        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis()
                                + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
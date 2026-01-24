package com.travolish.traveller.auth.util;

import com.travolish.traveller.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token generation and validation
 * Specifically designed for OAuth2 authentication flows
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:travolish-secret-key-for-oauth2-authentication-tokens-please-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpirationMs;

    @Value("${jwt.refresh.expiration:604800000}")
    private Long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token for authenticated user
     */
    public String generateToken(User user) {
        log.debug("Generating JWT token for user: {}", user.getId());
        return Jwts.builder()
                .setClaims(buildTokenClaims(user))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate refresh token for authenticated user
     */
    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getId());
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .claim("provider", user.getProvider())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate token with custom expiration
     */
    public String generateTokenWithExpiration(User user, Long expirationMs) {
        log.debug("Generating JWT token with custom expiration for user: {}", user.getId());
        return Jwts.builder()
                .setClaims(buildTokenClaims(user))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract claims from token
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token", e);
            throw new RuntimeException("Invalid token: " + e.getMessage(), e);
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Get expiration from token
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getExpiration();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return getExpirationFromToken(token).before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration", e);
            return true;
        }
    }

    /**
     * Build claims map for JWT token
     */
    private Map<String, Object> buildTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("type", "access");
        claims.put("provider", user.getProvider());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        
        return claims;
    }
}

package com.dyrnq.distops.registry.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Issues and verifies OAuth-style refresh tokens.
 *
 * <p>Previously the refresh token was just {@code base64(username:expiry:refresh)},
 * which any caller could forge for any user. Refresh tokens are now signed JWTs
 * keyed off the application's main JWT secret (see {@code jwt.secret}); the
 * signing key is derived with SHA-256 so the existing app secret can be used
 * directly without length concerns.
 *
 * <h3>Key derivation</h3>
 * Refresh tokens and API access tokens (issued by {@code JwtUtils}) share the
 * same {@code jwt.secret} configuration value but derive different HMAC keys:
 * <ul>
 *   <li>Access tokens: {@code Keys.hmacShaKeyFor(Base64.decode(secret))}</li>
 *   <li>Refresh tokens: {@code Keys.hmacShaKeyFor(SHA-256(secret))}</li>
 * </ul>
 * This is <strong>intentional</strong>: it prevents token-type confusion.
 * A refresh token cannot be used as an API access token, and vice versa.
 */
public final class RefreshTokenService {

    private final SecretKey signingKey;

    public RefreshTokenService(String appJwtSecret) {
        if (appJwtSecret == null || appJwtSecret.isEmpty()) {
            throw new IllegalStateException(
                    "jwt.secret must be configured before issuing refresh tokens; refusing to fall back to an unsigned token");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(appJwtSecret.getBytes(StandardCharsets.UTF_8));
            this.signingKey = Keys.hmacShaKeyFor(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Sign a refresh token. The token's {@code sub} claim is the username and
     * {@code exp} is the absolute expiration epoch in seconds.
     */
    public String issue(String username, long expiresEpochSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(expiresEpochSeconds * 1000L))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verify a refresh token's signature and expiration. Returns the username
     * ({@code sub}) on success, or null if the token is invalid for any reason
     * (bad signature, malformed, expired, etc.).
     */
    public String verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}

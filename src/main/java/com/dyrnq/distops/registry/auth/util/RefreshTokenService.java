package com.dyrnq.distops.registry.auth.util;

import com.dyrnq.distops.dso.RefreshTokenRevocationMapper;
import com.dyrnq.distops.model.RefreshTokenRevocation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;
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
        return issue(username, expiresEpochSeconds, 1L);
    }

    public String issue(String username, long expiresEpochSeconds, Long instId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("inst_id", instId != null ? instId : 1L)
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
    // Guard against null: in fail-safe no-DI scenarios, verify without revocation.
    // Callers should inject the mapper when available.
    private RefreshTokenRevocationMapper revocationMapper;

    public void setRevocationMapper(RefreshTokenRevocationMapper mapper) {
        this.revocationMapper = mapper;
    }

    public String verify(String token) {
        return verify(token, null);
    }

    /**
     * Verify a refresh token with optional JTI revocation check.
     * When {@code revocationMapper} is available, tokens whose JTI is in the
     * blacklist are rejected. This allows an administrator or password-change
     * flow to invalidate all outstanding refresh tokens for a user.
     */
    public String verify(String token, RefreshTokenRevocationMapper mapper) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // Check JTI blacklist (per-inst_id)
            RefreshTokenRevocationMapper rmap = mapper != null ? mapper : this.revocationMapper;
            if (rmap != null) {
                String jti = claims.getId();
                if (jti != null && !jti.isEmpty()) {
                    Long instId = claims.get("inst_id", Long.class);
                    if (instId == null) instId = 1L;
                    if (rmap.existsByJtiAndInstId(jti, instId)) {
                        return null; // explicitly revoked
                    }
                }
            }
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Revoke all refresh tokens for a user by writing their outstanding
     * tokens to the blacklist. Callers should invoke this after a password
     * reset, account disable, or explicit logout.
     */
    public void revokeAllForUser(String username, Long instId) {
        if (revocationMapper == null || username == null) return;
        Date now = new Date();
        RefreshTokenRevocation bulk = new RefreshTokenRevocation();
        bulk.setJti("revoke-all-" + username + "-" + System.currentTimeMillis());
        bulk.setInstId(instId != null ? instId : 1L);
        bulk.setUsername(username);
        bulk.setRevokedAt(now);
        bulk.setExpiresAt(new Date(System.currentTimeMillis() + 86400L * 30 * 1000));
        revocationMapper.insert(bulk, false);
    }

    public void revokeAllForUser(String username) {
        revokeAllForUser(username, 1L);
    }
}

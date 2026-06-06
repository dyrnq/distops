package com.dyrnq.distops.registry.auth.util;

import com.dyrnq.distops.dso.RefreshTokenRevocationMapper;
import com.dyrnq.distops.model.RefreshTokenRevocation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

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
@Component
@Slf4j
public class RefreshTokenService {

    @Inject("secretKey")
    private SecretKey signingKey;

    @Inject
    private RefreshTokenRevocationMapper revocationMapper;

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

            String jti = claims.getId();
            String username = claims.getSubject();
            Long instId = claims.get("inst_id", Long.class);
            if (instId == null) instId = 1L;
            // Exact JTI match (individual token revocation)
            if (jti != null && !jti.isEmpty() && revocationMapper.existsByJtiAndInstId(jti, instId)) {
                return null;
            }
            // Bulk revocation for user (e.g. account disable)
            if (username != null && revocationMapper.existsByUsernameAndInstId(username, instId)) {
                log.warn("Refresh token rejected by bulk revocation: user={}", username);
                return null;
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

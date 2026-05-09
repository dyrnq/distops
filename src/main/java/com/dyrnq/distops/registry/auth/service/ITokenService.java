package com.dyrnq.distops.registry.auth.service;

import com.dyrnq.distops.registry.auth.model.JWTPayload;

import java.util.List;

/**
 * Token service interface for JWT token generation
 */
public interface ITokenService {

    /**
     * Get the key type supported by this service
     * @return Key type (EC, RSA, HMAC)
     */
    String getKeyType();

    /**
     * Get the algorithm supported by this service
     * @return Algorithm name (ES256, RS256, HS256, etc.)
     */
    String getAlgorithm();

    /**
     * Create a signed JWT token
     * @param subject Token subject
     * @param audience Token audience
     * @param accessList Access control list
     * @return Signed JWT token
     */
    String createToken(String subject, String audience, List<JWTPayload.ResourceAccess> accessList);


}

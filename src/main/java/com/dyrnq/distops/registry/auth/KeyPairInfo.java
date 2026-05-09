package com.dyrnq.distops.registry.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key pair information for token authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPairInfo {

    /**
     * Key type: EC, RSA or oct (HMAC)
     */
    private String keyType;

    /**
     * Key algorithm: ES256, ES384, ES512, RS256, RS384, RS512, HS256, HS384, HS512
     */
    private String algorithm;

    /**
     * Private key in PEM format (or Base64 secret for HMAC)
     */
    private String privateKeyPem;

    /**
     * Public key in PEM format (or Base64 secret for HMAC)
     */
    private String publicKeyPem;

    /**
     * JWKS JSON content
     */
    private String jwksJson;

    /**
     * Key ID (kid) for JWT header
     */
    private String kid;
}

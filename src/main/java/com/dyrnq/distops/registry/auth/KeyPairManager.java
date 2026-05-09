package com.dyrnq.distops.registry.auth;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * Key pair management service for token authentication
 * Provides unified interface for generating and managing EC, RSA and HMAC key pairs
 */
@Slf4j
@Component
public class KeyPairManager {

    @Inject
    private ECKeyPairGenerator ecKeyPairGenerator;

    @Inject
    private RSAKeyPairGenerator rsaKeyPairGenerator;
    
    @Inject
    private HMACKeyGenerator hmacKeyGenerator;

    private final Map<String, KeyPairGeneratorService> generators = new HashMap<>();

    public KeyPairManager() {
        // Register generators
        // Will be populated by Solon injection
    }

    /**
     * Generate a new key pair for the specified algorithm
     * @param algorithm Key algorithm (ES256, ES384, ES512, RS256, RS384, RS512, PS256, PS384, PS512, HS256, HS384, HS512)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateKeyPair(String algorithm) {
        // Handle HMAC algorithms
        if (algorithm.startsWith("HS")) {
            return generateHMACKeyPair(algorithm);
        }
        
        // Handle RSA algorithms (including RSA-PSS)
        if (algorithm.startsWith("RS") || algorithm.startsWith("PS")) {
            return generateRSAKeyPair(algorithm);
        }
        
        // Handle EC algorithms
        if (algorithm.startsWith("ES")) {
            return generateECKeyPair(algorithm);
        }
        
        throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
    }

    /**
     * Generate a new HMAC key pair (secret key for HMAC)
     * @param algorithm HMAC algorithm (HS256, HS384, HS512)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateHMACKeyPair(String algorithm) {
        log.info("Generating HMAC key pair with algorithm {}", algorithm);

        String secretKey = HMACKeyGenerator.generateSecretKey(algorithm);
        String kid = "hmac-" + algorithm.toLowerCase() + "-" + System.currentTimeMillis();

        // For HMAC, private and public keys are the same (symmetric)
        KeyPairInfo keyPairInfo = new KeyPairInfo();
        keyPairInfo.setKeyType("HMAC"); // Use HMAC as key type for database
        keyPairInfo.setAlgorithm(algorithm);
        keyPairInfo.setPrivateKeyPem(secretKey);
        keyPairInfo.setPublicKeyPem(secretKey);
        keyPairInfo.setKid(kid);

        // Generate JWKS for HMAC
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "oct");
        jwk.put("kid", kid);
        jwk.put("alg", algorithm);
        jwk.put("use", "sig");
        jwk.put("k", secretKey); // HMAC uses 'k' for shared secret

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", new Object[] { jwk });

        keyPairInfo.setJwksJson(serializeJWKS(jwks));

        log.info("Generated HMAC key pair with kid: {}", kid);

        return keyPairInfo;
    }
    
    /**
     * Serialize JWKS to JSON string
     */
    private String serializeJWKS(Map<String, Object> jwks) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(jwks);
        } catch (Exception e) {
            log.error("Failed to serialize JWKS", e);
            return "{}";
        }
    }
    
    /**
     * Generate a new EC key pair with default algorithm (ES256)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateECKeyPair() {
        return ecKeyPairGenerator.generateKeyPair();
    }
    
    /**
     * Generate a new EC key pair with specified algorithm
     * @param algorithm EC algorithm (ES256, ES384, ES512)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateECKeyPair(String algorithm) {
        return ecKeyPairGenerator.generateKeyPair(algorithm);
    }
    
    /**
     * Generate a new RSA key pair with default algorithm (RS256)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateRSAKeyPair() {
        return rsaKeyPairGenerator.generateKeyPair();
    }
    
    /**
     * Generate a new RSA key pair with specified algorithm
     * @param algorithm RSA algorithm (RS256, RS384, RS512)
     * @return KeyPairInfo containing keys and JWKS
     */
    public KeyPairInfo generateRSAKeyPair(String algorithm) {
        return rsaKeyPairGenerator.generateKeyPair(algorithm);
    }
    
    /**
     * Get supported algorithms
     * @return Map of key type to array of supported algorithms
     */
    public Map<String, String[]> getSupportedAlgorithms() {
        Map<String, String[]> supported = new HashMap<>();
        supported.put("EC", ecKeyPairGenerator.getSupportedAlgorithms());
        supported.put("RSA", rsaKeyPairGenerator.getSupportedAlgorithms());
        return supported;
    }
    
    /**
     * Get generator for the specified algorithm
     */
    private KeyPairGeneratorService getGeneratorForAlgorithm(String algorithm) {
        if (algorithm == null) {
            return null;
        }
        
        if (algorithm.startsWith("ES")) {
            return ecKeyPairGenerator;
        } else if (algorithm.startsWith("RS")) {
            return rsaKeyPairGenerator;
        }
        
        return null;
    }
}

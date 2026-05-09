package com.dyrnq.distops.registry.auth;

/**
 * Key pair generator interface for token authentication
 * Supports both EC and RSA key types
 */
public interface KeyPairGeneratorService {
    
    /**
     * Generate a new key pair
     * @return KeyPairInfo containing private key, public key, and JWKS
     */
    KeyPairInfo generateKeyPair();
    
    /**
     * Generate a new key pair with specified algorithm
     * @param algorithm Key algorithm (e.g., ES256, RS256)
     * @return KeyPairInfo containing private key, public key, and JWKS
     */
    KeyPairInfo generateKeyPair(String algorithm);
    
    /**
     * Get the key type (EC or RSA)
     * @return Key type
     */
    String getKeyType();
    
    /**
     * Get supported algorithms
     * @return Array of supported algorithm names
     */
    String[] getSupportedAlgorithms();
}

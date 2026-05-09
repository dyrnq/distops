package com.dyrnq.distops.registry.auth;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA key pair generator for JWT token signing
 * Supports RS256, RS384, RS512, PS256, PS384, PS512 algorithms
 */
@Slf4j
@Component
public class RSAKeyPairGenerator implements KeyPairGeneratorService {

    private static final String KEY_TYPE_RSA = "RSA";
    private static final String ALG_RS256 = "RS256";
    private static final String ALG_RS384 = "RS384";
    private static final String ALG_RS512 = "RS512";
    private static final String ALG_PS256 = "PS256";
    private static final String ALG_PS384 = "PS384";
    private static final String ALG_PS512 = "PS512";
    
    private static final int KEY_SIZE_2048 = 2048;
    private static final int KEY_SIZE_3072 = 3072;
    private static final int KEY_SIZE_4096 = 4096;
    
    @Override
    public KeyPairInfo generateKeyPair() {
        return generateKeyPair(ALG_RS256);
    }
    
    @Override
    public KeyPairInfo generateKeyPair(String algorithm) {
        try {
            int keySize = getKeySize(algorithm);
            
            // Generate RSA key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_TYPE_RSA);
            keyPairGenerator.initialize(new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4));
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // Get keys
            byte[] privateKeyEncoded = keyPair.getPrivate().getEncoded();
            byte[] publicKeyEncoded = keyPair.getPublic().getEncoded();
            
            // Convert to PEM format
            String privateKeyPem = encodePem("PRIVATE KEY", privateKeyEncoded);
            String publicKeyPem = encodePem("PUBLIC KEY", publicKeyEncoded);
            
            // Generate JWKS
            String jwksJson = generateJwks(keyPair.getPublic(), algorithm);
            
            // Generate kid from public key
            String kid = generateKid(keyPair.getPublic());
            
            return KeyPairInfo.builder()
                    .keyType(KEY_TYPE_RSA)
                    .algorithm(algorithm)
                    .privateKeyPem(privateKeyPem)
                    .publicKeyPem(publicKeyPem)
                    .jwksJson(jwksJson)
                    .kid(kid)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
    
    @Override
    public String getKeyType() {
        return KEY_TYPE_RSA;
    }
    
    @Override
    public String[] getSupportedAlgorithms() {
        return new String[]{ALG_RS256, ALG_RS384, ALG_RS512, ALG_PS256, ALG_PS384, ALG_PS512};
    }
    
    /**
     * Get key size for the specified algorithm
     */
    private int getKeySize(String algorithm) {
        // PS256/384/512 use same key sizes as RS256/384/512
        switch (algorithm) {
            case ALG_RS256:
            case ALG_PS256:
                return KEY_SIZE_2048;
            case ALG_RS384:
            case ALG_PS384:
                return KEY_SIZE_3072;
            case ALG_RS512:
            case ALG_PS512:
                return KEY_SIZE_4096;
            default:
                return KEY_SIZE_2048;
        }
    }
    
    /**
     * Encode key bytes to PEM format
     */
    private String encodePem(String type, byte[] keyBytes) {
        String base64 = Base64.getEncoder().encodeToString(keyBytes);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");
        
        // Split into 64-character lines
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            pem.append(base64, i, end).append("\n");
        }
        
        pem.append("-----END ").append(type).append("-----");
        return pem.toString();
    }
    
    /**
     * Generate JWKS from public key
     */
    private String generateJwks(PublicKey publicKey, String algorithm) throws Exception {
        java.security.interfaces.RSAPublicKey rsaPublicKey = 
            (java.security.interfaces.RSAPublicKey) publicKey;
        
        // Extract modulus (n) and exponent (e)
        byte[] nBytes = rsaPublicKey.getModulus().toByteArray();
        byte[] eBytes = rsaPublicKey.getPublicExponent().toByteArray();
        
        // Remove leading zero if present
        if (nBytes[0] == 0) {
            byte[] temp = new byte[nBytes.length - 1];
            System.arraycopy(nBytes, 1, temp, 0, temp.length);
            nBytes = temp;
        }
        if (eBytes[0] == 0) {
            byte[] temp = new byte[eBytes.length - 1];
            System.arraycopy(eBytes, 1, temp, 0, temp.length);
            eBytes = temp;
        }
        
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(eBytes);
        
        // Generate kid
        String kid = generateKid(publicKey);
        
        // Build JWKS
        Map<String, Object> key = new HashMap<>();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", algorithm);
        key.put("kid", kid);
        key.put("n", n);
        key.put("e", e);

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", new Object[]{key});

        // Return compact JSON (no pretty printing to avoid newlines)
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(jwks);
    }
    
    /**
     * Generate Key ID (kid) from public key using JWK thumbprint (RFC 7638)
     */
    private String generateKid(PublicKey publicKey) throws Exception {
        java.security.interfaces.RSAPublicKey rsaPublicKey = 
            (java.security.interfaces.RSAPublicKey) publicKey;
        
        // Extract modulus and exponent
        byte[] nBytes = rsaPublicKey.getModulus().toByteArray();
        byte[] eBytes = rsaPublicKey.getPublicExponent().toByteArray();
        
        // Remove leading zero
        if (nBytes[0] == 0) {
            byte[] temp = new byte[nBytes.length - 1];
            System.arraycopy(nBytes, 1, temp, 0, temp.length);
            nBytes = temp;
        }
        if (eBytes[0] == 0) {
            byte[] temp = new byte[eBytes.length - 1];
            System.arraycopy(eBytes, 1, temp, 0, temp.length);
            eBytes = temp;
        }
        
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(eBytes);
        
        // Create canonical JWK for thumbprint
        String jwkCanonical = String.format(
            "{\"e\":\"%s\",\"kty\":\"RSA\",\"n\":\"%s\"}",
            e, n
        );
        
        // Calculate SHA-256 hash
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] thumbprint = sha256.digest(jwkCanonical.getBytes("UTF-8"));
        
        // Base64URL encode and truncate to 43 characters
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(thumbprint)
                .substring(0, Math.min(43, Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(thumbprint).length()));
    }
}

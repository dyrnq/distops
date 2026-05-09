package com.dyrnq.distops.registry.auth;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * EC (Elliptic Curve) key pair generator for JWT token signing
 * Supports ES256, ES384, ES512 algorithms
 */
@Slf4j
@Component
public class ECKeyPairGenerator implements KeyPairGeneratorService {
    
    private static final String KEY_TYPE_EC = "EC";
    private static final String ALG_ES256 = "ES256";
    private static final String ALG_ES384 = "ES384";
    private static final String ALG_ES512 = "ES512";
    
    @Override
    public KeyPairInfo generateKeyPair() {
        return generateKeyPair(ALG_ES256);
    }
    
    @Override
    public KeyPairInfo generateKeyPair(String algorithm) {
        try {
            String curveName = getCurveName(algorithm);
            
            // Generate EC key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_TYPE_EC);
            keyPairGenerator.initialize(new ECGenParameterSpec(curveName));
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
                    .keyType(KEY_TYPE_EC)
                    .algorithm(algorithm)
                    .privateKeyPem(privateKeyPem)
                    .publicKeyPem(publicKeyPem)
                    .jwksJson(jwksJson)
                    .kid(kid)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to generate EC key pair", e);
            throw new RuntimeException("Failed to generate EC key pair", e);
        }
    }
    
    @Override
    public String getKeyType() {
        return KEY_TYPE_EC;
    }
    
    @Override
    public String[] getSupportedAlgorithms() {
        return new String[]{ALG_ES256, ALG_ES384, ALG_ES512};
    }
    
    /**
     * Get curve name for the specified algorithm
     */
    private String getCurveName(String algorithm) {
        switch (algorithm) {
            case ALG_ES256:
                return "secp256r1"; // P-256
            case ALG_ES384:
                return "secp384r1"; // P-384
            case ALG_ES512:
                return "secp521r1"; // P-521
            default:
                return "secp256r1";
        }
    }

    /**
     * Get expected coordinate size in bytes for the algorithm
     */
    private int getCoordinateSize(String algorithm) {
        switch (algorithm) {
            case ALG_ES256:
                return 32; // P-256: 32 bytes
            case ALG_ES384:
                return 48; // P-384: 48 bytes
            case ALG_ES512:
                return 66; // P-521: 66 bytes (521 bits = 65.125 bytes, rounded up to 66)
            default:
                return 32;
        }
    }
    
    /**
     * Get JWKS curve name from Java curve name
     */
    private String getJwksCurveName(String javaCurveName) {
        switch (javaCurveName) {
            case "secp256r1":
                return "P-256";
            case "secp384r1":
                return "P-384";
            case "secp521r1":
                return "P-521";
            default:
                return "P-256";
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
        java.security.interfaces.ECPublicKey ecPublicKey =
            (java.security.interfaces.ECPublicKey) publicKey;

        // Extract x and y coordinates
        byte[] xBytes = ecPublicKey.getW().getAffineX().toByteArray();
        byte[] yBytes = ecPublicKey.getW().getAffineY().toByteArray();

        // Get expected coordinate size for the curve
        int expectedSize = getCoordinateSize(algorithm);
        
        // Ensure coordinates are the correct size (remove leading zero if needed, then pad)
        if (xBytes[0] == 0 && xBytes.length > expectedSize) {
            byte[] temp = new byte[xBytes.length - 1];
            System.arraycopy(xBytes, 1, temp, 0, temp.length);
            xBytes = temp;
        }
        if (yBytes[0] == 0 && yBytes.length > expectedSize) {
            byte[] temp = new byte[yBytes.length - 1];
            System.arraycopy(yBytes, 1, temp, 0, temp.length);
            yBytes = temp;
        }
        
        // Pad to expected size if necessary
        if (xBytes.length < expectedSize) {
            byte[] padded = new byte[expectedSize];
            System.arraycopy(xBytes, 0, padded, expectedSize - xBytes.length, xBytes.length);
            xBytes = padded;
        }
        if (yBytes.length < expectedSize) {
            byte[] padded = new byte[expectedSize];
            System.arraycopy(yBytes, 0, padded, expectedSize - yBytes.length, yBytes.length);
            yBytes = padded;
        }

        String x = Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes);
        String y = Base64.getUrlEncoder().withoutPadding().encodeToString(yBytes);

        // Generate kid
        String kid = generateKid(publicKey);

        // Get correct JWKS curve name
        String javaCurveName = getCurveName(algorithm);
        String jwksCurveName = getJwksCurveName(javaCurveName);
        
        // Build JWKS
        Map<String, Object> key = new HashMap<>();
        key.put("kty", "EC");
        key.put("use", "sig");
        key.put("alg", algorithm);
        key.put("kid", kid);
        key.put("crv", jwksCurveName);
        key.put("x", x);
        key.put("y", y);
        
        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", new Object[]{key});

        // Return compact JSON manually to avoid any newlines
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);
        return mapper.writeValueAsString(jwks);
    }
    
    /**
     * Generate Key ID (kid) from public key using JWK thumbprint (RFC 7638)
     */
    private String generateKid(PublicKey publicKey) throws Exception {
        java.security.interfaces.ECPublicKey ecPublicKey = 
            (java.security.interfaces.ECPublicKey) publicKey;
        
        // Extract coordinates
        byte[] xBytes = ecPublicKey.getW().getAffineX().toByteArray();
        byte[] yBytes = ecPublicKey.getW().getAffineY().toByteArray();
        
        // Remove leading zero
        if (xBytes[0] == 0) {
            byte[] temp = new byte[xBytes.length - 1];
            System.arraycopy(xBytes, 1, temp, 0, temp.length);
            xBytes = temp;
        }
        if (yBytes[0] == 0) {
            byte[] temp = new byte[yBytes.length - 1];
            System.arraycopy(yBytes, 1, temp, 0, temp.length);
            yBytes = temp;
        }
        
        String x = Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes);
        String y = Base64.getUrlEncoder().withoutPadding().encodeToString(yBytes);
        
        // Create canonical JWK for thumbprint
        String jwkCanonical = String.format(
            "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"%s\",\"y\":\"%s\"}",
            x, y
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

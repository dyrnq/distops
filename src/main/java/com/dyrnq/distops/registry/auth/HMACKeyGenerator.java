package com.dyrnq.distops.registry.auth;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HMAC Key Generator for HS256/HS384/HS512
 */
@Slf4j
@Component
public class HMACKeyGenerator {

    /**
     * Generate HMAC secret key
     * @param algorithm HS256, HS384, or HS512
     * @return Base64 encoded secret key
     */
    public static String generateSecretKey(String algorithm) {
        try {
            String algName = getJcaAlgorithm(algorithm);
            KeyGenerator keyGen = KeyGenerator.getInstance(algName);
            int keySize = getKeySize(algorithm);
            keyGen.init(keySize);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate HMAC key for algorithm: {}", algorithm, e);
            throw new RuntimeException("Failed to generate HMAC key", e);
        }
    }

    /**
     * Get JCA algorithm name
     */
    private static String getJcaAlgorithm(String algorithm) {
        switch (algorithm) {
            case "HS256":
                return "HmacSHA256";
            case "HS384":
                return "HmacSHA384";
            case "HS512":
                return "HmacSHA512";
            default:
                throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithm);
        }
    }

    /**
     * Get key size in bits
     */
    private static int getKeySize(String algorithm) {
        switch (algorithm) {
            case "HS256":
                return 256;
            case "HS384":
                return 384;
            case "HS512":
                return 512;
            default:
                throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithm);
        }
    }

    /**
     * Generate and print key for testing
     */
    public static void main(String[] args) {
        System.out.println("HMAC Secret Keys:");
        System.out.println("HS256: " + generateSecretKey("HS256"));
        System.out.println("HS384: " + generateSecretKey("HS384"));
        System.out.println("HS512: " + generateSecretKey("HS512"));
    }
}

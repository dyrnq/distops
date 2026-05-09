package com.dyrnq.distops.registry.auth.controller;

import com.dyrnq.distops.registry.auth.KeyPairInfo;
import com.dyrnq.distops.registry.auth.KeyPairManager;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for key pair generation and management
 */
@Slf4j
@Controller
@Mapping("/auth/keys")
public class KeyPairController {

    @Inject
    private KeyPairManager keyPairManager;

    /**
     * Generate a new key pair
     *
     * @param type      Key type (EC or RSA), default is EC
     * @param algorithm Key algorithm (ES256, ES384, ES512, RS256, RS384, RS512)
     * @return Key pair information including private key, public key, and JWKS
     */
    @Mapping(value = "/generate")
    public Map<String, Object> generateKeyPair(
            @Param(required = false, name = "type") String type,
            @Param(required = false, name = "algorithm") String algorithm) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Default to EC if not specified
            String keyType = type != null ? type.toUpperCase() : "EC";

            KeyPairInfo keyPair;
            if ("RSA".equalsIgnoreCase(keyType)) {
                // Default to RS256 if not specified
                String alg = algorithm != null ? algorithm : "RS256";
                keyPair = keyPairManager.generateRSAKeyPair(alg);
            } else {
                // Default to ES256 if not specified
                String alg = algorithm != null ? algorithm : "ES256";
                keyPair = keyPairManager.generateECKeyPair(alg);
            }

            result.put("success", true);
            result.put("data", keyPair);
            result.put("message", "Key pair generated successfully");

            log.info("Generated {} key pair with algorithm {} and kid {}",
                    keyPair.getKeyType(), keyPair.getAlgorithm(), keyPair.getKid());

        } catch (Exception e) {
            log.error("Failed to generate key pair", e);
            result.put("success", false);
            result.put("message", "Failed to generate key pair: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get supported key types and algorithms
     *
     * @return Map of supported key types and algorithms
     */
    @Mapping(value = "/supported")
    public Map<String, Object> getSupportedAlgorithms() {
        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("data", keyPairManager.getSupportedAlgorithms());
        result.put("message", "Supported algorithms retrieved successfully");

        return result;
    }

    /**
     * Generate JWKS from existing public key
     *
     * @param publicKeyPem Public key in PEM format
     * @param algorithm    Key algorithm
     * @return JWKS JSON
     */
    @Mapping(value = "/jwks")
    public Map<String, Object> generateJwks(
            @Param(name = "public_key") String publicKeyPem,
            @Param(name = "algorithm") String algorithm) {

        Map<String, Object> result = new HashMap<>();

        try {
            // This would require additional implementation to parse PEM and generate JWKS
            // For now, return error
            result.put("success", false);
            result.put("message", "Not implemented yet. Please use /generate endpoint.");

        } catch (Exception e) {
            log.error("Failed to generate JWKS", e);
            result.put("success", false);
            result.put("message", "Failed to generate JWKS: " + e.getMessage());
        }

        return result;
    }
}

package com.dyrnq.distops.registry.auth.service.impl;

import com.dyrnq.distops.registry.auth.model.JWTPayload;
import com.dyrnq.distops.registry.auth.service.ITokenService;
import com.dyrnq.distops.registry.auth.util.NimbusJwtService;
import com.dyrnq.distops.model.Inst;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * HMAC Token Service Implementation using Nimbus JOSE+JWT
 */
@Slf4j
@RequiredArgsConstructor
public class HMACTokenServiceImpl implements ITokenService {
    private final Inst inst;
    private final NimbusJwtService nimbusJwtService = new NimbusJwtService();
    private String algorithm;

    @Override
    public String getKeyType() {
        return inst.getAuthKeyType();
    }

    @Override
    public String getAlgorithm() {
        return inst.getAuthKeyAlg();
    }


    @Override
    public String createToken(String subject, String audience, List<JWTPayload.ResourceAccess> accessList) {

        String jwksJson = inst.getAuthJwksJson();
        String issuer = inst.getAuthIssuer();
        String privateKey = inst.getAuthPrivateKey();
        long expiration = 60000;

        try {
            // Extract kid and algorithm from JWKS
            String kid = null;
            if (jwksJson != null && !jwksJson.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jwks = mapper.readTree(jwksJson);
                com.fasterxml.jackson.databind.JsonNode keysNode = jwks.path("keys");
                if (keysNode.isArray() && !keysNode.isEmpty()) {
                    kid = keysNode.path(0).path("kid").asText();
                    this.algorithm = keysNode.path(0).path("alg").asText("HS256");
                }
            }

            if (kid == null || kid.isEmpty()) {
                kid = "hmac-key-" + System.currentTimeMillis();
            }
            if (this.algorithm == null || this.algorithm.isEmpty()) {
                this.algorithm = "HS256";
            }

            // Initialize Nimbus JWT service with HMAC secret
            nimbusJwtService.initHMAC(privateKey, this.algorithm, kid);

            log.info("HMAC token service initialized with kid: {}, algorithm: {}", kid, this.algorithm);
        } catch (Exception e) {
            log.error("Failed to initialize HMAC token service", e);
            throw new RuntimeException("Failed to initialize HMAC token service", e);
        }

        try {
            // Convert access list to Map format
            List<Map<String, Object>> accessMaps = new java.util.ArrayList<>();
            for (JWTPayload.ResourceAccess access : accessList) {
                java.util.Map<String, Object> accessMap = new java.util.HashMap<>();
                accessMap.put("type", access.getType());
                accessMap.put("name", access.getName());
                accessMap.put("actions", access.getActions());
                accessMaps.add(accessMap);
            }

            String token = nimbusJwtService.createToken(
                    subject,
                    audience,
                    accessMaps,
                    issuer,
                    expiration
            );

            log.info("New HMAC token for subject: {}, audience: {}, algorithm: {}", subject, audience, algorithm);

            return token;
        } catch (JOSEException e) {
            log.error("Failed to create HMAC token", e);
            throw new RuntimeException("Failed to create HMAC token", e);
        }
    }
}

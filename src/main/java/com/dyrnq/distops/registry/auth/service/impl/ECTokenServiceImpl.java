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
 * ECDSA Token Service Implementation using Nimbus JOSE+JWT
 */
@Slf4j
@RequiredArgsConstructor
public class ECTokenServiceImpl implements ITokenService {

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
        String privateKeyPem = inst.getAuthPrivateKey();
        long expiration = 60000;

        try {
            // Extract kid and algorithm from JWKS
            String kid = null;
            if (jwksJson != null && !jwksJson.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jwks = mapper.readTree(jwksJson);
                com.fasterxml.jackson.databind.JsonNode keysNode = jwks.path("keys");
                if (keysNode.isArray() && keysNode.size() > 0) {
                    kid = keysNode.path(0).path("kid").asText();
                    this.algorithm = keysNode.path(0).path("alg").asText("ES256");
                }
            }

            if (kid == null || kid.isEmpty()) {
                kid = "ec-key-" + System.currentTimeMillis();
            }
            if (this.algorithm == null || this.algorithm.isEmpty()) {
                this.algorithm = "ES256";
            }

            // Initialize Nimbus JWT service
            nimbusJwtService.initEC(privateKeyPem, this.algorithm, kid);

            log.info("EC token service initialized with kid: {}, algorithm: {}", kid, this.algorithm);
        } catch (Exception e) {
            log.error("Failed to initialize EC token service", e);
            throw new RuntimeException("Failed to initialize EC token service", e);
        }


        try {
            log.info("Creating token with algorithm: {}", algorithm);

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

            log.info("New ECDSA token for subject: {}, audience: {}", subject, audience);

            return token;
        } catch (JOSEException e) {
            log.error("Failed to create ECDSA token", e);
            throw new RuntimeException("Failed to create ECDSA token", e);
        }
    }
}

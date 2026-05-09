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
 * RSA Token Service Implementation using Nimbus JOSE+JWT
 */
@Slf4j
@RequiredArgsConstructor
public class RSATokenServiceImpl implements ITokenService {
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
                if (keysNode.isArray() && !keysNode.isEmpty()) {
                    kid = keysNode.path(0).path("kid").asText();
                    this.algorithm = keysNode.path(0).path("alg").asText("RS256");
                }
            }

            if (kid == null || kid.isEmpty()) {
                kid = "rsa-key-" + System.currentTimeMillis();
            }
            if (this.algorithm == null || this.algorithm.isEmpty()) {
                this.algorithm = "RS256";
            }

            // Initialize Nimbus JWT service
            nimbusJwtService.initRSA(privateKeyPem, this.algorithm, kid);

            log.info("RSA token service initialized with kid: {}, algorithm: {}", kid, this.algorithm);
        } catch (Exception e) {
            log.error("Failed to initialize RSA token service", e);
            throw new RuntimeException("Failed to initialize RSA token service", e);
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

            log.info("New RSA token for subject: {}, audience: {}, algorithm: {}", subject, audience, algorithm);

            return token;
        } catch (JOSEException e) {
            log.error("Failed to create RSA token", e);
            throw new RuntimeException("Failed to create RSA token", e);
        }
    }
}

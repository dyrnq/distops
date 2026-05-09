package com.dyrnq.distops.registry.auth.controller;

import com.dyrnq.distops.registry.auth.KeyPairInfo;
import com.dyrnq.distops.registry.auth.KeyPairManager;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Inst;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for key pair generation
 */
@Slf4j
@Controller
@Mapping("/test/keys")
public class KeyPairTestController {

    @Inject
    private KeyPairManager keyPairManager;

    @Inject
    private InstMapper instMapper;

    /**
     * Test key pair generation and save to database
     */
    @Mapping(value = "/generate-and-save")
    public Map<String, Object> generateAndSave(
            @Param(name = "inst_id") Long instId,
            @Param(required = false, name = "type") String type,
            @Param(required = false, name = "algorithm") String algorithm) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find instance
            Inst inst = instMapper.selectById(instId);
            if (inst == null) {
                result.put("success", false);
                result.put("message", "Instance not found with id: " + instId);
                return result;
            }

            // Generate key pair
            KeyPairInfo keyPair;
            if ("RSA".equalsIgnoreCase(type)) {
                keyPair = keyPairManager.generateRSAKeyPair(algorithm != null ? algorithm : "RS256");
            } else {
                keyPair = keyPairManager.generateECKeyPair(algorithm != null ? algorithm : "ES256");
            }

            // Save to database
            inst.setAuthPrivateKey(keyPair.getPrivateKeyPem());
            inst.setAuthPublicKey(keyPair.getPublicKeyPem());
            inst.setAuthJwksJson(keyPair.getJwksJson());
            inst.setAuthKeyType(keyPair.getKeyType());
            inst.setAuthKeyAlg(keyPair.getAlgorithm());

            // Use update method from InstMapper
            instMapper.updateKeyPair(inst);

            result.put("success", true);
            result.put("data", keyPair);
            result.put("message", "Key pair generated and saved successfully");

            log.info("Generated and saved {} key pair for instance {} (id={})",
                    keyPair.getKeyType(), inst.getName(), inst.getId());

        } catch (Exception e) {
            log.error("Failed to generate and save key pair", e);
            result.put("success", false);
            result.put("message", "Failed: " + e.getMessage());
            result.put("error", e.toString());
        }

        return result;
    }

    /**
     * Test simple key pair generation
     */
    @Mapping(value = "/generate")
    public Map<String, Object> generate(
            @Param(required = false, name = "type") String type,
            @Param(required = false, name = "algorithm") String algorithm) {

        Map<String, Object> result = new HashMap<>();

        try {
            KeyPairInfo keyPair;
            if ("RSA".equalsIgnoreCase(type)) {
                keyPair = keyPairManager.generateRSAKeyPair(algorithm != null ? algorithm : "RS256");
            } else {
                keyPair = keyPairManager.generateECKeyPair(algorithm != null ? algorithm : "ES256");
            }

            result.put("success", true);
            result.put("data", keyPair);
            result.put("message", "Key pair generated successfully");

        } catch (Exception e) {
            log.error("Failed to generate key pair", e);
            result.put("success", false);
            result.put("message", "Failed: " + e.getMessage());
            result.put("error", e.toString());
        }

        return result;
    }
}

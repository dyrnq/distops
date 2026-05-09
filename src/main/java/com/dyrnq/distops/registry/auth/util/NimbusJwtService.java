package com.dyrnq.distops.registry.auth.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT Token Service using Nimbus JOSE+JWT
 * Supports EC (ES256/ES384/ES512), RSA (RS256/RS384/RS512/PS256/PS384/PS512), and HMAC (HS256/HS384/HS512)
 */
@Slf4j
public class NimbusJwtService {

    private JWSAlgorithm algorithm;
    private JWSSigner signer;
    private String kid;

    /**
     * Initialize with EC private key
     */
    public void initEC(String privateKeyPem, String algorithmStr, String kid) throws Exception {
        this.kid = kid;
        this.algorithm = JWSAlgorithm.parse(algorithmStr);
        
        // Parse PEM to get EC private key
        String pemContent = privateKeyPem
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = java.util.Base64.getDecoder().decode(pemContent);
        ECPrivateKey ecPrivateKey = (ECPrivateKey) java.security.KeyFactory.getInstance("EC")
            .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
        
        // Create signer directly from private key
        switch (algorithmStr) {
            case "ES256":
                this.signer = new ECDSASigner(ecPrivateKey);
                break;
            case "ES384":
                this.signer = new ECDSASigner(ecPrivateKey);
                break;
            case "ES512":
                this.signer = new ECDSASigner(ecPrivateKey);
                break;
            default:
                throw new IllegalArgumentException("Unsupported EC algorithm: " + algorithmStr);
        }
        
        log.info("Initialized EC signer with algorithm: {}, kid: {}", algorithmStr, kid);
    }

    /**
     * Initialize with RSA private key
     */
    public void initRSA(String privateKeyPem, String algorithmStr, String kid) throws Exception {
        this.kid = kid;
        this.algorithm = JWSAlgorithm.parse(algorithmStr);
        
        // Parse PEM to get RSA private key
        String pemContent = privateKeyPem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = java.util.Base64.getDecoder().decode(pemContent);
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) java.security.KeyFactory.getInstance("RSA")
            .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
        
        // Create signer based on algorithm
        switch (algorithmStr) {
            case "RS256":
            case "RS384":
            case "RS512":
                this.signer = new RSASSASigner(rsaPrivateKey);
                break;
            case "PS256":
            case "PS384":
            case "PS512":
                // PS256/384/512 not supported in Nimbus 10.8, fallback to RS256/384/512
                log.warn("PS algorithms not supported in Nimbus 10.8, falling back to RS algorithm");
                this.signer = new RSASSASigner(rsaPrivateKey);
                break;
            default:
                throw new IllegalArgumentException("Unsupported RSA algorithm: " + algorithmStr);
        }
        
        log.info("Initialized RSA signer with algorithm: {}, kid: {}", algorithmStr, kid);
    }

    /**
     * Initialize with HMAC secret
     */
    public void initHMAC(String secretBase64, String algorithmStr, String kid) throws Exception {
        this.kid = kid;
        this.algorithm = JWSAlgorithm.parse(algorithmStr);
        
        byte[] secretBytes = java.util.Base64.getDecoder().decode(secretBase64);
        javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(secretBytes, "HMAC");
        
        switch (algorithmStr) {
            case "HS256":
                this.signer = new MACSigner(secretKey);
                break;
            case "HS384":
                this.signer = new MACSigner(secretKey);
                break;
            case "HS512":
                this.signer = new MACSigner(secretKey);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithmStr);
        }
        
        log.info("Initialized HMAC signer with algorithm: {}, kid: {}", algorithmStr, kid);
    }

    /**
     * Create signed JWT token
     */
    public String createToken(String subject, String audience, List<Map<String, Object>> accessList, 
                             String issuer, long expirationSeconds) throws JOSEException {
        if (signer == null) {
            throw new JOSEException("Signer not initialized");
        }

        long now = System.currentTimeMillis() / 1000;
        
        // Build JWT claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .expirationTime(new Date((now + expirationSeconds) * 1000))
            .notBeforeTime(new Date((now - 10) * 1000))
            .issueTime(new Date(now * 1000))
            .jwtID(String.valueOf(Math.abs(new java.util.Random().nextLong())))
            .claim("access", accessList)
            .build();

        // Create JWS header
        JWSHeader header = new JWSHeader.Builder(algorithm)
            .keyID(kid)
            .type(JOSEObjectType.JWT)
            .build();

        // Create and sign JWT
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);

        log.info("Created {} token for subject: {}, audience: {}", algorithm.getName(), subject, audience);

        return signedJWT.serialize();
    }

    /**
     * Get current algorithm
     */
    public JWSAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Get current kid
     */
    public String getKid() {
        return kid;
    }
}

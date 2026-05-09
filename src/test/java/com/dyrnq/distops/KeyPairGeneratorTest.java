package com.dyrnq.distops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class KeyPairGeneratorTest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKeyPEM = "-----BEGIN PRIVATE KEY-----\n" +
                               Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) +
                               "\n-----END PRIVATE KEY-----";

        String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n" +
                              Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) +
                              "\n-----END PUBLIC KEY-----";

        Files.writeString(Path.of("scripts/hello.crt"), privateKeyPEM);
        Files.writeString(Path.of("scripts/hello.key"), publicKeyPEM);


    }
}

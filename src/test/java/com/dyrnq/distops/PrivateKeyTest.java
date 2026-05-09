package com.dyrnq.distops;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.*;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class PrivateKeyTest {
    private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static PrivateKey load(File file) throws IOException, CertificateException {
        Reader reader = null;

        try {
            reader = new FileReader(file);
            PEMParser parser = new PEMParser(reader);
            Object obj = parser.readObject();
            log.info(obj.getClass().getName());
            if (obj instanceof PEMKeyPair) {
                // 将 PEM 密钥对转换为 JCE 格式的密钥对
                KeyPair keyPair = new JcaPEMKeyConverter().setProvider(BC).getKeyPair((PEMKeyPair) obj);
                return keyPair.getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                return new JcaPEMKeyConverter().setProvider(BC).getPrivateKey((PrivateKeyInfo) obj);
            } else {
                throw new IllegalArgumentException("Unsupported PEM object.");
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

    }

    public static PrivateKey load(InputStream in) throws IOException, CertificateException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(in);
            PEMParser parser = new PEMParser(reader);
            Object obj = parser.readObject();
            if (obj instanceof PEMKeyPair) {
                // 将 PEM 密钥对转换为 JCE 格式的密钥对
                KeyPair keyPair = new JcaPEMKeyConverter().setProvider(BC).getKeyPair((PEMKeyPair) obj);
                return keyPair.getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                return new JcaPEMKeyConverter().setProvider(BC).getPrivateKey((PrivateKeyInfo) obj);
            } else {
                throw new IllegalArgumentException("Unsupported PEM object.");
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static void main(String[] args) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {

//        PrivateKey p =  PrivateKeyTest.load(new File("scripts/auth.pub") );
//        ECPrivateKey ecPrivateKey = (ECPrivateKey) p;


//        String keyContent = FileUtil.readString(new File("scripts/auth.key"), Charset.defaultCharset());
//
//        keyContent = StringUtils.substringBetween(keyContent,"-----BEGIN EC PRIVATE KEY-----","-----END EC PRIVATE KEY-----");
//
//        keyContent = keyContent
//                .replaceAll("\\s", "");
//
//        log.info(keyContent);
//
//
//        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
//        log.info(new String(keyBytes));

        PrivateKey ecPrivateKey  = load(new File("scripts/auth.key"));

        System.out.println(ecPrivateKey==null);

    }
}

package com.triasoft.garage.util;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String PUBLIC_KEY_PATH = "keys/public_key.pem";
    private static final String PRIVATE_KEY_PATH = "keys/private_key.pem";

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void loadKeys() {
//        generateKeys();
        this.publicKey = loadPublicKey();
        this.privateKey = loadPrivateKey();
    }

    public void generateKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate keys", e);
        }
    }

    private PrivateKey loadPrivateKey() {
        try {
            InputStream inputStream = new ClassPathResource(PRIVATE_KEY_PATH).getInputStream();
            String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    private PublicKey loadPublicKey() {
        try {
            InputStream inputStream = new ClassPathResource(PUBLIC_KEY_PATH).getInputStream();
            String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt", e);
        }

    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt", e);
        }
    }

    public static void main(String[] args) {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        encryptionUtil.generateKeys();
        String encryptedVal = encryptionUtil.encrypt("Nishan");
        System.out.println("encryptedVal : " + encryptedVal);

        String decryptedVal = encryptionUtil.decrypt(encryptedVal);
        System.out.println("decryptedVal : " + decryptedVal);
    }
}

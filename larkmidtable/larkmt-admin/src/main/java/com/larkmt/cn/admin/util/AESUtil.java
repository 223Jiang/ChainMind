package com.larkmt.cn.admin.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public class AESUtil {

    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY = "your-fixed-aes-key"; // 固定的 AES 密钥（16 字节）

    private static final IvParameterSpec IV = new IvParameterSpec(new byte[16]); // 固定的 IV

    public static String encrypt(String message) {
        try {
            SecretKey secretKey = getSecretKey(AES_KEY);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IV);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.warn("content encrypt error {}", e.getMessage());
        }
        return null;
    }

    public static String decrypt(String ciphertext) {
        try {
            SecretKey secretKey = getSecretKey(AES_KEY);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IV);
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("content decrypt error {}", e.getMessage());
        }
        return null;
    }

    private static SecretKey getSecretKey(String key) {
        byte[] keyBytes = Arrays.copyOf(key.getBytes(StandardCharsets.UTF_8), 16);
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    public static void main(String[] args) {
        final String message = "root";
        String ciphertext = encrypt(message);

        System.out.println("加密后密文为: " + ciphertext);
        System.out.println("解密后明文为: " + decrypt(ciphertext));
    }
}

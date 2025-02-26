package com.larkmt.cn.admin.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESSupersonicUtil {
    
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String KEY = "supersonic@2024a";

    public static String encryptPassword(String password) throws Exception {
        if (password == null || password.isEmpty()) {
            return password;
        }

        // 创建密钥规范
        SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
        
        // 初始化加密器
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        // 执行加密
        byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        
        // Base64编码
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}

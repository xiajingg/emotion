package com.emotion.api.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionUtil {

    // AES密钥，实际应用中应随机生成并安全存储
    private static final byte[] AES_KEY = {
            (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x80,
            (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0xf0,
            (byte)0x0e, (byte)0x00, (byte)0x00, (byte)0x0e,
            (byte)0x78, (byte)0x00, (byte)0x00, (byte)0x78,
            (byte)0x3c, (byte)0x00, (byte)0x00, (byte)0x3c,
            (byte)0x1e, (byte)0x00, (byte)0x00, (byte)0x1e,
            (byte)0x0f, (byte)0x00, (byte)0x00, (byte)0x0f,
            (byte)0x07, (byte)0x80, (byte)0x00, (byte)0x07
    };
    /**
     * 使用AES算法加密文本
     *
     * @param plainText 要加密的明文
     * @return 加密后的密文（Base64编码）
     * @throws Exception 如果加密过程中出现异常
     */
    public static String encrypt(String plainText) throws Exception {
        // 创建AES密钥（使用AES密钥和AES算法）
        SecretKeySpec key = new SecretKeySpec(AES_KEY, "AES");

        // 创建Cipher实例，并初始化为加密模式
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ECB模式简单，但安全性较低，推荐使用更安全的模式如CBC
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // 加密明文
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 将加密后的字节数组编码为Base64字符串，方便传输和存储
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

//    // 示例用法
//    public static void main(String[] args) {
//        try {
////            String plainText = "Hello, World!"; // 要加密的明文
////            String encryptedText = encrypt(plainText); // 加密明文
////            System.out.println("Encrypted Text: " + encryptedText); // 输出加密后的密文
//            System.out.println(getRandomSalt(4));
//        } catch (Exception e) {
//            e.printStackTrace(); // 处理加密过程中可能出现的异常
//        }
//    }

    /**
     * 生成指定长度的随机盐值
     *
     * @param i 盐值长度
     * @return 随机盐值
     */
    public static String getRandomSalt(int i) {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(i);
        for (int j = 0; j < i; j++) {
            int number = (int) (Math.random() * 62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}

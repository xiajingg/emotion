package com.emotion.api.util;

import java.util.Random;

public class ShortCodeGenerator {

    // 生成短码的方法
    public static String generateShortCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder shortCode = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            shortCode.append(characters.charAt(index));
        }

        return shortCode.toString();
    }

    // 主方法，演示如何生成短码
//    public static void main(String[] args) {
//        int length = 8; // 设置短码的长度
//        String shortCode = generateShortCode(length);
//        System.out.println("生成的短码是: " + shortCode);
//    }
}


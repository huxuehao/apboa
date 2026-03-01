package com.hxh.apboa.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * 描述：加密工具类
 *
 * @author huxuehao
 **/
public class CryptoUtils {

    /**
     * 使用默认的加盐值进行MD5加密
     * @param input  原始密码
     * @param salt 盐
     * @return 加密后的字符串
     */
    public static String md5(String input, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 获取一个随机的 UUID 字符串
     * @return 随机的 UUID 字符串
     */
    public static String uuid() {
         return UUID.randomUUID().toString().replace("-","");
    }


    /**
     * Rot13加密流程：
     * 数据 -> Base64 编码 -> ROT13 替换
     * @param str 要加密的字符串。
     * @return 加密后的字符串。
     */
    public static String encryptRot13(String str) {
        if (str == null) {
            return null;
        }
        try {
            // 先 Base64 编码，再 ROT13
            String base64Encoded = encode(str);
            return rot13(base64Encoded);
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * Rot13解密流程：
     * ROT13 -> Base64 -> 数据对象
     * @param encrypted 要解密的字符串
     * @return 解密后的字符串
     */
    public static String decryptRot13(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            // 先 ROT13 还原，再 Base64 解码
            String rot13Decoded = rot13(encrypted);
            return decode(rot13Decoded);
        } catch (Exception e) {
            return encrypted;
        }
    }

    /**
     * ROT13 函数：对给定字符串执行 ROT13 编码。
     * @param str - 要编码的字符串。
     * @return 编码后的字符串。
     */
    private static String rot13(String str) {
        StringBuilder result = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                // 小写字母处理
                c = (char) (((c - 'a' + 13) % 26) + 'a');
            } else if (c >= 'A' && c <= 'Z') {
                // 大写字母处理
                c = (char) (((c - 'A' + 13) % 26) + 'A');
            }
            // 非字母字符保持不变
            result.append(c);
        }

        return result.toString();
    }

    /**
     * 对输入字符串进行 Base64 编码
     *
     * @param input 输入字符串
     * @return 编码后的字符串
     */
    public static String encode(String input) {
        // 将字符串转换为字节数组
        byte[] encodedBytes = Base64.getEncoder().encode(input.getBytes());
        // 转换回字符串并返回
        return new String(encodedBytes);
    }

    /**
     * 对 Base64 编码的字符串进行解码
     *
     * @param encodedString 已编码的字符串
     * @return 解码后的原始字符串
     */
    public static String decode(String encodedString) {
        // 将字符串转换为字节数组
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString.getBytes());
        // 转换回字符串并返回
        return new String(decodedBytes);
    }
}

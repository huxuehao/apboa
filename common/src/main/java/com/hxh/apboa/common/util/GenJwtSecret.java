//package com.hxh.apboa.common.util;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.io.Encoders;
//
//import javax.crypto.SecretKey;
//
///**
// * 描述：
// *
// * @author huxuehao
// **/
//public class GenJwtSecret {
//    public static void main(String[] args) {
//        SecretKey key = Jwts.SIG.HS256.key().build();
//        String base64Key = Encoders.BASE64.encode(key.getEncoded());
//        System.out.println(base64Key);
//    }
//}

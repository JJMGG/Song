package com.iteima.mysong.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static String Keys="fengxiang";

    private static Long Time=100000000L;//时间不要设置错误了，靠北了，设置一秒过期搞得我找了好久的bug

    /**
     * 生成jwt令牌
     */
    public static String createJwt(Map<String, Object> claims){
        String jwt = Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256,Keys) //签名算法
                .setExpiration(new Date(System.currentTimeMillis() + Time))
                .compact();
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJwt(String jwt){

        Claims claims = Jwts.parser()
                .setSigningKey(Keys)
                .parseClaimsJws(jwt)
                .getBody();

        return claims;
    }
}

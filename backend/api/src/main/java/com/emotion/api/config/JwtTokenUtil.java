package com.emotion.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

// 使用Spring的@Component注解，表明这是一个Spring组件，可以被Spring容器自动检测并管理
@Component
public class JwtTokenUtil implements Serializable {

    // serialVersionUID用于序列化时确保类的版本兼容性
    private static final long serialVersionUID = -3301605591108950415L;

    // JWT的密钥，用于签名和验证token（Base64编码，32字节/256位，满足HMAC-SHA256最低要求）
    // 密钥不入库，通过配置注入：application-*.yml 的 jwt.secret，或环境变量 JWT_SECRET
    @Value("${jwt.secret}")
    private String secretKey;

    // JWT的过期时间，单位是毫秒。默认7天
    @Value("${jwt.expiration-ms:604800000}")
    private long jwtExpirationInMs;

    // 获取签名密钥
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 从token中提取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 从token中提取过期日期
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 这是一个泛型方法，用于从token中提取任意的claim（声明）。通过传入一个Function来决定提取哪个claim
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 解析token，并返回所有的claims（声明）
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 判断token是否已经过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 验证token是否有效。验证方式包括：1) token中的用户名与传入的用户名是否匹配 2) token是否已过期
    public boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    // 生成token的方法
    public String generateToken(String username) {
        // 设置JWT的签发时间和过期时间
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + jwtExpirationInMs);

        // 使用JWT Builder创建token
        return Jwts.builder()
                .subject(username) // 设置用户名
                .issuedAt(now) // 设置签发时间
                .expiration(expiration) // 设置过期时间
                .signWith(getSigningKey()) // 设置签名密钥
                .compact();
    }
}

package com.hmdp.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expire}")
    private Long expireTime;

    /**
     * 获取签名密钥
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT令牌，可以放自定义载荷
     * @param subject 主题，一般存用户名/用户id
     * @param extraClaims 额外自定义数据，比如角色、手机号等
     * @return jwt字符串
     */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireTime);
        return Jwts.builder()
                .claims(extraClaims) // 额外自定义数据
                .subject(subject)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSignKey())
                .compact();
    }

    // 重载：只传用户名，不需要额外数据
    public String generateToken(String subject) {
        return generateToken(subject, new HashMap<>());
    }

    /**
     * 解析token，拿到全部载荷Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验token是否合法（签名正确、未过期）
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("token已过期");
        } catch (MalformedJwtException e) {
            System.out.println("token格式错误");
        } catch (SignatureException e) {
            System.out.println("签名错误，token被篡改");
        } catch (Exception e) {
            System.out.println("token非法");
        }
        return false;
    }

    /**
     * 从token获取subject（用户名/用户id）
     */
    public String getSubject(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
}

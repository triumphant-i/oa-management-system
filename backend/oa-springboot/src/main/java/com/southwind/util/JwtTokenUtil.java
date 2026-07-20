package com.southwind.util;

import com.southwind.common.UserContext;
import com.southwind.enums.RoleType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token工具类
 * 用于生成、解析、验证JWT Token
 * 
 * Token包含信息：
 * - userId: 用户ID
 * - username: 用户名
 * - name: 真实姓名
 * - role: 角色
 * - departmentId: 部门ID
 */
@Component
public class JwtTokenUtil {

    // 密钥（生产环境应从配置文件读取）
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Token过期时间（默认24小时）
    @Value("${jwt.expiration:86400000}")
    private Long expiration = 86400000L; // 24小时（毫秒）

    /**
     * 生成JWT Token
     * @param userInfo 用户信息
     * @return JWT Token
     */
    public String generateToken(UserContext.UserInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userInfo.getUserId());
        claims.put("username", userInfo.getUsername());
        claims.put("name", userInfo.getName());
        claims.put("role", userInfo.getRole().getCode());
        claims.put("departmentId", userInfo.getDepartmentId());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userInfo.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 解析JWT Token
     * @param token JWT Token
     * @return 用户信息
     */
    public UserContext.UserInfo parseToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Integer userId = claims.get("userId", Integer.class);
            String username = claims.get("username", String.class);
            String name = claims.get("name", String.class);
            String roleCode = claims.get("role", String.class);
            Integer departmentId = claims.get("departmentId", Integer.class);

            RoleType role = RoleType.getByCode(roleCode);

            return new UserContext.UserInfo(userId, username, name, role, departmentId);
        } catch (Exception e) {
            // Token解析失败
            return null;
        }
    }

    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Token是否过期
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从Token中获取用户名
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
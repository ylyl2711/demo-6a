package com.project.stockwork.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.project.stockwork.common.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours:12}")
    private long expireHours;

    public String createToken(LoginUser user) {
        long now = System.currentTimeMillis();
        long expireAt = now + expireHours * 3600_000L;
        return JWT.create()
                .setPayload("uid", String.valueOf(user.getId()))
                .setPayload("username", user.getUsername())
                .setPayload("realName", user.getRealName())
                .setPayload("roleCode", user.getRoleCode())
                .setPayload("roleName", user.getRoleName())
                .setPayload("expireAt", String.valueOf(expireAt))
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    public LoginUser parseToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            jwt.setKey(secret.getBytes(StandardCharsets.UTF_8));
            if (!jwt.verify()) {
                return null;
            }
            String expireAt = String.valueOf(jwt.getPayload("expireAt"));
            if (System.currentTimeMillis() > Long.parseLong(expireAt)) {
                return null;
            }
            return new LoginUser(
                    Long.parseLong(String.valueOf(jwt.getPayload("uid"))),
                    String.valueOf(jwt.getPayload("username")),
                    String.valueOf(jwt.getPayload("realName")),
                    String.valueOf(jwt.getPayload("roleCode")),
                    String.valueOf(jwt.getPayload("roleName")),
                    null);
        } catch (Exception e) {
            return null;
        }
    }

    public Date expireDate() {
        return new Date(System.currentTimeMillis() + expireHours * 3600_000L);
    }
}

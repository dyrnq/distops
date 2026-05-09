package com.dyrnq.utils;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;


public class JwtUtils {
    static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private static final String TOKEN_HEADER = "Bearer ";

    public static String createKey() {
        //Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        Key key = Jwts.SIG.HS512.key().build();
        return Encoders.BASE64.encode(key.getEncoded());
    }


    /**
     * 构建令牌
     *
     * @param claims     数据
     * @param expire     超时（单位：毫秒）
     * @param jwt_secret 签名密钥
     * @param jwt_prefix 前缀
     */
    public static String buildJwt(Claims claims, long expire, String jwt_secret, String jwt_prefix) {

        byte[] keyBytes = Decoders.BASE64.decode(jwt_secret);
        SecretKey signKey = Keys.hmacShaKeyFor(keyBytes);

        JwtBuilder builder;
        if (expire > 0) {
            builder = Jwts.builder()
                    .claims().empty().add(claims).and()
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expire));
        } else {
            builder = Jwts.builder()
                    .claims().empty().add(claims).and()
                    .issuedAt(new Date());
        }

        if (Utils.isNotEmpty(Solon.cfg().appName())) {
            builder.issuer(Solon.cfg().appName());
        }

        if (Utils.isNotEmpty(jwt_prefix)) {
            return jwt_prefix + " " + builder.signWith(signKey).compact();
        } else {
            return builder.signWith(signKey).compact();
        }
    }


    /**
     * 解析令牌
     *
     * @param token      令牌
     * @param jwt_secret 签名密钥
     * @param jwt_prefix 前缀
     */
    public static Claims parseJwt(String token, String jwt_secret, String jwt_prefix) {


        byte[] keyBytes = Decoders.BASE64.decode(jwt_secret);
        SecretKey signKey = Keys.hmacShaKeyFor(keyBytes);

        if (token.startsWith(TOKEN_HEADER)) {
            token = token.substring(TOKEN_HEADER.length()).trim();
        }
        // %20 为空格URLEncoding
        if (token.startsWith(StrUtil.replace(TOKEN_HEADER, " ", "%20"))) {
            token = token.substring(StrUtil.replace(TOKEN_HEADER, " ", "%20").length()).trim();
        }

        if (Utils.isNotEmpty(jwt_prefix) && token.startsWith(jwt_prefix)) {
            token = token.substring(jwt_prefix.length()).trim();
        }

        try {
            return Jwts.parser()
                    .verifyWith(signKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {

        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }

        return null;
    }

}

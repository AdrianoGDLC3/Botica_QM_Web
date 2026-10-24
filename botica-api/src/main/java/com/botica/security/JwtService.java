package com.botica.security;

import com.botica.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // mínimo 32 bytes
        this.ttlMs = minutes * 60_000;
    }

    public String generar(Usuario u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("rol", u.getRol().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims leer(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

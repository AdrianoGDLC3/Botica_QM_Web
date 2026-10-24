package com.botica.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Lee "Authorization: Bearer <token>". El principal es el id del usuario (Long).
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;

    public JwtFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.leer(h.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(
                        Long.valueOf(c.getSubject()), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + c.get("rol", String.class))));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // token inválido o vencido: sigue sin autenticar -> 401
            }
        }
        chain.doFilter(req, res);
    }
}

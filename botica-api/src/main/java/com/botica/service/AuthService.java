package com.botica.service;

import com.botica.dto.Dtos.*;
import com.botica.error.ApiException;
import com.botica.repo.UsuarioRepository;
import com.botica.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public LoginRes login(LoginReq r) {
        String correo = r.correo() == null ? "" : r.correo().trim();
        var encontrado = usuarios.findByCorreoIgnoreCase(correo);

        // Diagnóstico temporal: nunca registrar la contraseña ni el hash.
        log.info("LOGIN_DIAG usuarioEncontrado={}", encontrado.isPresent());

        var u = encontrado.orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        boolean activo = Boolean.TRUE.equals(u.getActivo());
        boolean passwordCoincide = r.password() != null
                && u.getPassword() != null
                && encoder.matches(r.password(), u.getPassword());

        log.info("LOGIN_DIAG activo={}, passwordCoincide={}, rol={}",
                activo, passwordCoincide, u.getRol());

        if (!activo || !passwordCoincide) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos");
        }

        return new LoginRes(jwt.generar(u), new UsuarioRes(
                u.getId(), u.getNombre(), u.getDni(), u.getTelefono(),
                u.getCorreo(), u.getRol(), u.getActivo()));
    }
}

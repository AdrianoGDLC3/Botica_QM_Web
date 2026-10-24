package com.botica.web;

import com.botica.dto.Dtos.*;
import com.botica.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final CatalogoService s;

    @GetMapping public List<UsuarioRes> listar() { return s.usuarios(); }

    @GetMapping("/clientes") public List<UsuarioRes> clientes() { return s.clientes(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRes crear(@Valid @RequestBody UsuarioReq r) { return s.crearUsuario(r); }

    @PutMapping("/{id}")
    public UsuarioRes actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioReq r) { return s.actualizarUsuario(id, r); }

    // Baja lógica (activo = false)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id, @AuthenticationPrincipal Long actual) { s.desactivarUsuario(id, actual); }
}

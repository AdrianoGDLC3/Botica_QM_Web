package com.botica.web;

import com.botica.dto.Dtos.*;
import com.botica.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {
    private final CompraService s;

    @GetMapping public List<CompraRes> listar() { return s.listar(); }

    @GetMapping("/{id}") public CompraRes detalle(@PathVariable Long id) { return s.detalle(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CompraRes registrar(@Valid @RequestBody CompraReq r, @AuthenticationPrincipal Long usuarioId) {
        return s.registrar(r, usuarioId);
    }
}

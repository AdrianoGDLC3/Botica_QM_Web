package com.botica.web;

import com.botica.dto.Dtos.*;
import com.botica.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {
    private final VentaService s;

    @GetMapping public List<VentaRes> listar() { return s.listar(); }

    @GetMapping("/{id}") public VentaRes detalle(@PathVariable Long id) { return s.detalle(id); }

    // El empleado sale del token, el precio y el total los calcula el servidor
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public VentaRes registrar(@Valid @RequestBody VentaReq r, @AuthenticationPrincipal Long empleadoId) {
        return s.registrar(r, empleadoId);
    }
}

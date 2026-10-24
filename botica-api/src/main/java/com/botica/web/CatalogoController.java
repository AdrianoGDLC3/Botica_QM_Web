package com.botica.web;

import com.botica.dto.Dtos.*;
import com.botica.service.CatalogoService;
import com.botica.service.LaboratorioService;
import com.botica.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogoController {
    private final CatalogoService s;
    private final LaboratorioService labs;
    private final ProveedorService provs;

    // ---- laboratorios ----
    @GetMapping("/laboratorios") public List<EntidadContactoRes> laboratorios() { return labs.listar(); }

    @PostMapping("/laboratorios") @ResponseStatus(HttpStatus.CREATED)
    public EntidadContactoRes crearLab(@Valid @RequestBody LaboratorioReq r) { return labs.crear(r); }

    @PutMapping("/laboratorios/{id}")
    public EntidadContactoRes actualizarLab(@PathVariable Long id, @Valid @RequestBody LaboratorioReq r) { return labs.actualizar(id, r); }

    // Baja lógica (activo = false), igual que proveedores
    @DeleteMapping("/laboratorios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarLab(@PathVariable Long id) { labs.desactivar(id); }

    // Eliminación definitiva (409 con el motivo si hay historial que se perdería). El DELETE de arriba
    // sigue siendo la baja lógica de siempre.
    @DeleteMapping("/laboratorios/{id}/definitivo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarLab(@PathVariable Long id) { labs.eliminar(id); }

    // ---- proveedores ----
    @GetMapping("/proveedores") public List<EntidadContactoRes> proveedores() { return provs.listar(); }

    @PostMapping("/proveedores") @ResponseStatus(HttpStatus.CREATED)
    public EntidadContactoRes crearProveedor(@Valid @RequestBody ProveedorReq r) { return provs.crear(r); }

    @PutMapping("/proveedores/{id}")
    public EntidadContactoRes actualizarProveedor(@PathVariable Long id, @Valid @RequestBody ProveedorReq r) { return provs.actualizar(id, r); }

    @DeleteMapping("/proveedores/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarProveedor(@PathVariable Long id) { provs.desactivar(id); }

    @DeleteMapping("/proveedores/{id}/definitivo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProveedor(@PathVariable Long id) { provs.eliminar(id); }

    // ---- productos (medicamentos, higiene, cuidado personal, etc.) ----
    @GetMapping("/productos") public List<ProductoRes> productos() { return s.productos(); }

    @PostMapping("/productos") @ResponseStatus(HttpStatus.CREATED)
    public ProductoRes crearProducto(@Valid @RequestBody ProductoReq r) { return s.crearProducto(r); }

    @PutMapping("/productos/{id}")
    public ProductoRes actualizarProducto(@PathVariable Long id, @Valid @RequestBody ProductoReq r) { return s.actualizarProducto(id, r); }

    // Ruta nueva (no reemplaza ninguna): duplica un producto como plantilla. Solo ADMIN, por /productos/**
    @PostMapping("/productos/{id}/duplicar") @ResponseStatus(HttpStatus.CREATED)
    public ProductoRes duplicarProducto(@PathVariable Long id) { return s.duplicarProducto(id); }

    @DeleteMapping("/productos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarProducto(@PathVariable Long id) { s.desactivarProducto(id); }

    @DeleteMapping("/productos/{id}/definitivo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProducto(@PathVariable Long id) { s.eliminarProducto(id); }

    // ---- lotes (se crean desde compras) ----
    @GetMapping("/lotes") public List<LoteRes> lotes() { return s.lotes(); }

    @GetMapping("/lotes/disponibles")
    public List<LoteRes> disponibles(@RequestParam Long productoId) { return s.lotesDisponibles(productoId); }

    @PutMapping("/lotes/{id}")
    public LoteRes actualizarLote(@PathVariable Long id, @Valid @RequestBody LoteReq r) { return s.actualizarLote(id, r); }
}

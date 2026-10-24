package com.botica.dto;

import com.botica.domain.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    // ---- auth / usuarios ----
    public record LoginReq(@NotBlank String correo, @NotBlank String password) {}
    public record UsuarioRes(Long id, String nombre, String dni, String telefono, String correo, Rol rol, Boolean activo) {}
    public record LoginRes(String token, UsuarioRes usuario) {}
    public record UsuarioReq(@NotBlank String nombre, String dni, String telefono,
                             @NotBlank @Email String correo, String password,
                             @NotNull Rol rol, Boolean activo) {}

    // ---- laboratorios / proveedores ----
    public record LaboratorioReq(@NotBlank String nombre, String pais, String ruc, String contacto,
                                 String telefono, @Email String correo, String direccion, Boolean activo) {}
    public record ProveedorReq(@NotBlank String nombre, String ruc, String contacto, String telefono,
                               String correo, String direccion, Boolean activo) {}
    public record LabRes(Long id, String nombre) {}
    // Salida común de Laboratorio y Proveedor (ver adapter.EntidadContactoAdapter, patrón Adapter);
    // pais viene null para Proveedor, que no tiene ese campo.
    public record EntidadContactoRes(Long id, String nombre, String ruc, String contacto, String telefono,
                                     String correo, String direccion, String pais, Boolean activo) {}

    // ---- productos (medicamentos, higiene, cuidado personal, etc.) ----
    // precioVenta: un único precio, por UNA unidad de venta (todo se vende por unidad).
    // atributos: lo específico de cada categoría (concentración, talla, capacidad...), en JSON libre.
    public record ProductoReq(@NotBlank String nombre, String descripcion, @NotBlank String categoria,
                              Boolean requiereReceta, Map<String, Object> atributos,
                              @NotNull @DecimalMin("0.0") BigDecimal precioVenta,
                              @NotBlank String unidadVenta,
                              List<Long> laboratorioIds, Boolean activo) {}
    public record ProductoRes(Long id, String nombre, String descripcion, String categoria, Boolean requiereReceta,
                              Map<String, Object> atributos, BigDecimal precioVenta,
                              String unidadVenta, Boolean activo, List<LabRes> laboratorios) {}

    // laboratorioId: opcional, solo para corregir el laboratorio asignado a un lote ya creado
    public record LoteReq(@NotBlank String numeroLote, @NotNull LocalDate fechaVencimiento, Long laboratorioId) {}
    public record LoteRes(Long id, Long productoId, String productoNombre, Long laboratorioId,
                          String laboratorioNombre, String numeroLote, LocalDate fechaVencimiento, BigDecimal stockActual) {}

    // ---- ventas: cantidad está en la unidad_venta del producto ----
    public record VentaItemReq(@NotNull Long loteId, @NotNull @Min(1) Integer cantidad) {}
    // recetaPresentada: el vendedor confirma que el cliente trae la receta. Es obligatorio (true) si
    // algún producto de la venta tiene requiereReceta; número de receta y médico son opcionales.
    public record VentaReq(Long clienteId, String tipoComprobante, String numeroComprobante, String metodoPago,
                           Boolean recetaPresentada, String recetaNumero, String recetaMedico,
                           @NotEmpty @Valid List<VentaItemReq> items) {}
    public record DetalleVentaRes(String productoNombre, String numeroLote, Integer cantidad, BigDecimal precioVenta,
                                  Boolean requiereReceta) {}
    public record VentaRes(Long id, LocalDateTime fechaVenta, String clienteNombre, String empleadoNombre,
                           String tipoComprobante, String numeroComprobante, String metodoPago,
                           Boolean recetaPresentada, String recetaNumero, String recetaMedico,
                           BigDecimal total, List<DetalleVentaRes> detalles) {}

    // ---- compras ----
    // cantidad = cajas compradas; unidadesPorCaja = unidades que trae cada caja en ESTA compra;
    // precioCompra = costo por caja. Stock que entra = cantidad * unidadesPorCaja (en unidades).
    // laboratorioId: de qué laboratorio (de los asociados al producto) viene este lote
    public record CompraItemReq(@NotNull Long productoId, @NotNull Long laboratorioId, @NotBlank String numeroLote,
                                @NotNull LocalDate fechaVencimiento, @NotNull @Min(1) Integer cantidad,
                                @NotNull @Min(1) Integer unidadesPorCaja,
                                @NotNull @DecimalMin("0.0") BigDecimal precioCompra) {}
    public record CompraReq(@NotNull Long proveedorId, String tipoComprobante, String numeroComprobante,
                            @NotEmpty @Valid List<CompraItemReq> items) {}
    public record DetalleCompraRes(String productoNombre, String laboratorioNombre, String numeroLote,
                                   Integer cantidad, Integer unidadesPorCaja, BigDecimal precioCompra) {}
    public record CompraRes(Long id, LocalDateTime fechaCompra, String proveedorNombre, String usuarioNombre,
                            String tipoComprobante, String numeroComprobante, BigDecimal total,
                            List<DetalleCompraRes> detalles) {}
}

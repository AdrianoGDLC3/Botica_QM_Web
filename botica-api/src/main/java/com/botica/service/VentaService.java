package com.botica.service;

import com.botica.builder.VentaBuilder;
import com.botica.builder.VentaDetalleBuilder;
import com.botica.comprobante.GeneradorComprobanteFactory;
import com.botica.config.ConfiguracionNegocio;
import com.botica.domain.*;
import com.botica.dto.Dtos.*;
import com.botica.error.ApiException;
import com.botica.inventario.ObservadorInventario;
import com.botica.repo.*;
import com.botica.util.Tiempo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VentaService {
    private final VentaRepository ventas;
    private final LoteRepository lotes;
    private final UsuarioRepository usuarios;
    private final GeneradorComprobanteFactory comprobantes; // Abstract Factory + Decorator (ver comprobante/)
    private final List<ObservadorInventario> observadores;  // Observer: Spring inyecta todos los @Component

    // "cantidad" está en la unidad_venta del producto (ej. tabletas sueltas), que es la misma
    // unidad en la que vive lote.stockActual, así que aquí no hace falta ninguna conversión:
    // eso solo pasa al comprar (unidad_compra -> unidad_venta).
    @Transactional
    public VentaRes registrar(VentaReq req, Long empleadoId) {
        Usuario empleado = usuarios.findById(empleadoId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));

        Usuario cliente = null;
        if (req.clienteId() != null) {
            cliente = usuarios.findById(req.clienteId())
                    .filter(u -> u.getRol() == Rol.CLIENTE && Boolean.TRUE.equals(u.getActivo()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Cliente no válido"));
        }

        // Une líneas repetidas del mismo lote y bloquea en orden de id (evita deadlocks)
        Map<Long, Integer> pedido = new TreeMap<>();
        req.items().forEach(i -> pedido.merge(i.loteId(), i.cantidad(), Integer::sum));

        String tipo = blank(req.tipoComprobante()) ? "BOLETA" : req.tipoComprobante().trim().toUpperCase();
        // Builder: la cabecera se arma paso a paso y la entidad se crea al final, en build()
        VentaBuilder venta = VentaBuilder.nueva()
                .cliente(cliente)
                .empleado(empleado)
                .fechaVenta(Tiempo.ahora())
                .tipoComprobante(tipo)
                .metodoPago(req.metodoPago())
                .receta(req.recetaPresentada(), req.recetaNumero(), req.recetaMedico());
        boolean recetaPresentada = Boolean.TRUE.equals(req.recetaPresentada());
        BigDecimal umbralStockBajo = ConfiguracionNegocio.instancia().umbralStockBajo(); // Singleton

        List<String> conReceta = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var e : pedido.entrySet()) {
            Lote lote = lotes.bloquear(e.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El lote " + e.getKey() + " no existe"));
            Producto prod = lote.getProducto();
            String etiqueta = prod.getNombre() + " (lote " + lote.getNumeroLote() + ")";

            if (!Boolean.TRUE.equals(prod.getActivo()))
                throw new ApiException(HttpStatus.CONFLICT, prod.getNombre() + " está desactivado");
            if (!lote.getFechaVencimiento().isAfter(Tiempo.hoy()))
                throw new ApiException(HttpStatus.CONFLICT, etiqueta + " está vencido");

            BigDecimal unidadesNecesarias = BigDecimal.valueOf(e.getValue());
            if (lote.getStockActual().compareTo(unidadesNecesarias) < 0)
                throw new ApiException(HttpStatus.CONFLICT,
                        "Stock insuficiente de " + etiqueta + ": disponible " + lote.getStockActual()
                                + " " + prod.getUnidadVenta().toLowerCase());

            lote.setStockActual(lote.getStockActual().subtract(unidadesNecesarias));
            if (lote.getStockActual().compareTo(umbralStockBajo) <= 0)
                observadores.forEach(o -> o.onStockBajo(lote));

            // El precio lo pone el servidor (se congela en el detalle), siempre el único precio del producto
            BigDecimal precio = prod.getPrecioVenta();
            boolean exigeReceta = Boolean.TRUE.equals(prod.getRequiereReceta());
            venta.agregarDetalle(VentaDetalleBuilder.nuevo()
                    .lote(lote)
                    .cantidad(e.getValue())
                    .precioVenta(precio)
                    .requiereReceta(exigeReceta)
                    .build());
            if (exigeReceta && !conReceta.contains(prod.getNombre())) conReceta.add(prod.getNombre());
            total = total.add(precio.multiply(BigDecimal.valueOf(e.getValue())));
        }
        // Si algún producto exige receta, el vendedor debe confirmar que el cliente la presenta.
        // Al lanzar la excepción se revierte toda la transacción (incluidos los descuentos de stock).
        if (!conReceta.isEmpty() && !recetaPresentada)
            throw new ApiException(HttpStatus.CONFLICT,
                    "Requiere receta: " + String.join(", ", conReceta) + ". Confirma que el cliente la presenta.");

        // Si el front no manda número, la Abstract Factory entrega el generador de la serie que
        // corresponda (correlativo real por serie, no el id interno de la venta — ver comprobante/).
        Venta v = venta
                .numeroComprobante(blank(req.numeroComprobante())
                        ? comprobantes.paraTipo(tipo).siguienteNumero()
                        : req.numeroComprobante().trim())
                .total(total.setScale(2, RoundingMode.HALF_UP))
                .build();
        ventas.save(v);
        return res(v, true);
    }

    @Transactional(readOnly = true)
    public List<VentaRes> listar() {
        return ventas.listar().stream().map(v -> res(v, false)).toList();
    }

    @Transactional(readOnly = true)
    public VentaRes detalle(Long id) {
        return ventas.detalle(id).map(v -> res(v, true))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private VentaRes res(Venta v, boolean conDetalle) {
        List<DetalleVentaRes> det = conDetalle
                ? v.getDetalles().stream().map(d -> new DetalleVentaRes(
                        d.getLote().getProducto().getNombre(),
                        d.getLote().getNumeroLote(),
                        d.getCantidad(), d.getPrecioVenta(), d.getRequiereReceta())).toList()
                : null;
        return new VentaRes(v.getId(), v.getFechaVenta(),
                v.getCliente() == null ? null : v.getCliente().getNombre(),
                v.getEmpleado().getNombre(), v.getTipoComprobante(), v.getNumeroComprobante(),
                v.getMetodoPago(), v.getRecetaPresentada(), v.getRecetaNumero(), v.getRecetaMedico(),
                v.getTotal(), det);
    }
}

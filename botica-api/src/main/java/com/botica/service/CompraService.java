package com.botica.service;

import com.botica.builder.CompraBuilder;
import com.botica.builder.CompraDetalleBuilder;
import com.botica.builder.LoteBuilder;
import com.botica.domain.*;
import com.botica.dto.Dtos.*;
import com.botica.error.ApiException;
import com.botica.repo.*;
import com.botica.util.Tiempo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraService {
    private final CompraRepository compras;
    private final LoteRepository lotes;
    private final ProveedorRepository provs;
    private final ProductoRepository productos;
    private final LaboratorioRepository labs;
    private final UsuarioRepository usuarios;

    // Entrada de mercadería: crea el lote si no existe (producto + número) o le suma stock.
    // Cada línea trae: cajas compradas (cantidad), unidades por caja EN ESTA compra, precio por
    // caja, lote y vencimiento. El servidor suma al stock cantidad × unidadesPorCaja (en unidades);
    // la venta siempre es por unidad. Cada lote queda ligado al laboratorio que efectivamente lo
    // fabricó (debe ser uno de los ya asociados al producto).
    @Transactional
    public CompraRes registrar(CompraReq req, Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));
        Proveedor prov = provs.findById(req.proveedorId())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Proveedor no válido"));

        // Builder: cabecera y detalles se acumulan y la entidad se crea al final, en build()
        CompraBuilder compra = CompraBuilder.nueva()
                .proveedor(prov)
                .usuario(usuario)
                .fechaCompra(Tiempo.ahora())
                .tipoComprobante(req.tipoComprobante())
                .numeroComprobante(req.numeroComprobante());

        BigDecimal total = BigDecimal.ZERO;
        for (CompraItemReq i : req.items()) {
            Producto prod = productos.findById(i.productoId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El producto " + i.productoId() + " no existe"));

            Laboratorio lab = labs.findById(i.laboratorioId())
                    .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El laboratorio no existe o está inactivo"));
            boolean asociado = prod.getLaboratorios().stream().anyMatch(x -> x.getId().equals(lab.getId()));
            if (!asociado)
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        lab.getNombre() + " no está registrado como fabricante de " + prod.getNombre()
                                + ". Asócialo primero desde Productos.");

            String numero = i.numeroLote().trim();
            Lote lote = lotes.bloquearPorNumero(prod.getId(), numero).orElse(null);
            BigDecimal unidadesVenta = BigDecimal.valueOf((long) i.cantidad() * i.unidadesPorCaja());

            if (lote == null) {
                if (!i.fechaVencimiento().isAfter(Tiempo.hoy()))
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "El lote " + numero + " de " + prod.getNombre() + " ya está vencido");
                lote = LoteBuilder.nuevo()
                        .producto(prod)
                        .laboratorio(lab)
                        .numeroLote(numero)
                        .fechaVencimiento(i.fechaVencimiento())
                        .stockActual(unidadesVenta)
                        .build();
                lotes.save(lote);
            } else {
                if (!lote.getFechaVencimiento().equals(i.fechaVencimiento()))
                    throw new ApiException(HttpStatus.CONFLICT, "El lote " + numero + " de " + prod.getNombre()
                            + " ya existe con otra fecha de vencimiento (" + lote.getFechaVencimiento() + ")");
                if (!lote.getLaboratorio().getId().equals(lab.getId()))
                    throw new ApiException(HttpStatus.CONFLICT, "El lote " + numero + " de " + prod.getNombre()
                            + " ya está registrado con otro laboratorio (" + lote.getLaboratorio().getNombre() + ")");
                lote.setStockActual(lote.getStockActual().add(unidadesVenta));
            }

            compra.agregarDetalle(CompraDetalleBuilder.nuevo()
                    .lote(lote)
                    .cantidad(i.cantidad())
                    .unidadesPorCaja(i.unidadesPorCaja())
                    .precioCompra(i.precioCompra())
                    .build());
            total = total.add(i.precioCompra().multiply(BigDecimal.valueOf(i.cantidad())));
        }
        Compra c = compra.total(total.setScale(2, RoundingMode.HALF_UP)).build();
        compras.save(c);
        return res(c, true);
    }

    @Transactional(readOnly = true)
    public List<CompraRes> listar() {
        return compras.listar().stream().map(c -> res(c, false)).toList();
    }

    @Transactional(readOnly = true)
    public CompraRes detalle(Long id) {
        return compras.detalle(id).map(c -> res(c, true))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
    }

    private CompraRes res(Compra c, boolean conDetalle) {
        List<DetalleCompraRes> det = conDetalle
                ? c.getDetalles().stream().map(d -> new DetalleCompraRes(
                        d.getLote().getProducto().getNombre(),
                        d.getLote().getLaboratorio().getNombre(),
                        d.getLote().getNumeroLote(),
                        d.getCantidad(), d.getUnidadesPorCaja(), d.getPrecioCompra())).toList()
                : null;
        return new CompraRes(c.getId(), c.getFechaCompra(), c.getProveedor().getNombre(), c.getUsuario().getNombre(),
                c.getTipoComprobante(), c.getNumeroComprobante(), c.getTotal(), det);
    }
}

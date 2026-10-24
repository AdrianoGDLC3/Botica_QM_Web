package com.botica.service;

import com.botica.config.ConfiguracionNegocio;
import com.botica.domain.*;
import com.botica.dto.Dtos.*;
import com.botica.error.ApiException;
import com.botica.repo.*;
import com.botica.lotes.EstrategiaAsignacionLote;
import com.botica.util.Textos;
import com.botica.util.Tiempo;
import com.botica.validacion.RegistroValidadoresAtributos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// CRUD de usuarios, productos y lotes. Laboratorios y proveedores tienen su propio servicio
// (LaboratorioService / ProveedorService), que comparte el flujo vía ServicioCatalogoContacto.
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogoService {
    private final UsuarioRepository usuarios;
    private final LaboratorioRepository labs; // se usa para asociar laboratorios a un producto/lote
    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final PasswordEncoder encoder;
    private final RegistroValidadoresAtributos validadoresAtributos; // Strategy + Singleton
    private final EstrategiaAsignacionLote estrategiaLote; // Strategy (FEFO)

    private static ApiException nf(String what) { return new ApiException(HttpStatus.NOT_FOUND, what + " no encontrado"); }

    // ======================= USUARIOS =======================
    private UsuarioRes res(Usuario u) {
        return new UsuarioRes(u.getId(), u.getNombre(), u.getDni(), u.getTelefono(), u.getCorreo(), u.getRol(), u.getActivo());
    }

    private void aplicar(Usuario u, UsuarioReq r) {
        u.setNombre(r.nombre().trim());
        u.setDni(Textos.opcional(r.dni()));
        u.setTelefono(Textos.opcional(r.telefono()));
        u.setCorreo(r.correo().trim().toLowerCase());
        u.setRol(r.rol());
        u.setActivo(r.activo() == null || r.activo());
    }

    @Transactional(readOnly = true)
    public List<UsuarioRes> usuarios() {
        return usuarios.findAll(Sort.by("nombre")).stream().map(this::res).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioRes> clientes() {
        return usuarios.findByRolAndActivoTrueOrderByNombre(Rol.CLIENTE).stream().map(this::res).toList();
    }

    public UsuarioRes crearUsuario(UsuarioReq r) {
        if (r.password() == null || r.password().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        Usuario u = new Usuario();
        aplicar(u, r);
        u.setPassword(encoder.encode(r.password()));
        return res(usuarios.saveAndFlush(u));
    }

    public UsuarioRes actualizarUsuario(Long id, UsuarioReq r) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> nf("Usuario"));
        aplicar(u, r);
        if (r.password() != null && !r.password().isBlank()) u.setPassword(encoder.encode(r.password()));
        return res(usuarios.saveAndFlush(u));
    }

    public void desactivarUsuario(Long id, Long actualId) {
        if (id.equals(actualId)) throw new ApiException(HttpStatus.CONFLICT, "No puedes desactivar tu propio usuario");
        Usuario u = usuarios.findById(id).orElseThrow(() -> nf("Usuario"));
        u.setActivo(false);
    }

    // ======================= PRODUCTOS =======================
    private ProductoRes res(Producto p) {
        return new ProductoRes(p.getId(), p.getNombre(), p.getDescripcion(), p.getCategoria(), p.getRequiereReceta(),
                p.getAtributos(), p.getPrecioVenta(),
                p.getUnidadVenta(), p.getActivo(),
                p.getLaboratorios().stream().map(l -> new LabRes(l.getId(), l.getNombre())).toList());
    }

    private void aplicar(Producto p, ProductoReq r) {
        String categoria = r.categoria().trim().toUpperCase();
        if (!ConfiguracionNegocio.instancia().esCategoriaValida(categoria)) // Singleton
            throw new ApiException(HttpStatus.BAD_REQUEST, "Categoría no válida: " + categoria);

        Map<String, Object> atributos = r.atributos() == null ? Map.of() : r.atributos();
        validadoresAtributos.validar(categoria, atributos); // lanza ApiException si algo no calza

        p.setNombre(r.nombre().trim());
        p.setDescripcion(Textos.opcional(r.descripcion()));
        p.setCategoria(categoria);
        p.setRequiereReceta(Boolean.TRUE.equals(r.requiereReceta()));
        p.setAtributos(atributos);
        p.setPrecioVenta(r.precioVenta());
        p.setUnidadVenta(r.unidadVenta().trim().toUpperCase());
        p.setActivo(r.activo() == null || r.activo());

        Set<Long> ids = r.laboratorioIds() == null ? Set.of() : new HashSet<>(r.laboratorioIds());
        List<Laboratorio> encontrados = labs.findAllById(ids);
        if (encontrados.size() != ids.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "Algún laboratorio no existe");
        p.getLaboratorios().clear();
        p.getLaboratorios().addAll(encontrados);
    }

    @Transactional(readOnly = true)
    public List<ProductoRes> productos() {
        return productos.findAllByOrderByNombreAsc().stream().map(this::res).toList();
    }

    public ProductoRes crearProducto(ProductoReq r) {
        Producto p = new Producto();
        aplicar(p, r);
        return res(productos.save(p));
    }

    public ProductoRes actualizarProducto(Long id, ProductoReq r) {
        Producto p = productos.findById(id).orElseThrow(() -> nf("Producto"));
        aplicar(p, r);
        return res(productos.saveAndFlush(p));
    }

    // Prototype: crea un producto nuevo a partir de uno existente (ver Producto.clonar()).
    // Queda con el nombre "<original> (copia)" para que el usuario lo edite después.
    public ProductoRes duplicarProducto(Long id) {
        Producto copia = productos.findById(id).orElseThrow(() -> nf("Producto")).clonar();
        copia.setNombre(copia.getNombre() + " (copia)");
        return res(productos.save(copia));
    }

    public void desactivarProducto(Long id) {
        productos.findById(id).orElseThrow(() -> nf("Producto")).setActivo(false);
    }

    // Eliminación definitiva. Un producto que ya tiene lotes tiene también compras (los lotes nacen
    // de una compra) y quizá ventas; borrarlo obligaría a borrar ese historial. Por eso solo se
    // elimina un producto SIN lotes (p. ej. uno creado por error). Si tiene lotes, se conserva
    // todo y se avisa cuánto stock queda; la salida es desactivarlo, que lo saca de las ventas
    // pero deja sus lotes y movimientos como registro.
    public void eliminarProducto(Long id) {
        Producto p = productos.findById(id).orElseThrow(() -> nf("Producto"));
        long n = lotes.contarPorProducto(id);
        if (n > 0) {
            BigDecimal stock = lotes.stockTotalDeProducto(id);
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar «" + p.getNombre() + "»: tiene "
                    + n + (n == 1 ? " lote" : " lotes") + " con historial de compras o ventas"
                    + (stock.signum() > 0 ? " y aún quedan " + stock.stripTrailingZeros().toPlainString() + " "
                            + p.getUnidadVenta().toLowerCase() + " en stock" : "")
                    + ". Desactívalo para que deje de venderse; sus lotes y movimientos se conservan.");
        }
        productos.delete(p); // también quita sus filas de producto_laboratorio (el dueño de la relación es Producto)
        productos.flush();
    }

    // ======================= LOTES =======================
    private LoteRes res(Lote l) {
        Laboratorio lab = l.getLaboratorio();
        return new LoteRes(l.getId(), l.getProducto().getId(), l.getProducto().getNombre(),
                lab == null ? null : lab.getId(), lab == null ? null : lab.getNombre(),
                l.getNumeroLote(), l.getFechaVencimiento(), l.getStockActual());
    }

    @Transactional(readOnly = true)
    public List<LoteRes> lotes() { return lotes.listar().stream().map(this::res).toList(); }

    @Transactional(readOnly = true)
    public List<LoteRes> lotesDisponibles(Long productoId) {
        List<Lote> candidatos = lotes.disponibles(productoId, Tiempo.hoy());
        return estrategiaLote.ordenar(candidatos).stream().map(this::res).toList();
    }

    // Solo se corrige número, vencimiento y (si hace falta) el laboratorio del lote;
    // el stock cambia únicamente con compras y ventas.
    public LoteRes actualizarLote(Long id, LoteReq r) {
        Lote l = lotes.findById(id).orElseThrow(() -> nf("Lote"));
        l.setNumeroLote(r.numeroLote().trim());
        l.setFechaVencimiento(r.fechaVencimiento());
        if (r.laboratorioId() != null) {
            Laboratorio lab = labs.findById(r.laboratorioId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El laboratorio no existe"));
            l.setLaboratorio(lab);
        }
        return res(lotes.saveAndFlush(l));
    }
}

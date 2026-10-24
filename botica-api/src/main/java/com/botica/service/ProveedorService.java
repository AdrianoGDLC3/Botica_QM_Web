package com.botica.service;

import com.botica.domain.Proveedor;
import com.botica.dto.Dtos.ProveedorReq;
import com.botica.error.ApiException;
import com.botica.repo.CompraRepository;
import com.botica.repo.ProveedorRepository;
import com.botica.util.Textos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProveedorService extends ServicioCatalogoContacto<Proveedor, Long, ProveedorReq> {
    private final ProveedorRepository repo;
    private final CompraRepository compras;

    public ProveedorService(ProveedorRepository repo, CompraRepository compras) {
        this.repo = repo;
        this.compras = compras;
    }

    @Override protected JpaRepository<Proveedor, Long> repo() { return repo; }
    @Override protected Proveedor nuevaEntidad() { return new Proveedor(); }
    @Override protected String nombreEntidad() { return "Proveedor"; }

    // Las compras apuntan al proveedor: borrarlo dejaría compras sin saber a quién se compró
    @Override
    protected void verificarSinUso(Proveedor p) {
        long n = compras.contarPorProveedor(p.getId());
        if (n > 0)
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar a «" + p.getNombre() + "»: tiene " + n
                    + (n == 1 ? " compra registrada" : " compras registradas")
                    + ". Para que deje de aparecer al comprar, desactívalo; el historial se conserva.");
    }

    @Override
    protected void aplicar(Proveedor p, ProveedorReq r) {
        p.setNombre(r.nombre().trim());
        p.setRuc(Textos.opcional(r.ruc()));
        p.setContacto(Textos.opcional(r.contacto()));
        p.setTelefono(Textos.opcional(r.telefono()));
        p.setCorreo(Textos.opcional(r.correo()));
        p.setDireccion(Textos.opcional(r.direccion()));
        p.setActivo(r.activo() == null || r.activo());
    }
}

package com.botica.service;

import com.botica.domain.Laboratorio;
import com.botica.dto.Dtos.LaboratorioReq;
import com.botica.error.ApiException;
import com.botica.repo.LaboratorioRepository;
import com.botica.repo.LoteRepository;
import com.botica.util.Textos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LaboratorioService extends ServicioCatalogoContacto<Laboratorio, Long, LaboratorioReq> {
    private final LaboratorioRepository repo;
    private final LoteRepository lotes;

    public LaboratorioService(LaboratorioRepository repo, LoteRepository lotes) {
        this.repo = repo;
        this.lotes = lotes;
    }

    @Override protected JpaRepository<Laboratorio, Long> repo() { return repo; }
    @Override protected Laboratorio nuevaEntidad() { return new Laboratorio(); }
    @Override protected String nombreEntidad() { return "Laboratorio"; }

    // Cada lote dice qué laboratorio lo fabricó: mientras exista alguno, el laboratorio es historial
    @Override
    protected void verificarSinUso(Laboratorio l) {
        long n = lotes.contarPorLaboratorio(l.getId());
        if (n > 0)
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar «" + l.getNombre() + "»: es el fabricante de "
                    + n + (n == 1 ? " lote" : " lotes")
                    + ". Para que deje de ofrecerse, desactívalo; los lotes conservan su laboratorio.");
    }

    // Que figure como fabricante posible de un producto no es historial: simplemente se quita de esas listas
    @Override
    protected void liberarVinculos(Laboratorio l) {
        repo.desvincularDeProductos(l.getId());
    }

    @Override
    protected void aplicar(Laboratorio l, LaboratorioReq r) {
        l.setNombre(r.nombre().trim());
        l.setPais(Textos.opcional(r.pais()));
        l.setRuc(Textos.opcional(r.ruc()));
        l.setContacto(Textos.opcional(r.contacto()));
        l.setTelefono(Textos.opcional(r.telefono()));
        l.setCorreo(Textos.opcional(r.correo()));
        l.setDireccion(Textos.opcional(r.direccion()));
        l.setActivo(r.activo() == null || r.activo());
    }
}

package com.botica.service;

import com.botica.adapter.EntidadContactoAdapter;
import com.botica.domain.EntidadContacto;
import com.botica.dto.Dtos.EntidadContactoRes;
import com.botica.error.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Patrón Template Method: Laboratorio y Proveedor (y cualquier catálogo de contacto que se
// agregue después) comparten el mismo flujo — listar ordenado por nombre, crear, actualizar,
// dar de baja lógica —, así que ese flujo se escribe UNA sola vez aquí. Cada subclase solo
// aporta su repositorio, cómo crear una entidad vacía y cómo volcar el request sobre ella
// (aplicar): ver LaboratorioService y ProveedorService.
//
// Las respuestas se adaptan a EntidadContactoRes (patrón Adapter, ver EntidadContactoAdapter)
// en vez de devolver la entidad JPA tal cual, para no exponer detalles de persistencia en la API.
public abstract class ServicioCatalogoContacto<E extends EntidadContacto, ID, Req> {

    protected abstract JpaRepository<E, ID> repo();
    protected abstract E nuevaEntidad();
    protected abstract void aplicar(E entidad, Req req);
    protected abstract String nombreEntidad(); // para el mensaje de "no encontrado"

    // Pasos propios de la eliminación definitiva: cada catálogo sabe qué registros dependen de él.
    // verificarSinUso lanza ApiException(CONFLICT) si todavía hay historial que se perdería;
    // liberarVinculos suelta lo que NO es historial (ej. la lista de fabricantes de un producto).
    protected abstract void verificarSinUso(E entidad);
    protected void liberarVinculos(E entidad) {}

    @Transactional(readOnly = true)
    public List<EntidadContactoRes> listar() {
        return repo().findAll(Sort.by("nombre")).stream().map(EntidadContactoAdapter::adaptar).toList();
    }

    @Transactional
    public EntidadContactoRes crear(Req req) {
        E entidad = nuevaEntidad();
        aplicar(entidad, req);
        return EntidadContactoAdapter.adaptar(repo().saveAndFlush(entidad));
    }

    @Transactional
    public EntidadContactoRes actualizar(ID id, Req req) {
        E entidad = buscar(id);
        aplicar(entidad, req);
        return EntidadContactoAdapter.adaptar(repo().saveAndFlush(entidad));
    }

    @Transactional
    public void desactivar(ID id) {
        buscar(id).setActivo(false);
    }

    // Eliminación definitiva (a diferencia de desactivar, que solo marca activo = false). Solo se
    // permite si nada del historial depende del registro: si no, se conserva y se sugiere desactivar.
    @Transactional
    public void eliminar(ID id) {
        E entidad = buscar(id);
        verificarSinUso(entidad);
        liberarVinculos(entidad);
        repo().delete(entidad);
        repo().flush(); // que cualquier restricción de la BD que no previmos falle aquí, dentro del método
    }

    protected E buscar(ID id) {
        return repo().findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, nombreEntidad() + " no encontrado"));
    }
}

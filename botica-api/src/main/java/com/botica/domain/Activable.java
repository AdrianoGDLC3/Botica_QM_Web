package com.botica.domain;

// Cualquier catálogo con baja lógica (activo = false en vez de borrar). La implementan
// Laboratorio, Proveedor, Producto y Usuario. La usa ServicioCatalogoContacto para poder
// desactivar sin conocer la entidad concreta.
public interface Activable {
    Boolean getActivo();
    void setActivo(Boolean activo);
}

package com.botica.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

// Campos comunes a cualquier catálogo de contacto: a quién se le compra (Proveedor) o quién
// fabrica (Laboratorio), con los mismos datos de contacto y la misma baja lógica. Evita repetir
// estos 7 campos en cada entidad nueva de este tipo; ver ServicioCatalogoContacto para el CRUD.
@MappedSuperclass
@Getter @Setter
public abstract class EntidadContacto implements Activable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String ruc;
    private String contacto;
    private String telefono;
    private String correo;
    private String direccion;
    private Boolean activo = true;

    // Hook para el Adapter (ver adapter.EntidadContactoAdapter): por defecto ninguna entidad de
    // contacto tiene país; Laboratorio lo sobreescribe devolviendo su propio campo.
    public String paisParaAdaptar() { return null; }
}

package com.botica.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Sin campos propios: es EntidadContacto tal cual (nombre, ruc, contacto, teléfono, correo,
// dirección, activo). Si mañana necesita algo propio (ej. condición de pago), se agrega aquí.
@Entity
@Table(name = "proveedor")
public class Proveedor extends EntidadContacto {
}

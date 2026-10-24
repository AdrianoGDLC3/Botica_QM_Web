package com.botica.adapter;

import com.botica.domain.EntidadContacto;
import com.botica.dto.Dtos.EntidadContactoRes;

// Adapter: convierte la entidad de persistencia (JPA, con proxies y detalles internos de
// Hibernate) a la forma que expone la API. Laboratorio y Proveedor son dos clases distintas,
// pero ambas se adaptan al mismo contrato de salida, así que el resto del código (y el
// frontend) no necesita saber que son clases diferentes.
public final class EntidadContactoAdapter {
    private EntidadContactoAdapter() {}

    public static EntidadContactoRes adaptar(EntidadContacto e) {
        return new EntidadContactoRes(e.getId(), e.getNombre(), e.getRuc(), e.getContacto(),
                e.getTelefono(), e.getCorreo(), e.getDireccion(), e.paisParaAdaptar(), e.getActivo());
    }
}

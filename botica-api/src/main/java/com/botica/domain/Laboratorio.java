package com.botica.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "laboratorio")
@Getter @Setter
public class Laboratorio extends EntidadContacto {
    private String pais; // lo único propio de Laboratorio; el resto vive en EntidadContacto

    @Override
    public String paisParaAdaptar() { return getPais(); }
}

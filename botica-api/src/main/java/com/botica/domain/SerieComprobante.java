package com.botica.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Un correlativo por serie de comprobante (B001, F001...). Solo se toca mediante
// SerieComprobanteRepository.incrementarYObtener (UPDATE atómico); no se usa save()/find() normal.
@Entity
@Table(name = "serie_comprobante")
@Getter @Setter
public class SerieComprobante {
    @Id
    private String serie;
    private Long correlativo = 0L;
}

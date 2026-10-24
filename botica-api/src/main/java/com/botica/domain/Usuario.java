package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter @Setter
public class Usuario implements Activable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String dni;
    private String telefono;
    private String correo;
    private String password;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    private Boolean activo = true;
}

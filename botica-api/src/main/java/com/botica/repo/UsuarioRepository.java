package com.botica.repo;

import com.botica.domain.Rol;
import com.botica.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    List<Usuario> findByRolAndActivoTrueOrderByNombre(Rol rol);
}

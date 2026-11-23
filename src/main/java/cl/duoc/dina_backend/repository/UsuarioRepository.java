package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.Rol;
import cl.duoc.dina_backend.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    List<Usuario> findByRolAndActivoTrue(Rol rol);
}

package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.BloqueHorario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Long> {

    List<BloqueHorario> findAllByOrderByNumeroAsc();
}

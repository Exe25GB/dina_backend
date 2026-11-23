package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.Mensaje;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** Mensajes de un chat con id mayor a [despuesDe] (0 = todos), del más antiguo al más nuevo. */
    @EntityGraph(attributePaths = {"autor"})
    List<Mensaje> findBySolicitudIdAndIdGreaterThanOrderByIdAsc(Long solicitudId, Long despuesDe);

    @EntityGraph(attributePaths = {"autor"})
    Optional<Mensaje> findFirstBySolicitudIdOrderByIdDesc(Long solicitudId);
}

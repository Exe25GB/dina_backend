package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.EstadoSolicitud;
import cl.duoc.dina_backend.model.Solicitud;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    @EntityGraph(attributePaths = {"espacio", "usuario", "bloques"})
    List<Solicitud> findByUsuarioIdOrderByFechaDescCreadaEnDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"espacio", "usuario", "bloques"})
    List<Solicitud> findByEstadoOrderByFechaAscCreadaEnAsc(EstadoSolicitud estado);

    @EntityGraph(attributePaths = {"espacio", "usuario", "bloques"})
    List<Solicitud> findAllByOrderByFechaDescCreadaEnDesc();

    /** Lee la solicitud bloqueando su fila hasta terminar la transacción (revisión por un admin). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.id = :id")
    Optional<Solicitud> findByIdParaActualizar(@Param("id") Long id);

    /** Solicitudes de un espacio y fecha en ciertos estados (para armar la disponibilidad). */
    @EntityGraph(attributePaths = {"bloques"})
    List<Solicitud> findByEspacioIdAndFechaAndEstadoIn(Long espacioId, LocalDate fecha,
                                                       Collection<EstadoSolicitud> estados);

    /**
     * Solicitudes que comparten al menos un bloque con los indicados, en el mismo espacio y fecha.
     * [excluirId] permite ignorar la propia solicitud (usar -1 si no aplica).
     */
    @Query("""
            select distinct s from Solicitud s join s.bloques b
            where s.espacio.id = :espacioId
              and s.fecha = :fecha
              and s.estado in :estados
              and b.id in :bloqueIds
              and s.id <> :excluirId
            """)
    List<Solicitud> buscarCruces(@Param("espacioId") Long espacioId,
                                 @Param("fecha") LocalDate fecha,
                                 @Param("bloqueIds") Collection<Long> bloqueIds,
                                 @Param("estados") Collection<EstadoSolicitud> estados,
                                 @Param("excluirId") Long excluirId);

    /** Lo mismo, pero solo de un usuario (evita que pida dos veces el mismo horario). */
    @Query("""
            select count(distinct s) > 0 from Solicitud s join s.bloques b
            where s.usuario.id = :usuarioId
              and s.fecha = :fecha
              and s.estado in :estados
              and b.id in :bloqueIds
            """)
    boolean usuarioTieneCruce(@Param("usuarioId") Long usuarioId,
                              @Param("fecha") LocalDate fecha,
                              @Param("bloqueIds") Collection<Long> bloqueIds,
                              @Param("estados") Collection<EstadoSolicitud> estados);
}

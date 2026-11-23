package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.Notificacion;
import cl.duoc.dina_backend.model.TipoNotificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);

    /** El aviso "mensaje nuevo" sin leer de un chat (para actualizarlo en vez de crear otro). */
    Optional<Notificacion> findFirstByUsuarioIdAndTipoAndSolicitudIdAndLeidaFalse(
            Long usuarioId, TipoNotificacion tipo, Long solicitudId);

    /** Avisos sin leer de un tipo y una solicitud (p. ej. "mensaje nuevo" de un chat). */
    List<Notificacion> findByUsuarioIdAndSolicitudIdAndTipoAndLeidaFalse(
            Long usuarioId, Long solicitudId, TipoNotificacion tipo);

    @Modifying
    @Query("delete from Notificacion n where n.usuario.id = :usuarioId")
    int borrarTodas(@Param("usuarioId") Long usuarioId);
}

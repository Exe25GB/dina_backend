package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.config.DinaProperties;
import cl.duoc.dina_backend.dto.Dtos.NotificacionDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Notificacion;
import cl.duoc.dina_backend.model.Rol;
import cl.duoc.dina_backend.model.TipoNotificacion;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.NotificacionRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avisos dentro de la app. La app los consulta en /api/notificaciones.
 * Más adelante, aquí mismo se puede enviar además un push (Firebase Cloud Messaging).
 */
@Service
public class NotificacionService {

    private final NotificacionRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final DinaProperties props;

    public NotificacionService(NotificacionRepository repository, UsuarioRepository usuarioRepository,
                               DinaProperties props) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.props = props;
    }

    @Transactional
    public void notificar(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje, Long solicitudId) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setSolicitudId(solicitudId);
        n.setCreadaEn(LocalDateTime.now(props.zona()));
        repository.save(n);
    }

    @Transactional
    public void notificarAdmins(TipoNotificacion tipo, String titulo, String mensaje, Long solicitudId) {
        usuarioRepository.findByRolAndActivoTrue(Rol.ADMIN)
                .forEach(admin -> notificar(admin, tipo, titulo, mensaje, solicitudId));
    }

    @Transactional(readOnly = true)
    public List<NotificacionDto> listar(Usuario usuario) {
        return repository.findByUsuarioIdOrderByCreadaEnDesc(usuario.getId()).stream()
                .map(NotificacionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Usuario usuario) {
        return repository.countByUsuarioIdAndLeidaFalse(usuario.getId());
    }

    @Transactional
    public NotificacionDto marcarLeida(Usuario usuario, Long id) {
        Notificacion n = repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> ApiException.noEncontrado("Notificación no encontrada"));
        n.setLeida(true);
        return NotificacionDto.de(n);
    }

    @Transactional
    public int marcarTodasLeidas(Usuario usuario) {
        return repository.marcarTodasLeidas(usuario.getId());
    }

    @Transactional
    public void borrar(Usuario usuario, Long id) {
        Notificacion n = repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> ApiException.noEncontrado("Notificación no encontrada"));
        repository.delete(n);
    }

    @Transactional
    public int borrarTodas(Usuario usuario) {
        return repository.borrarTodas(usuario.getId());
    }

    /**
     * "Mensaje nuevo en el chat": un solo aviso por chat mientras no se lea. Si ya hay uno sin leer,
     * se actualiza con el último mensaje y sube al principio de la lista (en vez de llenarla).
     */
    @Transactional
    public void avisarMensajeNuevo(Usuario destinatario, Long solicitudId, String titulo, String mensaje) {
        Notificacion n = repository
                .findFirstByUsuarioIdAndTipoAndSolicitudIdAndLeidaFalse(
                        destinatario.getId(), TipoNotificacion.MENSAJE_NUEVO, solicitudId)
                .orElse(null);
        if (n == null) {
            notificar(destinatario, TipoNotificacion.MENSAJE_NUEVO, titulo, mensaje, solicitudId);
        } else {
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setCreadaEn(LocalDateTime.now(props.zona()));
        }
    }

    /** Al abrir un chat, sus avisos de "mensaje nuevo" quedan leídos. */
    @Transactional
    public void marcarLeidosLosMensajesDe(Usuario usuario, Long solicitudId) {
        // Uno a uno (no un UPDATE masivo): así los avisos ya cargados en esta transacción quedan al día
        repository.findByUsuarioIdAndSolicitudIdAndTipoAndLeidaFalse(
                        usuario.getId(), solicitudId, TipoNotificacion.MENSAJE_NUEVO)
                .forEach(n -> n.setLeida(true));
    }
}

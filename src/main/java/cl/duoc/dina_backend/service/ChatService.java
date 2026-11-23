package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.config.DinaProperties;
import cl.duoc.dina_backend.dto.Dtos.ChatDetalleDto;
import cl.duoc.dina_backend.dto.Dtos.ChatDto;
import cl.duoc.dina_backend.dto.Dtos.MensajeDto;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.EstadoChat;
import cl.duoc.dina_backend.model.Mensaje;
import cl.duoc.dina_backend.model.Rol;
import cl.duoc.dina_backend.model.Solicitud;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.MensajeRepository;
import cl.duoc.dina_backend.repository.SolicitudRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat de cada solicitud aprobada: el usuario que la pidió y los administradores.
 * - Existe desde que la solicitud se aprueba (las pendientes y rechazadas no tienen chat).
 * - Está ABIERTO hasta que termina el horario de la reserva o hasta que un administrador lo cierra.
 *   Si la reserva se cancela, sigue abierto para aclarar lo que haga falta.
 */
@Service
public class ChatService {

    private final SolicitudRepository solicitudRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificaciones;
    private final DinaProperties props;

    public ChatService(SolicitudRepository solicitudRepository, MensajeRepository mensajeRepository,
                       UsuarioRepository usuarioRepository, NotificacionService notificaciones,
                       DinaProperties props) {
        this.solicitudRepository = solicitudRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificaciones = notificaciones;
        this.props = props;
    }

    /** Chats del usuario (los de todos, si es administrador). Primero los de actividad más reciente. */
    @Transactional(readOnly = true)
    public List<ChatDto> listar(Usuario usuario) {
        List<Solicitud> conChat = usuario.esAdmin()
                ? solicitudRepository.findByAprobadaEnIsNotNull()
                : solicitudRepository.findByUsuarioIdAndAprobadaEnIsNotNull(usuario.getId());
        return conChat.stream()
                .map(this::aDto)
                .sorted(Comparator.comparing(ChatService::ultimaActividad).reversed())
                .toList();
    }

    /**
     * El chat con sus mensajes posteriores a [despuesDe] (0 = todos): así la app pide solo los nuevos.
     * Quien lo mira ya vio los mensajes: sus avisos de "mensaje nuevo" de este chat quedan leídos.
     */
    @Transactional
    public ChatDetalleDto detalle(Usuario usuario, Long solicitudId, long despuesDe) {
        Solicitud s = buscarChat(usuario, solicitudId);
        notificaciones.marcarLeidosLosMensajesDe(usuario, s.getId());
        List<MensajeDto> mensajes = mensajeRepository
                .findBySolicitudIdAndIdGreaterThanOrderByIdAsc(s.getId(), despuesDe).stream()
                .map(MensajeDto::de).toList();
        return new ChatDetalleDto(aDto(s), mensajes);
    }

    @Transactional
    public MensajeDto enviar(Usuario usuario, Long solicitudId, String texto) {
        Solicitud s = buscarChat(usuario, solicitudId);
        if (estado(s) == EstadoChat.CERRADO) throw ApiException.conflicto("El chat está cerrado");
        Mensaje m = mensajeRepository.save(new Mensaje(s, usuario, texto.trim(), ahora()));
        avisarALaOtraParte(s, usuario, m.getTexto());
        return MensajeDto.de(m);
    }

    /** Si escribe quien pidió el espacio, se avisa a los administradores; si escribe un admin, a quien lo pidió. */
    private void avisarALaOtraParte(Solicitud s, Usuario autor, String texto) {
        String titulo = "Mensaje nuevo · " + s.getEspacio().getCodigo();
        String mensaje = autor.getNombre() + ": " + (texto.length() > 120 ? texto.substring(0, 117) + "..." : texto);
        List<Usuario> destinatarios = autor.getId().equals(s.getUsuario().getId())
                ? usuarioRepository.findByRolAndActivoTrue(Rol.ADMIN)
                : List.of(s.getUsuario());
        destinatarios.stream()
                .filter(u -> !u.getId().equals(autor.getId()))
                .forEach(u -> notificaciones.avisarMensajeNuevo(u, s.getId(), titulo, mensaje));
    }

    /** Solo ADMIN: lo pasa a "Cerrados" (ya no se puede escribir). */
    @Transactional
    public ChatDto cerrar(Usuario admin, Long solicitudId) {
        if (!admin.esAdmin()) throw ApiException.prohibido("Solo un administrador puede cerrar el chat");
        Solicitud s = buscarChat(admin, solicitudId);
        if (estado(s) == EstadoChat.CERRADO) throw ApiException.conflicto("El chat ya está cerrado");
        s.setChatCerradoEn(ahora());
        mensajeDelSistema(s, "El administrador cerró el chat.");
        return aDto(s);
    }

    /** Agrega un aviso del sistema al chat, si la solicitud lo tiene (lo usa SolicitudService). */
    @Transactional
    public void mensajeDelSistema(Solicitud s, String texto) {
        if (s.getAprobadaEn() != null) mensajeRepository.save(new Mensaje(s, null, texto, ahora()));
    }

    // ── Apoyo ───────────────────────────────────────────────────────────

    /** CERRADO si un administrador lo cerró o si ya terminó el horario de la reserva. */
    public EstadoChat estado(Solicitud s) {
        if (s.getChatCerradoEn() != null) return EstadoChat.CERRADO;
        var bloques = s.getBloques();
        LocalDateTime fin = s.getFecha().atTime(bloques.get(bloques.size() - 1).getHoraFin());
        return fin.isAfter(ahora()) ? EstadoChat.ABIERTO : EstadoChat.CERRADO;
    }

    /** La solicitud debe tener chat y quien pregunta debe ser quien la pidió o un administrador. */
    private Solicitud buscarChat(Usuario usuario, Long solicitudId) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .filter(x -> x.getAprobadaEn() != null)
                .orElseThrow(() -> ApiException.noEncontrado("Esta solicitud no tiene chat"));
        if (!usuario.esAdmin() && !s.getUsuario().getId().equals(usuario.getId())) {
            throw ApiException.noEncontrado("Esta solicitud no tiene chat");
        }
        return s;
    }

    private ChatDto aDto(Solicitud s) {
        MensajeDto ultimo = mensajeRepository.findFirstBySolicitudIdOrderByIdDesc(s.getId())
                .map(MensajeDto::de).orElse(null);
        boolean iniciada = mensajeRepository.existsBySolicitudIdAndAutorIsNotNull(s.getId());
        return new ChatDto(SolicitudDto.de(s), estado(s), ultimo, iniciada);
    }

    private static LocalDateTime ultimaActividad(ChatDto c) {
        return c.ultimoMensaje() != null ? c.ultimoMensaje().creadoEn() : c.solicitud().creadaEn();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(props.zona());
    }
}

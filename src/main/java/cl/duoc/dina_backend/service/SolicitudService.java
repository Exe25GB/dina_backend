package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.config.DinaProperties;
import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.BloqueHorarioRepository;
import cl.duoc.dina_backend.repository.EspacioRepository;
import cl.duoc.dina_backend.repository.SolicitudRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de una solicitud:
 *   usuario la crea (PENDIENTE) → se avisa a los administradores
 *   → un administrador la APRUEBA o la RECHAZA → se avisa al usuario.
 * Al aprobar, las demás solicitudes pendientes que chocan con ese horario se rechazan solas
 * (y también se avisa a sus usuarios). Así nunca hay dos reservas aprobadas en el mismo bloque.
 */
@Service
public class SolicitudService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final List<EstadoSolicitud> ACTIVAS = List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADA);
    private static final List<EstadoSolicitud> APROBADAS = List.of(EstadoSolicitud.APROBADA);
    private static final List<EstadoSolicitud> PENDIENTES = List.of(EstadoSolicitud.PENDIENTE);

    private final SolicitudRepository solicitudRepository;
    private final EspacioRepository espacioRepository;
    private final BloqueHorarioRepository bloqueRepository;
    private final EspacioService espacioService;
    private final HorarioService horario;
    private final NotificacionService notificaciones;
    private final ChatService chat;
    private final DinaProperties props;

    public SolicitudService(SolicitudRepository solicitudRepository, EspacioRepository espacioRepository,
                            BloqueHorarioRepository bloqueRepository, EspacioService espacioService,
                            HorarioService horario, NotificacionService notificaciones, ChatService chat,
                            DinaProperties props) {
        this.chat = chat;
        this.solicitudRepository = solicitudRepository;
        this.espacioRepository = espacioRepository;
        this.bloqueRepository = bloqueRepository;
        this.espacioService = espacioService;
        this.horario = horario;
        this.notificaciones = notificaciones;
        this.props = props;
    }

    // Usuario

    @Transactional
    public SolicitudDto crear(Usuario usuario, CrearSolicitudRequest req) {
        Espacio espacio = espacioService.buscarActivo(req.espacioId());
        LocalDate fecha = req.fecha();

        if (fecha.isBefore(horario.hoy())) {
            throw ApiException.invalido("No puedes solicitar una fecha pasada");
        }
        if (fecha.isAfter(horario.fechaMaxima())) {
            throw ApiException.invalido("Solo puedes solicitar con hasta "
                    + props.solicitudes().maxDiasAnticipacion() + " días de anticipación");
        }
        if (req.asistentes() > espacio.getCapacidad()) {
            throw ApiException.invalido("El espacio tiene capacidad para " + espacio.getCapacidad() + " personas");
        }

        List<BloqueHorario> bloques = validarBloques(fecha, req.bloqueIds());
        List<Long> bloqueIds = bloques.stream().map(BloqueHorario::getId).toList();

        if (!solicitudRepository.buscarCruces(espacio.getId(), fecha, bloqueIds, APROBADAS, -1L).isEmpty()) {
            throw ApiException.conflicto("El espacio ya está reservado en ese horario");
        }
        if (solicitudRepository.usuarioTieneCruce(usuario.getId(), fecha, bloqueIds, ACTIVAS)) {
            throw ApiException.conflicto("Ya tienes una solicitud en ese horario");
        }

        Solicitud s = new Solicitud();
        s.setUsuario(usuario);
        s.setEspacio(espacio);
        s.setFecha(fecha);
        s.setBloques(new ArrayList<>(bloques));
        s.setActividad(req.actividad().trim());
        s.setAsistentes(req.asistentes());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setCreadaEn(ahora());
        solicitudRepository.save(s);

        notificaciones.notificarAdmins(TipoNotificacion.NUEVA_SOLICITUD,
                "Nueva solicitud por revisar",
                usuario.getNombre() + " solicita " + descripcion(s) + ": " + s.getActividad(),
                s.getId());

        return SolicitudDto.de(s);
    }

    @Transactional(readOnly = true)
    public List<SolicitudDto> mias(Usuario usuario) {
        return solicitudRepository.findByUsuarioIdOrderByFechaDescCreadaEnDesc(usuario.getId()).stream()
                .map(SolicitudDto::de).toList();
    }

    /**
     * Detalle de una solicitud. La ve quien la pidió o un administrador; para cualquier otro
     * "no existe" (no se revela que el id es de otra persona).
     */
    @Transactional(readOnly = true)
    public SolicitudDto detalle(Usuario usuario, Long id) {
        Solicitud s = buscar(id);
        if (!usuario.esAdmin() && !s.getUsuario().getId().equals(usuario.getId())) {
            throw ApiException.noEncontrado("Solicitud no encontrada");
        }
        return SolicitudDto.de(s);
    }

    /**
     * Cancelar una reserva que todavía no comienza.
     * - Quien la pidió: su solicitud pendiente o aprobada, al instante; el motivo es opcional.
     * - Un administrador: solo una aprobada de otra persona, y debe explicar el motivo
     *   (queda en el chat y le llega una notificación al usuario).
     * Si la solicitud tiene chat, queda un aviso en él y el chat sigue abierto para aclarar dudas.
     */
    @Transactional
    public SolicitudDto cancelar(Usuario usuario, Long id, String motivo) {
        // Bloquea la fila, igual que aprobar/rechazar: si el admin aprueba mientras el usuario cancela,
        // el segundo ve el estado ya cambiado (no quedan una notificación de "aprobada" y una cancelación)
        Solicitud s = solicitudRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> ApiException.noEncontrado("Solicitud no encontrada"));
        boolean esDueno = s.getUsuario().getId().equals(usuario.getId());
        if (!esDueno && !usuario.esAdmin()) throw ApiException.noEncontrado("Solicitud no encontrada");

        String explicacion = textoOpcional(motivo);
        if (esDueno) {
            if (s.getEstado() != EstadoSolicitud.PENDIENTE && s.getEstado() != EstadoSolicitud.APROBADA) {
                throw ApiException.conflicto("La solicitud ya está " + s.getEstado().name().toLowerCase());
            }
        } else {
            if (s.getEstado() != EstadoSolicitud.APROBADA) {
                throw ApiException.conflicto("Solo se puede cancelar una reserva aprobada");
            }
            if (explicacion == null) throw ApiException.invalido("Explica al usuario por qué se cancela su reserva");
        }
        if (horario.yaPaso(s.getFecha(), s.getBloques().get(0))) {
            throw ApiException.conflicto("La reserva ya comenzó, no se puede cancelar");
        }

        s.setEstado(EstadoSolicitud.CANCELADA);
        String sufijo = explicacion != null ? " Motivo: " + explicacion : "";
        if (esDueno) {
            chat.mensajeDelSistema(s, s.getUsuario().getNombre() + " canceló la reserva." + sufijo);
        } else {
            s.setComentarioAdmin(explicacion);
            chat.mensajeDelSistema(s, "El administrador canceló la reserva." + sufijo);
            notificaciones.notificar(s.getUsuario(), TipoNotificacion.RESERVA_CANCELADA,
                    "Reserva cancelada",
                    "Tu reserva de " + descripcion(s) + " fue cancelada por el administrador." + sufijo,
                    s.getId());
        }
        return SolicitudDto.de(s);
    }

    // Administrador

    @Transactional(readOnly = true)
    public List<SolicitudDto> listar(EstadoSolicitud estado) {
        List<Solicitud> lista = estado == null
                ? solicitudRepository.findAllByOrderByFechaDescCreadaEnDesc()
                : solicitudRepository.findByEstadoOrderByFechaAscCreadaEnAsc(estado);
        return lista.stream().map(SolicitudDto::de).toList();
    }

    @Transactional
    public SolicitudDto aprobar(Usuario admin, Long id, String comentario) {
        Solicitud s = buscarPendiente(id);
        // Bloquea el espacio: si otro admin aprueba algo del mismo espacio a la vez, espera su turno
        espacioRepository.findByIdParaActualizar(s.getEspacio().getId());

        if (horario.yaPaso(s.getFecha(), s.getBloques().get(0))) {
            throw ApiException.conflicto("El horario de la solicitud ya pasó; solo se puede rechazar");
        }
        List<Long> bloqueIds = s.getBloques().stream().map(BloqueHorario::getId).toList();
        if (!solicitudRepository.buscarCruces(s.getEspacio().getId(), s.getFecha(), bloqueIds, APROBADAS, s.getId()).isEmpty()) {
            throw ApiException.conflicto("Ya hay otra solicitud aprobada en ese horario");
        }

        marcarRevisada(s, admin, EstadoSolicitud.APROBADA, textoOpcional(comentario));
        // Desde ahora la solicitud tiene chat (abierto hasta que termine la reserva)
        s.setAprobadaEn(ahora());
        chat.mensajeDelSistema(s, "Solicitud aprobada. Este chat queda abierto hasta que termine la reserva."
                + (s.getComentarioAdmin() != null ? " Comentario: " + s.getComentarioAdmin() : ""));
        notificaciones.notificar(s.getUsuario(), TipoNotificacion.SOLICITUD_APROBADA,
                "Solicitud aprobada",
                "Tu solicitud de " + descripcion(s) + " fue aprobada."
                        + (s.getComentarioAdmin() != null ? " Comentario: " + s.getComentarioAdmin() : ""),
                s.getId());

        // Las demás pendientes que chocan pierden el horario
        for (Solicitud otra : solicitudRepository.buscarCruces(
                s.getEspacio().getId(), s.getFecha(), bloqueIds, PENDIENTES, s.getId())) {
            marcarRevisada(otra, admin, EstadoSolicitud.RECHAZADA,
                    "El espacio fue asignado a otra solicitud en ese horario");
            notificarRechazo(otra);
        }
        return SolicitudDto.de(s);
    }

    @Transactional
    public SolicitudDto rechazar(Usuario admin, Long id, String motivo) {
        Solicitud s = buscarPendiente(id);
        marcarRevisada(s, admin, EstadoSolicitud.RECHAZADA, textoOpcional(motivo));
        notificarRechazo(s);
        return SolicitudDto.de(s);
    }

    // Apoyo

    /**
     * Los bloques deben existir, ser seguidos (p. ej. 3-4-5), no superar el máximo
     * y estar dentro del horario de atención de ese día.
     */
    private List<BloqueHorario> validarBloques(LocalDate fecha, List<Long> ids) {
        LinkedHashSet<Long> unicos = new LinkedHashSet<>(ids);
        int max = props.solicitudes().maxBloques();
        if (unicos.size() > max) {
            throw ApiException.invalido("Puedes juntar como máximo " + max + " bloques (" + max + " horas)");
        }
        List<BloqueHorario> bloques = new ArrayList<>(bloqueRepository.findAllById(unicos));
        if (bloques.size() != unicos.size()) {
            throw ApiException.invalido("Uno o más bloques no existen");
        }
        bloques.sort(Comparator.comparingInt(BloqueHorario::getNumero));
        for (int i = 1; i < bloques.size(); i++) {
            if (bloques.get(i).getNumero() != bloques.get(i - 1).getNumero() + 1) {
                throw ApiException.invalido("Los bloques deben ser seguidos, sin horas libres entre ellos");
            }
        }
        for (BloqueHorario b : bloques) {
            if (!horario.atiende(fecha, b)) {
                throw ApiException.invalido("La sede no atiende en el bloque de " + b.getHoraInicio() + " ese día");
            }
            if (horario.yaPaso(fecha, b)) {
                throw ApiException.invalido("El bloque de " + b.getHoraInicio() + " ya comenzó");
            }
        }
        return bloques;
    }

    private void marcarRevisada(Solicitud s, Usuario admin, EstadoSolicitud estado, String comentario) {
        s.setEstado(estado);
        s.setRevisadaPor(admin);
        s.setRevisadaEn(ahora());
        s.setComentarioAdmin(comentario);
    }

    private void notificarRechazo(Solicitud s) {
        notificaciones.notificar(s.getUsuario(), TipoNotificacion.SOLICITUD_RECHAZADA,
                "Solicitud rechazada",
                "Tu solicitud de " + descripcion(s) + " fue rechazada."
                        + (s.getComentarioAdmin() != null ? " Motivo: " + s.getComentarioAdmin() : ""),
                s.getId());
    }

    /** "S41 (Edificio Norte) el 25-11-2026 de 10:00 a 12:00" */
    private String descripcion(Solicitud s) {
        List<BloqueHorario> b = s.getBloques();
        return s.getEspacio().getCodigo() + " (" + s.getEspacio().getEdificio().getNombre() + ") el "
                + s.getFecha().format(FECHA) + " de " + b.get(0).getHoraInicio()
                + " a " + b.get(b.size() - 1).getHoraFin();
    }

    private Solicitud buscar(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Solicitud no encontrada"));
    }

    /** Bloquea la fila: si dos admins revisan la misma solicitud a la vez, el segundo ve el estado ya cambiado. */
    private Solicitud buscarPendiente(Long id) {
        Solicitud s = solicitudRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> ApiException.noEncontrado("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw ApiException.conflicto("La solicitud ya está " + s.getEstado().name().toLowerCase());
        }
        return s;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(props.zona());
    }
}

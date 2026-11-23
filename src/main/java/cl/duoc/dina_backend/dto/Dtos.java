package cl.duoc.dina_backend.dto;

import cl.duoc.dina_backend.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Objetos que entran y salen de la API (JSON).
 * Nunca se devuelven las entidades directamente: así no se expone, por ejemplo, la contraseña.
 */
public final class Dtos {

    private Dtos() {}

    // ── Autenticación ───────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") String correo,
            @NotBlank(message = "es obligatoria") String password) {}

    public record LoginResponse(String token, LocalDateTime expiraEn, UsuarioDto usuario) {}

    public record UsuarioDto(Long id, String correo, String nombre, Rol rol) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getCorreo(), u.getNombre(), u.getRol());
        }
    }

    // ── Espacios y bloques ──────────────────────────────────────────────

    public record EspacioDto(
            Long id, String codigo, String nombre,
            Edificio edificio, String edificioNombre, int piso,
            TipoEspacio tipo, int capacidad, Escuela escuela,
            boolean proyector, boolean computadores, boolean climatizacion) {
        public static EspacioDto de(Espacio e) {
            return new EspacioDto(e.getId(), e.getCodigo(), e.getNombre(),
                    e.getEdificio(), e.getEdificio().getNombre(), e.getPiso(),
                    e.getTipo(), e.getCapacidad(), e.getEscuela(),
                    e.isProyector(), e.isComputadores(), e.isClimatizacion());
        }
    }

    public record BloqueDto(Long id, int numero, LocalTime horaInicio, LocalTime horaFin) {
        public static BloqueDto de(BloqueHorario b) {
            return new BloqueDto(b.getId(), b.getNumero(), b.getHoraInicio(), b.getHoraFin());
        }
    }

    public record EdificioDto(Edificio codigo, String nombre) {}

    /** Estado de un bloque para un espacio en una fecha. */
    public enum EstadoBloque {
        /** Se puede solicitar. */
        DISPONIBLE,
        /** Ya tiene una solicitud aprobada. */
        OCUPADO,
        /** Ya pasó, o la sede no atiende en ese horario (domingo, sábado en la tarde). */
        NO_DISPONIBLE
    }

    public record BloqueDisponibilidadDto(
            Long bloqueId, int numero, LocalTime horaInicio, LocalTime horaFin,
            EstadoBloque estado,
            /** Cuántas solicitudes pendientes compiten por este bloque. */
            int solicitudesPendientes,
            /** true si el usuario que consulta ya pidió este bloque (pendiente o aprobada). */
            boolean solicitadoPorMi) {}

    public record DisponibilidadDto(EspacioDto espacio, LocalDate fecha, List<BloqueDisponibilidadDto> bloques) {}

    // ── Solicitudes ─────────────────────────────────────────────────────

    public record CrearSolicitudRequest(
            @NotNull(message = "es obligatorio") Long espacioId,
            @NotNull(message = "es obligatoria") LocalDate fecha,
            @NotEmpty(message = "debe incluir al menos un bloque") List<@NotNull Long> bloqueIds,
            @NotBlank(message = "es obligatoria") @Size(max = 150, message = "máximo 150 caracteres") String actividad,
            @Min(value = 1, message = "debe ser al menos 1") int asistentes) {}

    public record AprobarRequest(@Size(max = 300, message = "máximo 300 caracteres") String comentario) {}

    public record RechazarRequest(
            @NotBlank(message = "es obligatorio indicar el motivo")
            @Size(max = 300, message = "máximo 300 caracteres") String motivo) {}

    public record SolicitudDto(
            Long id, EspacioDto espacio, LocalDate fecha, List<BloqueDto> bloques,
            LocalTime horaInicio, LocalTime horaFin,
            String actividad, int asistentes, EstadoSolicitud estado,
            String comentarioAdmin, UsuarioDto solicitante,
            LocalDateTime creadaEn, LocalDateTime revisadaEn) {
        public static SolicitudDto de(Solicitud s) {
            List<BloqueDto> bloques = s.getBloques().stream().map(BloqueDto::de).toList();
            return new SolicitudDto(s.getId(), EspacioDto.de(s.getEspacio()), s.getFecha(), bloques,
                    bloques.get(0).horaInicio(), bloques.get(bloques.size() - 1).horaFin(),
                    s.getActividad(), s.getAsistentes(), s.getEstado(),
                    s.getComentarioAdmin(), UsuarioDto.de(s.getUsuario()),
                    s.getCreadaEn(), s.getRevisadaEn());
        }
    }

    // ── Notificaciones ──────────────────────────────────────────────────

    public record NotificacionDto(
            Long id, TipoNotificacion tipo, String titulo, String mensaje,
            Long solicitudId, boolean leida, LocalDateTime creadaEn) {
        public static NotificacionDto de(Notificacion n) {
            return new NotificacionDto(n.getId(), n.getTipo(), n.getTitulo(), n.getMensaje(),
                    n.getSolicitudId(), n.isLeida(), n.getCreadaEn());
        }
    }

    public record ConteoDto(long noLeidas) {}
}

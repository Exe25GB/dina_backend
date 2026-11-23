package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.model.EstadoSolicitud;
import cl.duoc.dina_backend.model.Solicitud;
import cl.duoc.dina_backend.model.TipoNotificacion;
import cl.duoc.dina_backend.repository.SolicitudRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avisa "Tu reserva es hoy" a quien tiene una reserva aprobada que empieza dentro de la próxima hora. */
@Service
public class RecordatorioService {

    /** Con cuánta anticipación se avisa. */
    public static final Duration ANTICIPACION = Duration.ofHours(1);

    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificaciones;

    public RecordatorioService(SolicitudRepository solicitudRepository, NotificacionService notificaciones) {
        this.solicitudRepository = solicitudRepository;
        this.notificaciones = notificaciones;
    }

    /** Devuelve cuántos avisos envió. [ahora] es un parámetro para poder probarlo a cualquier hora. */
    @Transactional
    public int enviarRecordatorios(LocalDateTime ahora) {
        int enviados = 0;
        for (Solicitud s : solicitudRepository.findByEstadoAndFechaAndRecordatorioEnviadoEnIsNull(
                EstadoSolicitud.APROBADA, ahora.toLocalDate())) {
            var bloques = s.getBloques();
            LocalDateTime inicio = s.getFecha().atTime(bloques.get(0).getHoraInicio());
            if (inicio.isBefore(ahora) || inicio.isAfter(ahora.plus(ANTICIPACION))) continue;

            long minutos = Math.max(1, Duration.between(ahora, inicio).toMinutes());
            notificaciones.notificar(s.getUsuario(), TipoNotificacion.RECORDATORIO_RESERVA,
                    "Tu reserva es hoy",
                    s.getEspacio().getCodigo() + " (" + s.getEspacio().getEdificio().getNombre() + ") de "
                            + bloques.get(0).getHoraInicio() + " a " + bloques.get(bloques.size() - 1).getHoraFin()
                            + ". Empieza en " + minutos + " minutos.",
                    s.getId());
            s.setRecordatorioEnviadoEn(ahora);
            enviados++;
        }
        return enviados;
    }
}

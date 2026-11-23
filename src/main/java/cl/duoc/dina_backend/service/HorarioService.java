package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.config.DinaProperties;
import cl.duoc.dina_backend.model.BloqueHorario;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

/**
 * Reglas del calendario de la sede:
 * - Lunes a viernes: todos los bloques.
 * - Sábado: solo bloques que terminan a las 14:00 o antes.
 * - Domingo: cerrado.
 * - Un bloque que ya empezó no se puede solicitar.
 */
@Service
public class HorarioService {

    static final LocalTime CIERRE_SABADO = LocalTime.of(14, 0);

    private final DinaProperties props;

    public HorarioService(DinaProperties props) {
        this.props = props;
    }

    public LocalDate hoy() {
        return LocalDate.now(props.zona());
    }

    /** ¿La sede atiende en este bloque ese día? */
    public boolean atiende(LocalDate fecha, BloqueHorario bloque) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY) return false;
        if (dia == DayOfWeek.SATURDAY) return !bloque.getHoraFin().isAfter(CIERRE_SABADO);
        return true;
    }

    /** ¿El bloque ya comenzó o pasó? */
    public boolean yaPaso(LocalDate fecha, BloqueHorario bloque) {
        LocalDateTime inicio = fecha.atTime(bloque.getHoraInicio());
        return !inicio.isAfter(LocalDateTime.now(props.zona()));
    }

    /** ¿Se puede pedir este bloque hoy (atiende y no ha empezado)? */
    public boolean solicitable(LocalDate fecha, BloqueHorario bloque) {
        return atiende(fecha, bloque) && !yaPaso(fecha, bloque);
    }

    public LocalDate fechaMaxima() {
        return hoy().plusDays(props.solicitudes().maxDiasAnticipacion());
    }
}

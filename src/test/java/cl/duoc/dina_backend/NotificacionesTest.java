package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.dto.Dtos.NotificacionDto;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.*;
import cl.duoc.dina_backend.service.ChatService;
import cl.duoc.dina_backend.service.NotificacionService;
import cl.duoc.dina_backend.service.RecordatorioService;
import cl.duoc.dina_backend.service.SolicitudService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Avisos: borrar, "mensaje nuevo" (uno por chat), cancelación del usuario y recordatorio. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificacionesTest {

    @Autowired SolicitudService solicitudes;
    @Autowired ChatService chats;
    @Autowired NotificacionService notificaciones;
    @Autowired RecordatorioService recordatorios;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspacioRepository espacios;
    @Autowired BloqueHorarioRepository bloques;

    Usuario admin;
    Usuario vict;
    Long lc1;
    LocalDate lunes;

    @BeforeEach
    void preparar() {
        admin = usuarios.findByCorreoIgnoreCase("exe.guzman@duocuc.cl").orElseThrow();
        vict = usuarios.findByCorreoIgnoreCase("vict.godoy@duocuc.cl").orElseThrow();
        lc1 = espacios.findAll().stream().filter(e -> e.getCodigo().equals("LC1")).findFirst().orElseThrow().getId();
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /** Solicitud de Victor, aprobada. Bloque 1 = 08:00, bloque 4 = 11:00. */
    private SolicitudDto aprobada(int bloque) {
        Long id = bloques.findAllByOrderByNumeroAsc().get(bloque - 1).getId();
        SolicitudDto s = solicitudes.crear(vict, new CrearSolicitudRequest(lc1, lunes, List.of(id), "Clase", 10));
        return solicitudes.aprobar(admin, s.id(), null);
    }

    private List<NotificacionDto> de(Usuario u, TipoNotificacion tipo) {
        return notificaciones.listar(u).stream().filter(n -> n.tipo() == tipo).toList();
    }

    @Test
    void borrarUnaYTodas() {
        aprobada(2);
        aprobada(3);
        List<NotificacionDto> deVict = notificaciones.listar(vict);
        assertThat(deVict).hasSizeGreaterThanOrEqualTo(2);

        // Nadie borra las de otro
        Long ajena = deVict.get(0).id();
        try {
            notificaciones.borrar(admin, ajena);
        } catch (ApiException e) {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        notificaciones.borrar(vict, ajena);
        assertThat(notificaciones.listar(vict)).hasSize(deVict.size() - 1);

        notificaciones.borrarTodas(vict);
        assertThat(notificaciones.listar(vict)).isEmpty();
        assertThat(notificaciones.listar(admin)).isNotEmpty(); // las del admin siguen
    }

    @Test
    void mensajeNuevoEsUnAvisoPorChatYSeLeeAlAbrirlo() {
        SolicitudDto s = aprobada(5);
        chats.enviar(vict, s.id(), "Hola");
        chats.enviar(vict, s.id(), "¿Hay proyector?");

        List<NotificacionDto> avisos = de(admin, TipoNotificacion.MENSAJE_NUEVO);
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).mensaje()).isEqualTo("Victor Godoy: ¿Hay proyector?");
        assertThat(avisos.get(0).solicitudId()).isEqualTo(s.id());

        // El admin abre el chat: el aviso queda leído; quien escribe no recibe aviso de lo suyo
        chats.detalle(admin, s.id(), 0);
        assertThat(de(admin, TipoNotificacion.MENSAJE_NUEVO)).allMatch(NotificacionDto::leida);
        assertThat(de(vict, TipoNotificacion.MENSAJE_NUEVO)).isEmpty();

        chats.enviar(admin, s.id(), "Sí");
        assertThat(de(vict, TipoNotificacion.MENSAJE_NUEVO)).hasSize(1);
    }

    @Test
    void cancelarUnaAprobadaAvisaALosAdmins() {
        SolicitudDto aprobada = aprobada(6);
        solicitudes.cancelar(vict, aprobada.id(), "Se suspendió la clase");
        assertThat(de(admin, TipoNotificacion.CANCELADA_POR_USUARIO))
                .anyMatch(n -> n.solicitudId().equals(aprobada.id()) && n.mensaje().contains("Se suspendió"));

        // Una pendiente cancelada solo desaparece de "por revisar"
        Long id = bloques.findAllByOrderByNumeroAsc().get(7).getId();
        SolicitudDto pendiente = solicitudes.crear(vict, new CrearSolicitudRequest(lc1, lunes, List.of(id), "Clase", 10));
        solicitudes.cancelar(vict, pendiente.id(), null);
        assertThat(de(admin, TipoNotificacion.CANCELADA_POR_USUARIO))
                .noneMatch(n -> n.solicitudId().equals(pendiente.id()));
    }

    @Test
    void recordatorioUnaHoraAntesYUnaSolaVez() {
        SolicitudDto s = aprobada(4); // 11:00
        assertThat(recordatorios.enviarRecordatorios(lunes.atTime(9, 30))).isZero();   // falta más de una hora
        assertThat(recordatorios.enviarRecordatorios(lunes.atTime(10, 15))).isEqualTo(1);
        assertThat(recordatorios.enviarRecordatorios(lunes.atTime(10, 20))).isZero();  // ya se avisó

        List<NotificacionDto> avisos = de(vict, TipoNotificacion.RECORDATORIO_RESERVA);
        assertThat(avisos).singleElement().satisfies(n -> {
            assertThat(n.solicitudId()).isEqualTo(s.id());
            assertThat(n.mensaje()).contains("LC1").contains("45 minutos");
        });
    }
}

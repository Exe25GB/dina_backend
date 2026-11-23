package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.dina_backend.dto.Dtos.ChatDto;
import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.*;
import cl.duoc.dina_backend.service.ChatService;
import cl.duoc.dina_backend.service.NotificacionService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Chat de las solicitudes aprobadas: quién lo ve, cuándo se cierra y las cancelaciones. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatFlujoTest {

    @Autowired SolicitudService solicitudes;
    @Autowired ChatService chats;
    @Autowired NotificacionService notificaciones;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspacioRepository espacios;
    @Autowired BloqueHorarioRepository bloques;
    @Autowired SolicitudRepository solicitudRepo;
    @Autowired PasswordEncoder encoder;

    Usuario admin;
    Usuario vict;
    Usuario otro;
    Long lc1;
    LocalDate lunes;

    @BeforeEach
    void preparar() {
        admin = usuarios.findByCorreoIgnoreCase("exe.guzman@duocuc.cl").orElseThrow();
        vict = usuarios.findByCorreoIgnoreCase("vict.godoy@duocuc.cl").orElseThrow();
        otro = usuarios.save(new Usuario("otro.chat@duocuc.cl", "Otro Chat", encoder.encode("duoc1234"), Rol.USUARIO));
        lc1 = espacios.findAll().stream().filter(e -> e.getCodigo().equals("LC1")).findFirst().orElseThrow().getId();
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private SolicitudDto pedir(int bloque) {
        Long id = bloques.findAllByOrderByNumeroAsc().get(bloque - 1).getId();
        return solicitudes.crear(vict, new CrearSolicitudRequest(lc1, lunes, List.of(id), "Clase de prueba", 10));
    }

    private SolicitudDto aprobada(int bloque) {
        return solicitudes.aprobar(admin, pedir(bloque).id(), null);
    }

    private HttpStatus estadoDe(Runnable r) {
        try {
            r.run();
            return HttpStatus.OK;
        } catch (ApiException e) {
            return e.getStatus();
        }
    }

    @Test
    void soloLasAprobadasTienenChat() {
        SolicitudDto pendiente = pedir(2);
        assertThat(pendiente.tieneChat()).isFalse();
        assertThat(estadoDe(() -> chats.detalle(vict, pendiente.id(), 0))).isEqualTo(HttpStatus.NOT_FOUND);

        SolicitudDto rechazada = solicitudes.rechazar(admin, pedir(3).id(), "No");
        assertThat(rechazada.tieneChat()).isFalse();
        assertThat(chats.listar(vict)).noneMatch(c -> c.solicitud().id().equals(rechazada.id()));
    }

    @Test
    void alAprobarSeAbreElChatYLoVenSoloElDuenoYElAdmin() {
        SolicitudDto s = aprobada(4);
        assertThat(s.tieneChat()).isTrue();

        ChatDto chat = chats.listar(vict).stream().filter(c -> c.solicitud().id().equals(s.id())).findFirst().orElseThrow();
        assertThat(chat.estado()).isEqualTo(EstadoChat.ABIERTO);
        assertThat(chat.ultimoMensaje().autor()).isNull(); // aviso del sistema
        assertThat(chat.ultimoMensaje().texto()).contains("aprobada");

        var enviado = chats.enviar(vict, s.id(), "  ¿Puedo llegar 10 minutos antes?  ");
        assertThat(enviado.texto()).isEqualTo("¿Puedo llegar 10 minutos antes?");
        chats.enviar(admin, s.id(), "Sí, sin problema");

        // Solo los mensajes nuevos (la app pregunta "después del último que tengo")
        assertThat(chats.detalle(admin, s.id(), enviado.id()).mensajes())
                .extracting(m -> m.texto()).containsExactly("Sí, sin problema");
        assertThat(estadoDe(() -> chats.detalle(otro, s.id(), 0))).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void elUsuarioCancelaAlInstanteYElChatSigueAbierto() {
        SolicitudDto s = aprobada(5);
        SolicitudDto cancelada = solicitudes.cancelar(vict, s.id(), "Se suspendió la clase");
        assertThat(cancelada.estado()).isEqualTo(EstadoSolicitud.CANCELADA);

        var chat = chats.detalle(admin, s.id(), 0);
        assertThat(chat.chat().estado()).isEqualTo(EstadoChat.ABIERTO);
        assertThat(chat.mensajes()).anyMatch(m -> m.autor() == null && m.texto().contains("Se suspendió la clase"));
        // El admin aún puede aclarar cosas
        chats.enviar(admin, s.id(), "Entendido, gracias por avisar");
    }

    @Test
    void elAdminCancelaConMotivoObligatorio() {
        SolicitudDto s = aprobada(6);
        assertThat(estadoDe(() -> solicitudes.cancelar(admin, s.id(), "  "))).isEqualTo(HttpStatus.BAD_REQUEST);

        SolicitudDto cancelada = solicitudes.cancelar(admin, s.id(), "Mantención del laboratorio");
        assertThat(cancelada.estado()).isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(notificaciones.listar(vict))
                .anyMatch(n -> n.tipo() == TipoNotificacion.RESERVA_CANCELADA && n.mensaje().contains("Mantención"));
        // Una pendiente no la "cancela" el admin: la rechaza
        SolicitudDto pendiente = pedir(7);
        assertThat(estadoDe(() -> solicitudes.cancelar(admin, pendiente.id(), "x"))).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elAdminCierraElChatYYaNoSePuedeEscribir() {
        SolicitudDto s = aprobada(8);
        assertThat(estadoDe(() -> chats.cerrar(vict, s.id()))).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(chats.cerrar(admin, s.id()).estado()).isEqualTo(EstadoChat.CERRADO);
        assertThat(estadoDe(() -> chats.enviar(vict, s.id(), "hola"))).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cuandoTerminaLaReservaElChatSeCierraSolo() {
        SolicitudDto s = aprobada(9);
        Solicitud entidad = solicitudRepo.findById(s.id()).orElseThrow();
        entidad.setFecha(LocalDate.now().minusDays(1)); // como si la reserva hubiera sido ayer

        assertThat(chats.detalle(vict, s.id(), 0).chat().estado()).isEqualTo(EstadoChat.CERRADO);
        assertThat(estadoDe(() -> chats.enviar(vict, s.id(), "hola"))).isEqualTo(HttpStatus.CONFLICT);
    }
}

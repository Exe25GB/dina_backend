package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.*;
import cl.duoc.dina_backend.service.AuthService;
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

/** Flujo completo: solicitar bloques → admin aprueba/rechaza → notificaciones. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SolicitudFlujoTest {

    @Autowired SolicitudService solicitudes;
    @Autowired NotificacionService notificaciones;
    @Autowired AuthService auth;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspacioRepository espacios;
    @Autowired BloqueHorarioRepository bloques;
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
        otro = usuarios.save(new Usuario("otro.prueba@profesor.duoc.cl", "Otro de Prueba",
                encoder.encode("duoc1234"), Rol.USUARIO));
        lc1 = espacios.findAll().stream().filter(e -> e.getCodigo().equals("LC1")).findFirst().orElseThrow().getId();
        // Un lunes futuro dentro del rango permitido
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private List<Long> bloquesNumero(int... numeros) {
        return java.util.Arrays.stream(numeros)
                .mapToObj(n -> bloques.findAllByOrderByNumeroAsc().get(n - 1).getId()).toList();
    }

    private SolicitudDto pedir(Usuario u, LocalDate fecha, int... numeros) {
        return solicitudes.crear(u, new CrearSolicitudRequest(lc1, fecha, bloquesNumero(numeros), "Clase de prueba", 20));
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
    void datosDePrueba() {
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(vict.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(bloques.count()).isEqualTo(14);
        assertThat(espacios.count()).isEqualTo(79);
    }

    @Test
    void loginConDominioYClave() {
        assertThat(auth.login("Exe.Guzman@duocuc.cl", "duoc1234").usuario().rol()).isEqualTo(Rol.ADMIN);
        assertThat(estadoDe(() -> auth.login("vict.godoy@duocuc.cl", "mala"))).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(estadoDe(() -> auth.login("alguien@gmail.com", "duoc1234"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(estadoDe(() -> auth.login("x@falso.duocuc.cl", "duoc1234"))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void crearQuedaPendienteYAvisaAlAdmin() {
        SolicitudDto s = pedir(vict, lunes, 3, 4);
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(s.horaInicio()).hasToString("10:00");
        assertThat(s.horaFin()).hasToString("12:00");
        assertThat(notificaciones.listar(admin))
                .anyMatch(n -> n.tipo() == TipoNotificacion.NUEVA_SOLICITUD && n.solicitudId().equals(s.id()));
    }

    @Test
    void reglasDeBloques() {
        // No seguidos
        assertThat(estadoDe(() -> pedir(vict, lunes, 3, 5))).isEqualTo(HttpStatus.BAD_REQUEST);
        // Más de 4 bloques
        assertThat(estadoDe(() -> pedir(vict, lunes, 1, 2, 3, 4, 5))).isEqualTo(HttpStatus.BAD_REQUEST);
        // Domingo cerrado
        assertThat(estadoDe(() -> pedir(vict, lunes.plusDays(6), 3))).isEqualTo(HttpStatus.BAD_REQUEST);
        // Sábado después de las 14:00 (bloque 7 = 14:00-15:00)
        assertThat(estadoDe(() -> pedir(vict, lunes.plusDays(5), 7))).isEqualTo(HttpStatus.BAD_REQUEST);
        // Sábado en la mañana sí
        assertThat(pedir(vict, lunes.plusDays(5), 1, 2).estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        // Fecha pasada
        assertThat(estadoDe(() -> pedir(vict, LocalDate.now().minusDays(1), 3))).isEqualTo(HttpStatus.BAD_REQUEST);
        // Más asistentes que la capacidad (LC1 = 30)
        assertThat(estadoDe(() -> solicitudes.crear(vict,
                new CrearSolicitudRequest(lc1, lunes, bloquesNumero(3), "Muchos", 31)))).isEqualTo(HttpStatus.BAD_REQUEST);
        // El mismo usuario no puede pedir dos veces el mismo horario
        pedir(vict, lunes, 3);
        assertThat(estadoDe(() -> pedir(vict, lunes, 3, 4))).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void aprobarRechazaLasQueChocanYNotifica() {
        SolicitudDto deVict = pedir(vict, lunes, 3, 4);
        SolicitudDto deOtro = pedir(otro, lunes, 4, 5);   // choca en el bloque 4

        SolicitudDto aprobada = solicitudes.aprobar(admin, deVict.id(), "Listo");
        assertThat(aprobada.estado()).isEqualTo(EstadoSolicitud.APROBADA);

        // La otra quedó rechazada automáticamente
        SolicitudDto rechazada = solicitudes.mias(otro).get(0);
        assertThat(rechazada.id()).isEqualTo(deOtro.id());
        assertThat(rechazada.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);

        // Ambos usuarios recibieron su notificación
        assertThat(notificaciones.listar(vict)).anyMatch(n -> n.tipo() == TipoNotificacion.SOLICITUD_APROBADA);
        assertThat(notificaciones.listar(otro)).anyMatch(n -> n.tipo() == TipoNotificacion.SOLICITUD_RECHAZADA);

        // Ya nadie puede pedir el bloque 3 o 4 de ese espacio ese día
        assertThat(estadoDe(() -> pedir(otro, lunes, 3))).isEqualTo(HttpStatus.CONFLICT);
        // Pero el bloque 5 sí quedó libre
        assertThat(pedir(otro, lunes, 5).estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        // No se puede revisar dos veces
        assertThat(estadoDe(() -> solicitudes.rechazar(admin, deVict.id(), "x"))).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rechazarConMotivoNotifica() {
        SolicitudDto s = pedir(vict, lunes, 6);
        SolicitudDto r = solicitudes.rechazar(admin, s.id(), "Espacio en mantención");
        assertThat(r.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(r.comentarioAdmin()).isEqualTo("Espacio en mantención");
        assertThat(notificaciones.listar(vict))
                .anyMatch(n -> n.tipo() == TipoNotificacion.SOLICITUD_RECHAZADA && n.mensaje().contains("mantención"));
        assertThat(notificaciones.contarNoLeidas(vict)).isEqualTo(1);
        notificaciones.marcarTodasLeidas(vict);
        assertThat(notificaciones.contarNoLeidas(vict)).isZero();
    }

    @Test
    void motivoDe250Caracteres() {
        String motivo = "x".repeat(250);
        SolicitudDto s = solicitudes.crear(vict, new CrearSolicitudRequest(lc1, lunes, bloquesNumero(9), motivo, 10));
        assertThat(s.actividad()).hasSize(250);
    }

    @Test
    void cancelarSoloLaPropia() {
        SolicitudDto s = pedir(vict, lunes, 8);
        assertThat(estadoDe(() -> solicitudes.cancelar(otro, s.id()))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(solicitudes.cancelar(vict, s.id()).estado()).isEqualTo(EstadoSolicitud.CANCELADA);
    }
}

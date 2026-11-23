package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.EstadoSolicitud;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.BloqueHorarioRepository;
import cl.duoc.dina_backend.repository.EspacioRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
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

/** Un usuario puede pedir varios espacios a la misma hora, confirmando que lo sabe. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CruceSolicitudesTest {

    @Autowired SolicitudService solicitudes;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspacioRepository espacios;
    @Autowired BloqueHorarioRepository bloques;

    Usuario vict;
    Long lc1;
    Long otroEspacio;
    LocalDate lunes;
    List<Long> bloque10;

    @BeforeEach
    void preparar() {
        vict = usuarios.findByCorreoIgnoreCase("vict.godoy@duocuc.cl").orElseThrow();
        var todos = espacios.findAll();
        lc1 = todos.stream().filter(e -> e.getCodigo().equals("LC1")).findFirst().orElseThrow().getId();
        otroEspacio = todos.stream().filter(e -> !e.getId().equals(lc1)).findFirst().orElseThrow().getId();
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        bloque10 = List.of(bloques.findAllByOrderByNumeroAsc().get(9).getId());
    }

    private CrearSolicitudRequest pedir(Long espacio, boolean confirmar) {
        return new CrearSolicitudRequest(espacio, lunes, bloque10, "Clase", 5, confirmar);
    }

    @Test
    void otroEspacioALaMismaHoraPideConfirmacion() {
        solicitudes.crear(vict, pedir(lc1, false));

        ApiException sinConfirmar = catchThrowableOfType(ApiException.class,
                () -> solicitudes.crear(vict, pedir(otroEspacio, false)));
        assertThat(sinConfirmar.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sinConfirmar.getCodigo()).isEqualTo(ApiException.CRUCE_PROPIO);
        assertThat(sinConfirmar.getMessage()).isEqualTo("Ya tienes una solicitud en ese horario:\n• LC1 de 17:00 a 18:00");

        assertThat(solicitudes.crear(vict, pedir(otroEspacio, true)).estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void elAvisoMuestraHastaTresYLuegoPuntosSuspensivos() {
        var otros = espacios.findAll().stream().map(e -> e.getId()).filter(id -> !id.equals(lc1)).limit(4).toList();
        solicitudes.crear(vict, pedir(lc1, false));
        for (int i = 0; i < 3; i++) solicitudes.crear(vict, pedir(otros.get(i), true)); // 4 a la misma hora

        ApiException e = catchThrowableOfType(ApiException.class,
                () -> solicitudes.crear(vict, pedir(otros.get(3), false)));
        String[] lineas = e.getMessage().split("\n");
        assertThat(lineas[0]).isEqualTo("Ya tienes 4 solicitudes en ese horario:");
        assertThat(lineas).hasSize(5); // título + 3 solicitudes + "…"
        assertThat(lineas[1]).startsWith("• ").contains(" de 17:00 a 18:00");
        assertThat(lineas[4]).isEqualTo("…");
    }

    @Test
    void elMismoEspacioDosVecesNoAunqueConfirme() {
        solicitudes.crear(vict, pedir(lc1, false));
        ApiException duplicado = catchThrowableOfType(ApiException.class,
                () -> solicitudes.crear(vict, pedir(lc1, true)));
        assertThat(duplicado.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicado.getCodigo()).isNull();
    }
}

package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.dina_backend.dto.Dtos.CrearEspacioRequest;
import cl.duoc.dina_backend.dto.Dtos.EspacioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Edificio;
import cl.duoc.dina_backend.model.Escuela;
import cl.duoc.dina_backend.model.TipoEspacio;
import cl.duoc.dina_backend.service.EspacioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Agregar espacios (administrador). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EspacioCrearTest {

    @Autowired EspacioService espacios;

    private CrearEspacioRequest sala(String codigo) {
        return new CrearEspacioRequest(codigo, "Sala de prueba", Edificio.SUR, 3, TipoEspacio.SALA, 40,
                Escuela.INFORMATICA, true, false, true);
    }

    @Test
    void creaYApareceEnElCatalogo() {
        EspacioDto e = espacios.crear(sala("  s99 "));
        assertThat(e.codigo()).isEqualTo("S99");
        assertThat(e.edificioNombre()).isEqualTo("Edificio Sur");
        assertThat(espacios.buscar(null, null, null, null, null, null))
                .anyMatch(x -> x.id().equals(e.id()));
    }

    @Test
    void noRepiteCodigo() {
        espacios.crear(sala("S98"));
        assertThatThrownBy(() -> espacios.crear(sala("s98")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("S98");
    }
}

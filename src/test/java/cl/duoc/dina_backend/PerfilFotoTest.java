package cl.duoc.dina_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.dina_backend.dto.Dtos.UsuarioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import cl.duoc.dina_backend.service.PerfilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Foto de perfil: subir, reemplazar, leer y quitar. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PerfilFotoTest {

    /** Cabecera mínima de un JPEG (lo que importa es el "número mágico"). */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};

    @Autowired PerfilService perfil;
    @Autowired UsuarioRepository usuarios;

    private Usuario vict() {
        return usuarios.findByCorreoIgnoreCase("vict.godoy@duocuc.cl").orElseThrow();
    }

    @Test
    void sinFotoNoHayUrl() {
        assertThat(UsuarioDto.de(vict()).fotoUrl()).isNull();
    }

    @Test
    void subirReemplazarYQuitar() {
        Usuario u = vict();

        UsuarioDto conFoto = perfil.guardarFoto(u, JPEG);
        assertThat(conFoto.fotoUrl()).startsWith("api/usuarios/" + u.getId() + "/foto?v=");
        assertThat(perfil.foto(u.getId()).getTipo()).isEqualTo("image/jpeg");

        UsuarioDto otra = perfil.guardarFoto(u, PNG);
        assertThat(perfil.foto(u.getId()).getTipo()).isEqualTo("image/png");
        assertThat(otra.fotoUrl()).startsWith("api/usuarios/" + u.getId() + "/foto?v=");

        assertThat(perfil.borrarFoto(u).fotoUrl()).isNull();
        assertThatThrownBy(() -> perfil.foto(u.getId())).isInstanceOf(ApiException.class);
    }

    @Test
    void soloAceptaImagenes() {
        byte[] html = "<html><script>alert(1)</script>".getBytes();
        assertThatThrownBy(() -> perfil.guardarFoto(vict(), html))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("JPG, PNG o WEBP");
        assertThatThrownBy(() -> perfil.guardarFoto(vict(), new byte[0])).isInstanceOf(ApiException.class);
    }
}

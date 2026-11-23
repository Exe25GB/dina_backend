package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.UsuarioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.FotoPerfil;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.PerfilService;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Perfil del usuario: foto. Todas las rutas exigen sesión (AuthInterceptor). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilService perfilService;

    public UsuarioController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /** Sube o reemplaza la foto propia. multipart/form-data con el campo "foto". */
    @PutMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioDto subirFoto(@UsuarioActual Usuario usuario, @RequestPart("foto") MultipartFile foto) {
        try {
            return perfilService.guardarFoto(usuario, foto.getBytes());
        } catch (IOException e) {
            throw ApiException.invalido("No se pudo leer la foto");
        }
    }

    /** Quita la foto propia (vuelven a mostrarse las iniciales). */
    @DeleteMapping("/me/foto")
    public UsuarioDto borrarFoto(@UsuarioActual Usuario usuario) {
        return perfilService.borrarFoto(usuario);
    }

    /**
     * Foto de cualquier usuario (el admin ve la de quien solicita).
     * La URL lleva "?v=..." que cambia con cada foto nueva, por eso se puede guardar en caché.
     */
    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        FotoPerfil foto = perfilService.foto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.getDatos());
    }
}

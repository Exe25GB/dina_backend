package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.dto.Dtos.UsuarioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.FotoPerfil;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.FotoPerfilRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Foto de perfil: se guarda en la base de datos (tabla fotos_perfil). */
@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final FotoPerfilRepository fotoRepository;

    public PerfilService(UsuarioRepository usuarioRepository, FotoPerfilRepository fotoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.fotoRepository = fotoRepository;
    }

    @Transactional
    public UsuarioDto guardarFoto(Usuario actual, byte[] datos) {
        if (datos == null || datos.length == 0) throw ApiException.invalido("La foto está vacía");
        if (datos.length > FotoPerfil.MAX_BYTES) throw ApiException.invalido("La foto no puede pesar más de 2 MB");
        String tipo = tipoDeImagen(datos);
        if (tipo == null) throw ApiException.invalido("La foto debe ser JPG, PNG o WEBP");

        Usuario usuario = buscar(actual);
        FotoPerfil foto = fotoRepository.findById(usuario.getId())
                .orElseGet(() -> new FotoPerfil(usuario.getId(), tipo, datos));
        foto.setTipo(tipo);
        foto.setDatos(datos);
        fotoRepository.save(foto);
        // En milisegundos: la "versión" de la URL debe ser igual después de guardar y leer de la base
        usuario.setFotoActualizadaEn(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        return UsuarioDto.de(usuario);
    }

    @Transactional
    public UsuarioDto borrarFoto(Usuario actual) {
        Usuario usuario = buscar(actual);
        fotoRepository.findById(usuario.getId()).ifPresent(fotoRepository::delete);
        usuario.setFotoActualizadaEn(null);
        return UsuarioDto.de(usuario);
    }

    @Transactional(readOnly = true)
    public FotoPerfil foto(Long usuarioId) {
        return fotoRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("El usuario no tiene foto de perfil"));
    }

    /** El usuario de la sesión viene de otra transacción: se vuelve a leer para poder modificarlo. */
    private Usuario buscar(Usuario actual) {
        return usuarioRepository.findById(actual.getId())
                .orElseThrow(() -> ApiException.noEncontrado("El usuario no existe"));
    }

    /**
     * Tipo según los primeros bytes del archivo ("número mágico"), o null si no es una imagen permitida.
     * No se confía en el Content-Type que envía el cliente: así nunca se sirve otra cosa como imagen.
     */
    static String tipoDeImagen(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}

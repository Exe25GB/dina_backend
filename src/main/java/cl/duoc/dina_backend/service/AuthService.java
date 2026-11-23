package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.config.DinaProperties;
import cl.duoc.dina_backend.dto.Dtos.LoginResponse;
import cl.duoc.dina_backend.dto.Dtos.UsuarioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.SesionToken;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.repository.SesionTokenRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final SesionTokenRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final DinaProperties props;
    private final SecureRandom random = new SecureRandom();
    /** Hash de relleno: compara aunque el usuario no exista, para no revelar qué correos existen por el tiempo de respuesta. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, SesionTokenRepository sesionRepository,
                       PasswordEncoder passwordEncoder, DinaProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.hashFalso = passwordEncoder.encode("no-es-una-clave-real");
    }

    @Transactional
    public LoginResponse login(String correo, String password) {
        String email = correo.trim().toLowerCase(Locale.ROOT);
        if (!esDominioPermitido(email)) {
            throw ApiException.invalido("Usa tu cuenta institucional: "
                    + String.join(" o ", props.dominiosPermitidos().stream().map(d -> "@" + d).toList()));
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(email).orElse(null);
        String hash = usuario != null ? usuario.getPasswordHash() : hashFalso;
        boolean claveOk = passwordEncoder.matches(password, hash);
        if (usuario == null || !claveOk || !usuario.isActivo()) {
            throw ApiException.noAutenticado(CREDENCIALES_INVALIDAS);
        }

        LocalDateTime ahora = LocalDateTime.now(props.zona());
        sesionRepository.borrarExpiradas(ahora); // limpieza de paso

        SesionToken sesion = new SesionToken();
        sesion.setToken(nuevoToken());
        sesion.setUsuario(usuario);
        sesion.setCreadaEn(ahora);
        sesion.setExpiraEn(ahora.plusDays(props.sesion().dias()));
        sesionRepository.save(sesion);

        return new LoginResponse(sesion.getToken(), sesion.getExpiraEn(), UsuarioDto.de(usuario));
    }

    /** Usuario dueño del token, o error 401 si no existe o expiró. */
    @Transactional
    public Usuario usuarioPorToken(String token) {
        SesionToken sesion = sesionRepository.findById(token)
                .orElseThrow(() -> ApiException.noAutenticado("Sesión no válida. Inicia sesión nuevamente"));
        if (sesion.getExpiraEn().isBefore(LocalDateTime.now(props.zona()))) {
            sesionRepository.delete(sesion);
            throw ApiException.noAutenticado("Tu sesión expiró. Inicia sesión nuevamente");
        }
        Usuario usuario = sesion.getUsuario();
        if (!usuario.isActivo()) {
            throw ApiException.noAutenticado("Usuario desactivado");
        }
        return usuario;
    }

    @Transactional
    public void logout(String token) {
        sesionRepository.deleteById(token);
    }

    /** Compara el dominio completo: "x@falso.duocuc.cl" no es válido. */
    private boolean esDominioPermitido(String correo) {
        int arroba = correo.lastIndexOf('@');
        if (arroba < 0) return false;
        String dominio = correo.substring(arroba + 1);
        return props.dominiosPermitidos().stream().anyMatch(d -> d.equalsIgnoreCase(dominio));
    }

    /** 32 bytes aleatorios en hexadecimal (64 caracteres). */
    private String nuevoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

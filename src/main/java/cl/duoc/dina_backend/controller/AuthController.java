package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.LoginRequest;
import cl.duoc.dina_backend.dto.Dtos.LoginResponse;
import cl.duoc.dina_backend.dto.Dtos.UsuarioDto;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Público. Devuelve el token que la app debe guardar y enviar en cada petición. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.correo(), req.password());
    }

    /** Datos del usuario dueño del token (útil al abrir la app con sesión guardada). */
    @GetMapping("/me")
    public UsuarioDto me(@UsuarioActual Usuario usuario) {
        return UsuarioDto.de(usuario);
    }

    /** Invalida el token actual. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@UsuarioActual String token) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}

package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.ConteoDto;
import cl.duoc.dina_backend.dto.Dtos.NotificacionDto;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.NotificacionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Avisos del usuario (pestaña "Avisos" de la app). */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionDto> listar(@UsuarioActual Usuario usuario) {
        return notificacionService.listar(usuario);
    }

    /** Para el globito con el número en el ícono de la campana. */
    @GetMapping("/no-leidas")
    public ConteoDto noLeidas(@UsuarioActual Usuario usuario) {
        return new ConteoDto(notificacionService.contarNoLeidas(usuario));
    }

    @PatchMapping("/{id}/leida")
    public NotificacionDto marcarLeida(@UsuarioActual Usuario usuario, @PathVariable Long id) {
        return notificacionService.marcarLeida(usuario, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@UsuarioActual Usuario usuario, @PathVariable Long id) {
        notificacionService.borrar(usuario, id);
        return ResponseEntity.noContent().build();
    }

    /** Borra todas las notificaciones del usuario. */
    @DeleteMapping
    public ResponseEntity<Void> borrarTodas(@UsuarioActual Usuario usuario) {
        notificacionService.borrarTodas(usuario);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leer-todas")
    public ConteoDto leerTodas(@UsuarioActual Usuario usuario) {
        notificacionService.marcarTodasLeidas(usuario);
        return new ConteoDto(0);
    }
}

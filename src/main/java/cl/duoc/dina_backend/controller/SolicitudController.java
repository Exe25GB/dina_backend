package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.CrearSolicitudRequest;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.SolicitudService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Solicitudes del usuario que hace la petición. */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /** Pide uno o varios bloques seguidos de un espacio. Queda PENDIENTE de aprobación. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto crear(@UsuarioActual Usuario usuario, @Valid @RequestBody CrearSolicitudRequest req) {
        return solicitudService.crear(usuario, req);
    }

    @GetMapping("/mias")
    public List<SolicitudDto> mias(@UsuarioActual Usuario usuario) {
        return solicitudService.mias(usuario);
    }

    /** Una solicitud: la propia, o cualquiera si es administrador. */
    @GetMapping("/{id}")
    public SolicitudDto detalle(@UsuarioActual Usuario usuario, @PathVariable Long id) {
        return solicitudService.detalle(usuario, id);
    }

    @PatchMapping("/{id}/cancelar")
    public SolicitudDto cancelar(@UsuarioActual Usuario usuario, @PathVariable Long id) {
        return solicitudService.cancelar(usuario, id);
    }
}

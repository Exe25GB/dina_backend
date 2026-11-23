package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.AprobarRequest;
import cl.duoc.dina_backend.dto.Dtos.RechazarRequest;
import cl.duoc.dina_backend.dto.Dtos.SolicitudDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.EstadoSolicitud;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.SolicitudService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Revisión de solicitudes. Solo ADMIN (lo exige AuthInterceptor y se verifica otra vez aquí). */
@RestController
@RequestMapping("/api/admin/solicitudes")
public class AdminSolicitudController {

    private final SolicitudService solicitudService;

    public AdminSolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /** Ej: /api/admin/solicitudes?estado=PENDIENTE (sin estado = todas). */
    @GetMapping
    public List<SolicitudDto> listar(@UsuarioActual Usuario admin,
                                     @RequestParam(required = false) EstadoSolicitud estado) {
        exigirAdmin(admin);
        return solicitudService.listar(estado);
    }

    @PatchMapping("/{id}/aprobar")
    public SolicitudDto aprobar(@UsuarioActual Usuario admin, @PathVariable Long id,
                                @Valid @RequestBody(required = false) AprobarRequest req) {
        exigirAdmin(admin);
        return solicitudService.aprobar(admin, id, req == null ? null : req.comentario());
    }

    @PatchMapping("/{id}/rechazar")
    public SolicitudDto rechazar(@UsuarioActual Usuario admin, @PathVariable Long id,
                                 @Valid @RequestBody RechazarRequest req) {
        exigirAdmin(admin);
        return solicitudService.rechazar(admin, id, req.motivo());
    }

    private static void exigirAdmin(Usuario usuario) {
        if (!usuario.esAdmin()) throw ApiException.prohibido("Solo un administrador puede hacer esto");
    }
}

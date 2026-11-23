package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.CrearEspacioRequest;
import cl.duoc.dina_backend.dto.Dtos.EspacioDto;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.EspacioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Administración de espacios. Solo ADMIN (lo exige AuthInterceptor y se verifica otra vez aquí). */
@RestController
@RequestMapping("/api/admin/espacios")
public class AdminEspacioController {

    private final EspacioService espacioService;

    public AdminEspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /** Agrega un espacio reservable. Responde 409 si el código ya existe. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspacioDto crear(@UsuarioActual Usuario admin, @Valid @RequestBody CrearEspacioRequest req) {
        if (!admin.esAdmin()) throw ApiException.prohibido("Solo un administrador puede hacer esto");
        return espacioService.crear(req);
    }
}

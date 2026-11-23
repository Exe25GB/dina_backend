package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.*;
import cl.duoc.dina_backend.model.Edificio;
import cl.duoc.dina_backend.model.TipoEspacio;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.EspacioService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /**
     * Catálogo de espacios con filtros opcionales.
     * Ej: /api/espacios?edificio=CTI&capacidadMin=30&computadores=true
     */
    @GetMapping("/espacios")
    public List<EspacioDto> buscar(@RequestParam(required = false) Edificio edificio,
                                   @RequestParam(required = false) TipoEspacio tipo,
                                   @RequestParam(required = false) Integer capacidadMin,
                                   @RequestParam(required = false) Boolean proyector,
                                   @RequestParam(required = false) Boolean computadores,
                                   @RequestParam(required = false) Boolean climatizacion) {
        return espacioService.buscar(edificio, tipo, capacidadMin, proyector, computadores, climatizacion);
    }

    @GetMapping("/espacios/{id}")
    public EspacioDto obtener(@PathVariable Long id) {
        return espacioService.obtener(id);
    }

    /** Estado de cada bloque del día. Ej: /api/espacios/5/disponibilidad?fecha=2026-11-25 */
    @GetMapping("/espacios/{id}/disponibilidad")
    public DisponibilidadDto disponibilidad(@PathVariable Long id,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                            @UsuarioActual Usuario usuario) {
        return espacioService.disponibilidad(id, fecha, usuario);
    }

    /** Los bloques de 1 hora que se pueden pedir. */
    @GetMapping("/bloques")
    public List<BloqueDto> bloques() {
        return espacioService.bloques();
    }

    @GetMapping("/edificios")
    public List<EdificioDto> edificios() {
        return Arrays.stream(Edificio.values()).map(e -> new EdificioDto(e, e.getNombre())).toList();
    }
}

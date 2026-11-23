package cl.duoc.dina_backend.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público. Sirve para comprobar que el backend está arriba (p. ej. en EC2: http://IP:8080/api/salud). */
@RestController
public class SaludController {

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "servicio", "dina_backend");
    }
}

package cl.duoc.dina_backend.config;

import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reglas de negocio configurables (prefijo "dina." en application.properties). */
@ConfigurationProperties(prefix = "dina")
public record DinaProperties(
        String zonaHoraria,
        List<String> dominiosPermitidos,
        Solicitudes solicitudes,
        Sesion sesion,
        Seed seed
) {
    public record Solicitudes(int maxBloques, int maxDiasAnticipacion) {}

    public record Sesion(int dias) {}

    /** Contraseña de los usuarios de prueba que se crean al iniciar. */
    public record Seed(String passwordInicial) {}

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}

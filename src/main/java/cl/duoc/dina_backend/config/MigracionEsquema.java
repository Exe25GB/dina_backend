package cl.duoc.dina_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes al esquema de bases de datos que ya existían.
 * Hibernate (ddl-auto=update) agrega tablas y columnas nuevas, pero NO cambia el largo de una
 * columna existente. Cada sentencia es idempotente: se puede ejecutar en cada inicio sin problema.
 * Se usa "MODIFY COLUMN", que entienden MySQL y H2 en modo MySQL (el perfil h2 usa MODE=MySQL).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // antes que DataInitializer
public class MigracionEsquema implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionEsquema.class);

    private final JdbcTemplate jdbc;

    public MigracionEsquema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // El motivo de la reserva ("actividad") pasó de 150 a 250 caracteres
        ejecutar("ALTER TABLE solicitudes MODIFY COLUMN actividad VARCHAR(250) NOT NULL");
        // Chat: las solicitudes aprobadas antes de que existiera "aprobada_en" también tienen chat
        ejecutar("UPDATE solicitudes SET aprobada_en = revisada_en WHERE estado = 'APROBADA' AND aprobada_en IS NULL");
        // Columnas de enums como texto (Hibernate las había creado como ENUM de la base, que rechaza
        // los valores nuevos; p. ej. la notificación RESERVA_CANCELADA)
        ejecutar("ALTER TABLE notificaciones MODIFY COLUMN tipo VARCHAR(30) NOT NULL");
        ejecutar("ALTER TABLE solicitudes MODIFY COLUMN estado VARCHAR(20) NOT NULL");
        ejecutar("ALTER TABLE usuarios MODIFY COLUMN rol VARCHAR(20) NOT NULL");
        ejecutar("ALTER TABLE espacios MODIFY COLUMN edificio VARCHAR(20) NOT NULL");
        ejecutar("ALTER TABLE espacios MODIFY COLUMN tipo VARCHAR(20) NOT NULL");
        ejecutar("ALTER TABLE espacios MODIFY COLUMN escuela VARCHAR(40)");
    }

    private void ejecutar(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            // No detiene el inicio: se registra para revisarlo
            log.warn("No se pudo aplicar el ajuste de esquema [{}]: {}", sql, e.getMessage());
        }
    }
}

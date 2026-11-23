package cl.duoc.dina_backend.config;

import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.BloqueHorarioRepository;
import cl.duoc.dina_backend.repository.EspacioRepository;
import cl.duoc.dina_backend.repository.UsuarioRepository;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los datos base al iniciar. Solo crea lo que falta, así que se puede reiniciar sin duplicar.
 *
 * Espacios: tomados de MAPA.pdf (sede Viña del Mar), edificios Norte, Central, Sur y CTI.
 * Quedan fuera los que no se reservan por bloques (Biblioteca, Casino, Cafetería, Capilla,
 * Pañol, CETECOM, CPD, CTA, Edificio Administrativo).
 * Capacidades, equipamiento y escuela de uso principal son SUPUESTOS para pruebas: ajústalos con datos reales.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final BloqueHorarioRepository bloqueRepository;
    private final PasswordEncoder passwordEncoder;
    private final DinaProperties props;

    public DataInitializer(UsuarioRepository usuarioRepository, EspacioRepository espacioRepository,
                           BloqueHorarioRepository bloqueRepository, PasswordEncoder passwordEncoder,
                           DinaProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.espacioRepository = espacioRepository;
        this.bloqueRepository = bloqueRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearUsuarios();
        crearBloques();
        crearEspacios();
    }

    // ── Usuarios de prueba ──────────────────────────────────────────────

    private void crearUsuarios() {
        // Nombre y apellido: la app arma las iniciales de la foto por defecto con ellos ("EG", "VG")
        usuario("exe.guzman@duocuc.cl", "Exequiel Guzman", Rol.ADMIN);
        usuario("vict.godoy@duocuc.cl", "Victor Godoy", Rol.USUARIO);
        // Docente: lo indica el dominio @profesor.duoc.cl (en el backend es un USUARIO normal)
        usuario("nan.bernal@profesor.duoc.cl", "Nancy Beatriz Bernal", Rol.USUARIO);
    }

    /** Crea el usuario de prueba; si ya existe, solo corrige el nombre (no toca contraseña, rol ni foto). */
    private void usuario(String correo, String nombre, Rol rol) {
        var existente = usuarioRepository.findByCorreoIgnoreCase(correo);
        if (existente.isPresent()) {
            Usuario u = existente.get();
            if (!u.getNombre().equals(nombre)) {
                log.info("Usuario de prueba {}: nombre '{}' → '{}'", correo, u.getNombre(), nombre);
                u.setNombre(nombre);
            }
            return;
        }
        String hash = passwordEncoder.encode(props.seed().passwordInicial());
        usuarioRepository.save(new Usuario(correo, nombre, hash, rol));
        log.info("Usuario de prueba creado: {} ({})", correo, rol);
    }

    // ── Bloques de 1 hora: 08:00 a 22:00 ────────────────────────────────

    private void crearBloques() {
        if (bloqueRepository.count() > 0) return;
        LocalTime inicio = LocalTime.of(8, 0);
        for (int numero = 1; numero <= 14; numero++) {
            bloqueRepository.save(new BloqueHorario(numero, inicio, inicio.plusHours(1)));
            inicio = inicio.plusHours(1);
        }
        log.info("Bloques horarios creados: 14 (08:00 a 22:00)");
    }

    // ── Espacios según MAPA.pdf ─────────────────────────────────────────

    private void crearEspacios() {
        int antes = (int) espacioRepository.count();

        // Edificio Norte
        salas("S1", 1, 3, Edificio.NORTE, 1, 35, null);                  // S11 · S13
        espacio("LAB-IND", "Laboratorio Industrial", Edificio.NORTE, 1, TipoEspacio.LABORATORIO, 25,
                Escuela.INGENIERIA_RN_MEDIO_AMBIENTE, true, true, true);
        espacio("LAB-DIS", "Laboratorio de Diseño", Edificio.NORTE, 1, TipoEspacio.LABORATORIO, 25,
                Escuela.DISENO, true, true, true);
        salas("S2", 1, 3, Edificio.NORTE, 2, 35, null);                  // S21 · S23
        espacio("SET-TV", "Set de TV", Edificio.NORTE, 2, TipoEspacio.ESTUDIO, 20,
                Escuela.COMUNICACION, false, true, true);
        espacio("SONIDO-1", "Estudio de Sonido 1", Edificio.NORTE, 2, TipoEspacio.ESTUDIO, 10,
                Escuela.COMUNICACION, false, true, true);
        salas("S3", 1, 9, Edificio.NORTE, 3, 40, null);                  // S31 · S39
        salas("S4", 1, 9, Edificio.NORTE, 4, 40, null);                  // S41 · S49

        // Edificio Central (el mapa no indica pisos: se infieren del código o se asume piso 1-2)
        salas("S5", 3, 4, Edificio.CENTRAL, 5, 35, null);                // S53 · S54
        espacio("SONIDO-2", "Estudio de Sonido 2", Edificio.CENTRAL, 2, TipoEspacio.ESTUDIO, 10,
                Escuela.COMUNICACION, false, true, true);
        espacio("POST-PROD", "Sala de Post-producción", Edificio.CENTRAL, 2, TipoEspacio.LABORATORIO, 15,
                Escuela.COMUNICACION, true, true, true);
        espacio("MULTICANCHA-1", "Multicancha 1", Edificio.CENTRAL, 1, TipoEspacio.DEPORTIVO, 60,
                null, false, false, false);
        espacio("ANFITEATRO", "Anfiteatro", Edificio.CENTRAL, 1, TipoEspacio.AUDITORIO, 120,
                null, true, false, false);

        // Edificio Sur
        espacio("ACTUACION", "Sala de Actuación", Edificio.SUR, 5, TipoEspacio.MULTIPROPOSITO, 30,
                Escuela.COMUNICACION, false, false, true);
        espacio("MULTICANCHA-2", "Multicancha 2", Edificio.SUR, 5, TipoEspacio.DEPORTIVO, 60,
                null, false, false, false);
        salas("S6", 1, 5, Edificio.SUR, 6, 40, null);                    // S61 · S65
        salas("S7", 1, 6, Edificio.SUR, 7, 40, null);                    // S71 · S76
        salas("S8", 1, 8, Edificio.SUR, 8, 40, null);                    // S81 · S88
        espacio("GIMNASIO", "Gimnasio", Edificio.SUR, 8, TipoEspacio.DEPORTIVO, 30,
                null, false, false, true);

        // Edificio Tecnológico CTI
        for (int i = 1; i <= 12; i++) {                                  // LC1 · LC12
            espacio("LC" + i, "Laboratorio de Computación " + i, Edificio.CTI, 2, TipoEspacio.LABORATORIO, 30,
                    Escuela.INFORMATICA, true, true, true);
        }
        espacio("S302", "Sala S302", Edificio.CTI, 3, TipoEspacio.SALA, 35,
                Escuela.INFORMATICA, true, false, true);
        espacio("COWORK-CIIT", "Espacio Cowork + CIIT", Edificio.CTI, 4, TipoEspacio.MULTIPROPOSITO, 40,
                null, true, false, true);
        for (int i = 1; i <= 9; i++) {                                   // S601 · S609
            espacio("S60" + i, "Sala S60" + i, Edificio.CTI, 6, TipoEspacio.SALA, 40,
                    null, true, false, true);
        }

        int creados = (int) espacioRepository.count() - antes;
        if (creados > 0) log.info("Espacios creados: {}", creados);
    }

    /** Salas comunes numeradas, por ejemplo prefijo "S4" de 1 a 9 → S41 … S49. */
    private void salas(String prefijo, int desde, int hasta, Edificio edificio, int piso, int capacidad, Escuela escuela) {
        for (int i = desde; i <= hasta; i++) {
            String codigo = prefijo + i;
            espacio(codigo, "Sala " + codigo, edificio, piso, TipoEspacio.SALA, capacidad, escuela, true, false, false);
        }
    }

    private void espacio(String codigo, String nombre, Edificio edificio, int piso, TipoEspacio tipo,
                         int capacidad, Escuela escuela, boolean proyector, boolean computadores, boolean climatizacion) {
        if (espacioRepository.existsByCodigo(codigo)) return;
        Espacio e = new Espacio();
        e.setCodigo(codigo);
        e.setNombre(nombre);
        e.setEdificio(edificio);
        e.setPiso(piso);
        e.setTipo(tipo);
        e.setCapacidad(capacidad);
        e.setEscuela(escuela);
        e.setProyector(proyector);
        e.setComputadores(computadores);
        e.setClimatizacion(climatizacion);
        espacioRepository.save(e);
    }
}

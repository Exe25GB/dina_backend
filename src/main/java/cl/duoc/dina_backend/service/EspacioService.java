package cl.duoc.dina_backend.service;

import cl.duoc.dina_backend.dto.Dtos.*;
import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.*;
import cl.duoc.dina_backend.repository.BloqueHorarioRepository;
import cl.duoc.dina_backend.repository.EspacioRepository;
import cl.duoc.dina_backend.repository.SolicitudRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final BloqueHorarioRepository bloqueRepository;
    private final SolicitudRepository solicitudRepository;
    private final HorarioService horario;

    public EspacioService(EspacioRepository espacioRepository, BloqueHorarioRepository bloqueRepository,
                          SolicitudRepository solicitudRepository, HorarioService horario) {
        this.espacioRepository = espacioRepository;
        this.bloqueRepository = bloqueRepository;
        this.solicitudRepository = solicitudRepository;
        this.horario = horario;
    }

    @Transactional(readOnly = true)
    public List<EspacioDto> buscar(Edificio edificio, TipoEspacio tipo, Integer capacidadMin,
                                   Boolean proyector, Boolean computadores, Boolean climatizacion) {
        return espacioRepository.buscar(edificio, tipo, capacidadMin).stream()
                .filter(e -> proyector == null || !proyector || e.isProyector())
                .filter(e -> computadores == null || !computadores || e.isComputadores())
                .filter(e -> climatizacion == null || !climatizacion || e.isClimatizacion())
                .map(EspacioDto::de)
                .toList();
    }

    /** Solo ADMIN. El código se guarda en mayúsculas y no se puede repetir ("s 41" → "S 41"). */
    @Transactional
    public EspacioDto crear(CrearEspacioRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (espacioRepository.existsByCodigo(codigo)) {
            throw ApiException.conflicto("Ya existe un espacio con el código " + codigo);
        }
        Espacio e = new Espacio();
        e.setCodigo(codigo);
        e.setNombre(req.nombre().trim());
        e.setEdificio(req.edificio());
        e.setPiso(req.piso());
        e.setTipo(req.tipo());
        e.setCapacidad(req.capacidad());
        e.setEscuela(req.escuela());
        e.setProyector(req.proyector());
        e.setComputadores(req.computadores());
        e.setClimatizacion(req.climatizacion());
        return EspacioDto.de(espacioRepository.save(e));
    }

    @Transactional(readOnly = true)
    public EspacioDto obtener(Long id) {
        return EspacioDto.de(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<BloqueDto> bloques() {
        return bloqueRepository.findAllByOrderByNumeroAsc().stream().map(BloqueDto::de).toList();
    }

    /** Estado de cada bloque del día para un espacio: disponible, ocupado o no disponible. */
    @Transactional(readOnly = true)
    public DisponibilidadDto disponibilidad(Long espacioId, LocalDate fecha, Usuario usuario) {
        Espacio espacio = buscarActivo(espacioId);

        List<Solicitud> solicitudes = solicitudRepository.findByEspacioIdAndFechaAndEstadoIn(
                espacioId, fecha, List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADA));

        Set<Long> ocupados = new HashSet<>();
        Set<Long> mios = new HashSet<>();
        Map<Long, Integer> pendientes = new HashMap<>();
        for (Solicitud s : solicitudes) {
            boolean esMia = s.getUsuario().getId().equals(usuario.getId());
            for (BloqueHorario b : s.getBloques()) {
                if (s.getEstado() == EstadoSolicitud.APROBADA) ocupados.add(b.getId());
                else pendientes.merge(b.getId(), 1, Integer::sum);
                if (esMia) mios.add(b.getId());
            }
        }

        boolean fueraDeRango = fecha.isAfter(horario.fechaMaxima());
        List<BloqueDisponibilidadDto> bloques = bloqueRepository.findAllByOrderByNumeroAsc().stream()
                .map(b -> {
                    EstadoBloque estado;
                    if (ocupados.contains(b.getId())) estado = EstadoBloque.OCUPADO;
                    else if (fueraDeRango || !horario.solicitable(fecha, b)) estado = EstadoBloque.NO_DISPONIBLE;
                    else estado = EstadoBloque.DISPONIBLE;
                    return new BloqueDisponibilidadDto(b.getId(), b.getNumero(), b.getHoraInicio(), b.getHoraFin(),
                            estado, pendientes.getOrDefault(b.getId(), 0), mios.contains(b.getId()));
                })
                .toList();

        return new DisponibilidadDto(EspacioDto.de(espacio), fecha, bloques);
    }

    Espacio buscarActivo(Long id) {
        return espacioRepository.findById(id)
                .filter(Espacio::isActivo)
                .orElseThrow(() -> ApiException.noEncontrado("Espacio no encontrado"));
    }
}

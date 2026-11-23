package cl.duoc.dina_backend.config;

import cl.duoc.dina_backend.service.RecordatorioService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa cada 5 minutos si hay reservas que empiezan dentro de la próxima hora.
 * Se desactiva con dina.recordatorios.activos=false (lo hacen las pruebas).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "dina.recordatorios.activos", havingValue = "true", matchIfMissing = true)
public class RecordatorioProgramado {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioProgramado.class);

    private final RecordatorioService recordatorios;
    private final DinaProperties props;

    public RecordatorioProgramado(RecordatorioService recordatorios, DinaProperties props) {
        this.recordatorios = recordatorios;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT30S")
    public void revisar() {
        int enviados = recordatorios.enviarRecordatorios(LocalDateTime.now(props.zona()));
        if (enviados > 0) log.info("Recordatorios de reserva enviados: {}", enviados);
    }
}

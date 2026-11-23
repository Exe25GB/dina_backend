package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mensaje del chat de una solicitud aprobada (uno por solicitud: el usuario que la pidió y los
 * administradores). Sin [autor] es un mensaje del sistema ("Solicitud aprobada", "Reserva cancelada"...).
 */
@Entity
@Table(name = "chat_mensajes", indexes = @Index(columnList = "solicitud_id, id"))
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {

    public static final int MAX_TEXTO = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    /** null = mensaje del sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false, length = MAX_TEXTO)
    private String texto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Mensaje(Solicitud solicitud, Usuario autor, String texto, LocalDateTime creadoEn) {
        this.solicitud = solicitud;
        this.autor = autor;
        this.texto = texto;
        this.creadoEn = creadoEn;
    }
}

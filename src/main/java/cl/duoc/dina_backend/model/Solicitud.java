package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido de un usuario para usar un espacio en una fecha, en uno o varios bloques seguidos.
 * Queda PENDIENTE hasta que un administrador la aprueba o la rechaza.
 */
@Entity
@Table(name = "solicitudes", indexes = @Index(columnList = "espacio_id, fecha, estado"))
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Bloques pedidos, siempre seguidos y ordenados por número. */
    @ManyToMany
    @JoinTable(
            name = "solicitud_bloques",
            joinColumns = @JoinColumn(name = "solicitud_id"),
            inverseJoinColumns = @JoinColumn(name = "bloque_id"))
    @OrderBy("numero ASC")
    private List<BloqueHorario> bloques = new ArrayList<>();

    /** Actividad o curso para el que se pide el espacio. */
    @Column(nullable = false, length = 250)
    private String actividad;

    @Column(nullable = false)
    private int asistentes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto, no ENUM de la base: así se pueden agregar valores nuevos
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    /** Comentario del administrador al aprobar o rechazar (opcional). */
    @Column(name = "comentario_admin", length = 300)
    private String comentarioAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisada_por_id")
    private Usuario revisadaPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "revisada_en")
    private LocalDateTime revisadaEn;

    /**
     * Cuándo se aprobó (null = nunca). Desde ese momento la solicitud tiene chat, aunque después
     * se cancele: por eso no basta con mirar el estado.
     */
    @Column(name = "aprobada_en")
    private LocalDateTime aprobadaEn;

    /** Cuándo un administrador pasó el chat a "Cerrados" (null = no lo ha cerrado). */
    @Column(name = "chat_cerrado_en")
    private LocalDateTime chatCerradoEn;

    /** Cuándo se avisó "tu reserva es hoy" (null = todavía no): así se avisa una sola vez. */
    @Column(name = "recordatorio_enviado_en")
    private LocalDateTime recordatorioEnviadoEn;
}

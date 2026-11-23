package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aviso para un usuario (la app las consulta y muestra en la pestaña Avisos). */
@Entity
@Table(name = "notificaciones", indexes = @Index(columnList = "usuario_id, leida"))
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto, no ENUM de la base: así se pueden agregar valores nuevos
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String mensaje;

    /** Solicitud relacionada, para que la app pueda abrir su detalle. */
    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}

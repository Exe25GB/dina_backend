package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sala, laboratorio u otro recinto que se puede solicitar por bloques. */
@Entity
@Table(name = "espacios")
@Getter
@Setter
@NoArgsConstructor
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible en la sede, por ejemplo "S41" o "LC3". */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto, no ENUM de la base: así se pueden agregar valores nuevos
    @Column(nullable = false, length = 20)
    private Edificio edificio;

    @Column(nullable = false)
    private int piso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto, no ENUM de la base: así se pueden agregar valores nuevos
    @Column(nullable = false, length = 20)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private int capacidad;

    /** Escuela de uso principal; null si es de uso general. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto, no ENUM de la base: así se pueden agregar valores nuevos
    @Column(length = 40)
    private Escuela escuela;

    private boolean proyector;
    private boolean computadores;
    private boolean climatizacion;

    /** Si es false, no aparece ni se puede solicitar (p. ej. en mantención). */
    @Column(nullable = false)
    private boolean activo = true;
}

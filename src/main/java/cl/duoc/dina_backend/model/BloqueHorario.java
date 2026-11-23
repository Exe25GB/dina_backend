package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bloque de 1 hora. Los usuarios solicitan uno o varios bloques seguidos. */
@Entity
@Table(name = "bloques_horario")
@Getter
@Setter
@NoArgsConstructor
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Orden del bloque en el día (1 = primero). Dos bloques son seguidos si sus números son consecutivos. */
    @Column(nullable = false, unique = true)
    private int numero;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    public BloqueHorario(int numero, LocalTime horaInicio, LocalTime horaFin) {
        this.numero = numero;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }
}

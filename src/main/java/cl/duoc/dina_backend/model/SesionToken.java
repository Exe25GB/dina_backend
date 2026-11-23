package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Token que recibe la app al iniciar sesión; se envía en cada petición (Authorization: Bearer ...). */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
@NoArgsConstructor
public class SesionToken {

    @Id
    @Column(length = 64)
    private String token;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;
}

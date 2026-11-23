package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Contraseña cifrada con BCrypt (nunca en texto plano). */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    /**
     * Cuándo se cambió la foto de perfil (null = sin foto). La imagen vive en la tabla fotos_perfil
     * para no cargarla cada vez que se lee un usuario; esta fecha es la "versión" de la URL,
     * así la app no muestra una foto antigua guardada en caché.
     */
    @Column(name = "foto_actualizada_en")
    private LocalDateTime fotoActualizadaEn;

    public Usuario(String correo, String nombre, String passwordHash, Rol rol) {
        this.correo = correo;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }
}

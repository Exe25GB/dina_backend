package cl.duoc.dina_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Foto de perfil de un usuario (una por usuario). La imagen ya viene recortada desde la app. */
@Entity
@Table(name = "fotos_perfil")
@Getter
@Setter
@NoArgsConstructor
public class FotoPerfil {

    /** 2 MB: la app envía ~512×512 px en JPEG (unos 50–100 KB), esto es solo un tope. */
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    /** Mismo id del usuario dueño de la foto. */
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** image/jpeg, image/png o image/webp (detectado por el contenido, no por lo que dice el cliente). */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Lob
    @Column(nullable = false, length = MAX_BYTES)
    private byte[] datos;

    public FotoPerfil(Long usuarioId, String tipo, byte[] datos) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.datos = datos;
    }
}

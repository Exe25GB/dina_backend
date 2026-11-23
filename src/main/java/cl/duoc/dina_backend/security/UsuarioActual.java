package cl.duoc.dina_backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parámetro de controlador para recibir el usuario que hizo la petición.
 * Ejemplo: {@code public List<...> mias(@UsuarioActual Usuario usuario)}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioActual {
}

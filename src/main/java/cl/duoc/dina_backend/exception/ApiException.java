package cl.duoc.dina_backend.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con su código HTTP y un mensaje que la app puede mostrar tal cual. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    /** Para que la app reconozca un caso y actúe distinto (null en la mayoría de los errores). */
    private final String codigo;

    public ApiException(HttpStatus status, String mensaje) {
        this(status, mensaje, null);
    }

    public ApiException(HttpStatus status, String mensaje, String codigo) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    /** El usuario ya tiene otra solicitud a esa hora: la app pregunta y, si acepta, reenvía confirmando. */
    public static final String CRUCE_PROPIO = "CRUCE_PROPIO";

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException invalido(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    public static ApiException noAutenticado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }
}

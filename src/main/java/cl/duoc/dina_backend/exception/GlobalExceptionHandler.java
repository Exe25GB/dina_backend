package cl.duoc.dina_backend.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte cualquier error en un JSON con la misma forma:
 * { "estado": 409, "error": "CONFLICT", "mensaje": "...", "fecha": "..." }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(int estado, String error, String mensaje, LocalDateTime fecha) {
        static ResponseEntity<ErrorResponse> de(HttpStatus status, String mensaje) {
            return ResponseEntity.status(status)
                    .body(new ErrorResponse(status.value(), status.name(), mensaje, LocalDateTime.now()));
        }
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e) {
        return ErrorResponse.de(e.getStatus(), e.getMessage());
    }

    /** Errores de @Valid: junta los mensajes de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ErrorResponse.de(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException e) {
        return ErrorResponse.de(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ErrorResponse.de(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + e.getName() + "'");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaNoExiste(NoResourceFoundException e) {
        return ErrorResponse.de(HttpStatus.NOT_FOUND, "La ruta no existe");
    }

    /** Cualquier otro error: se registra en el log y no se exponen detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return ErrorResponse.de(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }
}

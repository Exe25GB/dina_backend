package cl.duoc.dina_backend.model;

/** Estado del chat de una solicitud aprobada. */
public enum EstadoChat {
    /** Se puede escribir: la reserva aún no termina y el administrador no lo cerró. */
    ABIERTO,
    /** Solo lectura: la reserva ya terminó, o el administrador lo cerró. */
    CERRADO
}

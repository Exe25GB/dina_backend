package cl.duoc.dina_backend.model;

public enum EstadoSolicitud {
    /** Esperando revisión del administrador. */
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    /** Cancelada por el mismo usuario, o por un administrador si ya estaba aprobada. */
    CANCELADA
}

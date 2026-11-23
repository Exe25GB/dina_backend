package cl.duoc.dina_backend.model;

public enum EstadoSolicitud {
    /** Esperando revisión del administrador. */
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    /** Cancelada por el mismo usuario. */
    CANCELADA
}

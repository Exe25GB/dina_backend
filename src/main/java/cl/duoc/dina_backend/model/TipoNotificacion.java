package cl.duoc.dina_backend.model;

public enum TipoNotificacion {
    /** Para administradores: llegó una solicitud por revisar. */
    NUEVA_SOLICITUD,
    SOLICITUD_APROBADA,
    SOLICITUD_RECHAZADA,
    /** Un administrador canceló una reserva que ya estaba aprobada. */
    RESERVA_CANCELADA
}

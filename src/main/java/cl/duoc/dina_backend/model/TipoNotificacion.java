package cl.duoc.dina_backend.model;

public enum TipoNotificacion {
    /** Para administradores: llegó una solicitud por revisar. */
    NUEVA_SOLICITUD,
    SOLICITUD_APROBADA,
    SOLICITUD_RECHAZADA,
    /** Un administrador canceló una reserva que ya estaba aprobada. */
    RESERVA_CANCELADA,
    /** Para administradores: el usuario canceló su reserva ya aprobada. */
    CANCELADA_POR_USUARIO,
    /** Mensaje nuevo en un chat (uno por chat mientras no se lea: se actualiza con el último). */
    MENSAJE_NUEVO,
    /** La reserva aprobada empieza pronto (una hora antes). */
    RECORDATORIO_RESERVA
}

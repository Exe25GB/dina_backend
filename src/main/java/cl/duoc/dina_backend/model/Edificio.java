package cl.duoc.dina_backend.model;

/** Edificios de la sede Viña del Mar con espacios que se pueden solicitar (ver MAPA.pdf). */
public enum Edificio {
    NORTE("Edificio Norte"),
    CENTRAL("Edificio Central"),
    SUR("Edificio Sur"),
    CTI("Edificio Tecnológico CTI");

    private final String nombre;

    Edificio(String nombre) { this.nombre = nombre; }

    public String getNombre() { return nombre; }
}

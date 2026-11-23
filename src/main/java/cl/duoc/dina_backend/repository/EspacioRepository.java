package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.Edificio;
import cl.duoc.dina_backend.model.Espacio;
import cl.duoc.dina_backend.model.TipoEspacio;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    boolean existsByCodigo(String codigo);

    /** Filtros opcionales: si un parámetro es null, no filtra por él. */
    @Query("""
            select e from Espacio e
            where e.activo = true
              and (:edificio is null or e.edificio = :edificio)
              and (:tipo is null or e.tipo = :tipo)
              and (:capacidadMin is null or e.capacidad >= :capacidadMin)
            order by e.edificio, e.piso, e.codigo
            """)
    List<Espacio> buscar(@Param("edificio") Edificio edificio,
                         @Param("tipo") TipoEspacio tipo,
                         @Param("capacidadMin") Integer capacidadMin);

    /**
     * Bloquea la fila del espacio hasta terminar la transacción.
     * Evita que dos administradores aprueben a la vez dos solicitudes que chocan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> findByIdParaActualizar(@Param("id") Long id);
}

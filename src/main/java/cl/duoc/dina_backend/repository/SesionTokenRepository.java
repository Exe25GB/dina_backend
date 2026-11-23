package cl.duoc.dina_backend.repository;

import cl.duoc.dina_backend.model.SesionToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionTokenRepository extends JpaRepository<SesionToken, String> {

    @Modifying
    @Query("delete from SesionToken s where s.expiraEn < :ahora")
    int borrarExpiradas(@Param("ahora") LocalDateTime ahora);
}

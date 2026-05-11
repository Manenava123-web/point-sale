package mx.com.sale.repository;

import mx.com.sale.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, String> {
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
    List<Venta> findByUsuarioInAndFechaBetween(java.util.Collection<String> usuarios, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT v FROM Venta v WHERE LOWER(v.id) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY v.fecha DESC")
    List<Venta> findByIdPrefix(@Param("prefix") String prefix);
}

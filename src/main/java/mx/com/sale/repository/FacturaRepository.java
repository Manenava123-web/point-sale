package mx.com.sale.repository;

import mx.com.sale.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, String> {

    Optional<Factura> findByVentaId(String ventaId);

    Page<Factura> findAllByOrderByFechaDesc(Pageable pageable);

    @Query("select coalesce(max(f.numero), 0) from Factura f")
    long maxNumero();
}

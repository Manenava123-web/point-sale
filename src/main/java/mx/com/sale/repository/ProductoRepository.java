package mx.com.sale.repository;

import mx.com.sale.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {
    Optional<Producto> findByCodeIgnoreCase(String code);
    Page<Producto> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Producto> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
    List<Producto> findByStockLessThanEqualOrderByStockAsc(int umbral);
}

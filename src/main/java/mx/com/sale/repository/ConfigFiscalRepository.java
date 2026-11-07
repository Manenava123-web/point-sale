package mx.com.sale.repository;

import mx.com.sale.model.ConfigFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigFiscalRepository extends JpaRepository<ConfigFiscal, Long> {
}

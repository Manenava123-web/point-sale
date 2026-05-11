package mx.com.sale.repository;

import mx.com.sale.model.NegocioConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NegocioConfigRepository extends JpaRepository<NegocioConfig, Long> {}

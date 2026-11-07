package mx.com.sale.repository;

import mx.com.sale.model.PrestamoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PrestamoCajaRepository extends JpaRepository<PrestamoCaja, String> {

    List<PrestamoCaja> findByDevueltoFalseOrderByFechaAsc();

    List<PrestamoCaja> findByDevueltoFalseAndUsuarioInOrderByFechaAsc(Collection<String> usuarios);

    List<PrestamoCaja> findByFechaBetweenOrderByFechaAsc(LocalDateTime inicio, LocalDateTime fin);

    List<PrestamoCaja> findByUsuarioInAndFechaBetweenOrderByFechaAsc(
            Collection<String> usuarios, LocalDateTime inicio, LocalDateTime fin);

    List<PrestamoCaja> findByDevueltoTrueAndFechaDevolucionBetweenOrderByFechaDevolucionAsc(
            LocalDateTime inicio, LocalDateTime fin);

    List<PrestamoCaja> findByDevueltoTrueAndUsuarioInAndFechaDevolucionBetweenOrderByFechaDevolucionAsc(
            Collection<String> usuarios, LocalDateTime inicio, LocalDateTime fin);

    List<PrestamoCaja> findTop100ByDevueltoTrueAndFechaDevolucionGreaterThanEqualOrderByFechaDevolucionDesc(
            LocalDateTime desde);
}

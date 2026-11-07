package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.PrestamoCaja;
import mx.com.sale.repository.PrestamoCajaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoCajaService {

    final PrestamoCajaRepository repo;
    final AuditService audit;

    @Transactional
    public PrestamoCaja crear(String persona, double monto, String concepto, String usuario) {
        PrestamoCaja p = new PrestamoCaja();
        p.setPersona(persona.trim());
        p.setMonto(redondear(monto));
        p.setConcepto(concepto == null || concepto.isBlank() ? null : concepto.trim());
        p.setUsuario(usuario);
        p.setFecha(LocalDateTime.now());
        p.setDevuelto(false);
        PrestamoCaja saved = repo.save(p);
        audit.log(usuario, "PRESTAMO " + saved.getId() + " $" + saved.getMonto() + " " + saved.getPersona());
        return saved;
    }

    @Transactional
    public PrestamoCaja devolver(String id, String usuario) {
        PrestamoCaja p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
        if (p.isDevuelto())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este préstamo ya fue devuelto");
        p.setDevuelto(true);
        p.setFechaDevolucion(LocalDateTime.now());
        p.setUsuarioDevolucion(usuario);
        PrestamoCaja saved = repo.save(p);
        audit.log(usuario, "DEVOLUCION PRESTAMO " + saved.getId() + " $" + saved.getMonto());
        return saved;
    }

    @Transactional
    public void anular(String id, String usuario) {
        PrestamoCaja p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
        if (p.isDevuelto())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un préstamo devuelto no se puede anular");
        repo.delete(p);
        audit.log(usuario, "ANULACION PRESTAMO " + id + " $" + p.getMonto() + " " + p.getPersona());
    }

    public List<PrestamoCaja> pendientes(Collection<String> usuarios) {
        if (usuarios == null)
            return repo.findByDevueltoFalseOrderByFechaAsc();
        return repo.findByDevueltoFalseAndUsuarioInOrderByFechaAsc(usuarios);
    }

    public List<PrestamoCaja> devueltosRecientes() {
        LocalDateTime desde = LocalDate.now().minusDays(90).atStartOfDay();
        return repo.findTop100ByDevueltoTrueAndFechaDevolucionGreaterThanEqualOrderByFechaDevolucionDesc(desde);
    }

    /** Préstamos sacados de caja en el día. usuarios null = toda la caja. */
    public List<PrestamoCaja> prestamosDelDia(LocalDate dia, Collection<String> usuarios) {
        LocalDateTime ini = dia.atStartOfDay();
        LocalDateTime fin = finDia(dia);
        if (usuarios == null)
            return repo.findByFechaBetweenOrderByFechaAsc(ini, fin);
        return repo.findByUsuarioInAndFechaBetweenOrderByFechaAsc(usuarios, ini, fin);
    }

    /** Devoluciones que entraron a caja en el día, ligadas a quien retiró el préstamo. */
    public List<PrestamoCaja> devolucionesDelDia(LocalDate dia, Collection<String> usuarios) {
        LocalDateTime ini = dia.atStartOfDay();
        LocalDateTime fin = finDia(dia);
        if (usuarios == null)
            return repo.findByDevueltoTrueAndFechaDevolucionBetweenOrderByFechaDevolucionAsc(ini, fin);
        return repo.findByDevueltoTrueAndUsuarioInAndFechaDevolucionBetweenOrderByFechaDevolucionAsc(usuarios, ini, fin);
    }

    private static double redondear(double monto) {
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static LocalDateTime finDia(LocalDate d) {
        return d.atTime(23, 59, 59, 999_999_999);
    }
}

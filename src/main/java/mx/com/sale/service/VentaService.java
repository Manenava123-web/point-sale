package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentaService {

    final VentaRepository repo;
    final ProductoService  ps;
    final AuditService     audit;

    @Transactional
    public Venta crear(Venta v) {
        v.setFecha(LocalDateTime.now());

        double total = 0;
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (var item : v.getItems()) {
            cantidades.merge(item.getId(), 1, Integer::sum);
            total += item.getPrice();
        }
        cantidades.forEach(ps::descontar);

        v.setTotal(total);
        if (v.getMontoPagado() != null) {
            v.setCambio(Math.max(0, v.getMontoPagado() - total));
        }

        Venta saved = repo.save(v);
        audit.log(v.getUsuario(), "VENTA " + saved.getId());
        return saved;
    }

    @Transactional
    public Venta cancelar(String id, String usuario) {
        Venta v = repo.findById(id)
                .or(() -> repo.findByIdPrefix(id.toLowerCase()).stream().findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

        if (v.isCancelada())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta ya está cancelada");

        if (v.getItems() != null) {
            Map<String, Integer> cantidades = new LinkedHashMap<>();
            for (var item : v.getItems()) {
                cantidades.merge(item.getId(), 1, Integer::sum);
            }
            cantidades.forEach(ps::restaurar);
        }

        v.setCancelada(true);
        v.setFechaCancelacion(LocalDateTime.now());
        v.setCanceladaPor(usuario);

        Venta saved = repo.save(v);
        audit.log(usuario, "CANCELACION " + saved.getId());
        return saved;
    }

    public Page<Venta> listar(int page, int size, LocalDate desde, LocalDate hasta) {
        PageRequest pr = PageRequest.of(page, size);
        if (desde != null && hasta != null)
            return repo.findAllByFechaBetweenOrderByFechaDesc(
                    desde.atStartOfDay(), hasta.atTime(23, 59, 59, 999_999_999), pr);
        return repo.findAllByOrderByFechaDesc(pr);
    }

    public Venta get(String id) {
        return repo.findById(id)
                .or(() -> repo.findByIdPrefix(id.toLowerCase()).stream().findFirst())
                .orElse(null);
    }

    public List<Venta> all() {
        return repo.findAll();
    }
}

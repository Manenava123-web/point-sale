package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

    final VentaRepository repo;
    final ProductoService ps;
    final AuditService audit;

    @Transactional
    public Venta crear(Venta v) {
        v.setFecha(LocalDateTime.now());

        double total = 0;
        for (var item : v.getItems()) {
            ps.descontar(item.getId(), 1);
            total += item.getPrice();
        }

        v.setTotal(total);
        Venta saved = repo.save(v);
        audit.log(v.getUsuario(), "VENTA " + saved.getId());
        return saved;
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

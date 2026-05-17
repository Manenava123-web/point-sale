package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Producto;
import mx.com.sale.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    final ProductoRepository repo;

    public Producto crear(Producto p) {
        return repo.save(p);
    }

    public List<Producto> all() {
        return repo.findAll();
    }

    public Page<Producto> buscar(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        if (query == null || query.isBlank()) {
            return repo.findAll(pageable);
        }
        return repo.findByNameContainingIgnoreCase(query.trim(), pageable);
    }

    public Optional<Producto> getByCode(String code) {
        return repo.findByCodeIgnoreCase(code);
    }

    @Transactional
    public void descontar(String id, int cantidad) {
        repo.findById(id).ifPresent(p -> {
            p.setStock(Math.max(0, p.getStock() - cantidad));
            repo.save(p);
        });
    }

    @Transactional
    public void restaurar(String id, int cantidad) {
        repo.findById(id).ifPresent(p -> {
            p.setStock(p.getStock() + cantidad);
            repo.save(p);
        });
    }

    public List<Producto> bajoStock(int umbral) {
        return repo.findByStockLessThanEqualOrderByStockAsc(umbral);
    }

    @Transactional
    public Producto actualizarStock(String id, int nuevoStock) {
        Producto p = repo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(id));
        p.setStock(Math.max(0, nuevoStock));
        return repo.save(p);
    }
}

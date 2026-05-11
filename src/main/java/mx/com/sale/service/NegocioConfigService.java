package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.repository.NegocioConfigRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NegocioConfigService {

    final NegocioConfigRepository repo;

    private static final Long ID = 1L;

    public NegocioConfig get() {
        return repo.findById(ID)
                .orElseGet(() -> new NegocioConfig(ID, "Mi Negocio", "", ""));
    }

    public NegocioConfig actualizar(NegocioConfig config) {
        config.setId(ID);
        return repo.save(config);
    }

    public void initIfAbsent(String nombre, String direccion, String telefono) {
        if (!repo.existsById(ID)) {
            repo.save(new NegocioConfig(ID, nombre, direccion, telefono));
        }
    }
}

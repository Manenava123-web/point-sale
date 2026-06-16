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
                .orElseGet(() -> new NegocioConfig(ID, "Mi Negocio", "", "", null));
    }

    public NegocioConfig actualizar(NegocioConfig config) {
        config.setId(ID);
        if (config.getLogo() == null) config.setLogo(get().getLogo());
        return repo.save(config);
    }

    public NegocioConfig actualizarLogo(String logo) {
        NegocioConfig cfg = get();
        cfg.setLogo(logo);
        return repo.save(cfg);
    }

    public void initIfAbsent(String nombre, String direccion, String telefono) {
        if (!repo.existsById(ID)) {
            repo.save(new NegocioConfig(ID, nombre, direccion, telefono, null));
        }
    }
}

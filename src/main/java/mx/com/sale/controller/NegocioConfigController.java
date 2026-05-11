package mx.com.sale.controller;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.service.NegocioConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/negocio")
@RequiredArgsConstructor
public class NegocioConfigController {

    final NegocioConfigService s;

    @GetMapping
    public NegocioConfig get() {
        return s.get();
    }

    @RolesAllowed("ADMIN")
    @PutMapping
    public NegocioConfig actualizar(@RequestBody NegocioConfig config) {
        return s.actualizar(config);
    }
}

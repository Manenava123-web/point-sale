package mx.com.sale.controller;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.service.NegocioConfigService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;

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

    @RolesAllowed("ADMIN")
    @PostMapping("/logo")
    public NegocioConfig subirLogo(@RequestParam("file") MultipartFile file) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(file.getBytes());
        return s.actualizarLogo("data:" + file.getContentType() + ";base64," + b64);
    }

    @RolesAllowed("ADMIN")
    @DeleteMapping("/logo")
    public NegocioConfig eliminarLogo() {
        return s.actualizarLogo(null);
    }
}

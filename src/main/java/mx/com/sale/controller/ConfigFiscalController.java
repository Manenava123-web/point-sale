package mx.com.sale.controller;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.ConfigFiscal;
import mx.com.sale.service.ConfigFiscalService;
import mx.com.sale.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/config/fiscal")
@RolesAllowed("ADMIN")
@RequiredArgsConstructor
public class ConfigFiscalController {

    final ConfigFiscalService svc;
    final UsuarioService usuarios;

    @GetMapping
    public Map<String, Object> get() {
        return svc.vista();
    }

    @PutMapping
    public Map<String, Object> guardar(@RequestBody ConfigFiscal body, Authentication auth) {
        return svc.actualizar(body, nombre(auth));
    }

    @PostMapping("/cer")
    public Map<String, Object> certificado(@RequestParam("file") MultipartFile file, Authentication auth) throws IOException {
        return svc.guardarArchivo("cer", file.getBytes(), file.getOriginalFilename(), nombre(auth));
    }

    @PostMapping("/key")
    public Map<String, Object> llave(@RequestParam("file") MultipartFile file, Authentication auth) throws IOException {
        return svc.guardarArchivo("key", file.getBytes(), file.getOriginalFilename(), nombre(auth));
    }

    @DeleteMapping("/cer")
    public Map<String, Object> quitarCertificado(Authentication auth) {
        return svc.quitarArchivo("cer", nombre(auth));
    }

    @DeleteMapping("/key")
    public Map<String, Object> quitarLlave(Authentication auth) {
        return svc.quitarArchivo("key", nombre(auth));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "No se pudo completar la operación";
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", reason));
    }

    private String nombre(Authentication auth) {
        if (auth == null) return "-";
        return usuarios.resolveNombre(auth.getName());
    }
}

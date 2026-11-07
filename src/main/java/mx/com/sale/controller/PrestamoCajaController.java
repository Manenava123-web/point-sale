package mx.com.sale.controller;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.PrestamoCaja;
import mx.com.sale.service.PrestamoCajaService;
import mx.com.sale.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/prestamos")
@RequiredArgsConstructor
public class PrestamoCajaController {

    final PrestamoCajaService svc;
    final UsuarioService usuarios;

    @GetMapping
    public Map<String, Object> listar() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pendientes", svc.pendientes(null));
        body.put("devueltos", svc.devueltosRecientes());
        return body;
    }

    @PostMapping
    public PrestamoCaja crear(@RequestBody PrestamoCaja body, Authentication auth) {
        String persona = body.getPersona() == null ? "" : body.getPersona().trim();
        if (persona.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica quién retira el dinero");
        if (persona.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es demasiado largo");
        if (Double.isNaN(body.getMonto()) || Double.isInfinite(body.getMonto()) || body.getMonto() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        String concepto = body.getConcepto();
        if (concepto != null && concepto.trim().length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El concepto es demasiado largo");
        return svc.crear(persona, body.getMonto(), concepto, nombre(auth));
    }

    @PostMapping("/{id}/devolver")
    public PrestamoCaja devolver(@PathVariable String id, Authentication auth) {
        return svc.devolver(id, nombre(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable String id, Authentication auth) {
        svc.anular(id, nombre(auth));
        return ResponseEntity.noContent().build();
    }

    private String nombre(Authentication auth) {
        if (auth == null) return "-";
        return usuarios.resolveNombre(auth.getName());
    }
}

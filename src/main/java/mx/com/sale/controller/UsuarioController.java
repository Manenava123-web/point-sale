package mx.com.sale.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Usuario;
import mx.com.sale.service.UsuarioService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsuarioController {

    final UsuarioService us;

    @RolesAllowed("ADMIN")
    @PostMapping
    public Usuario crear(@RequestBody Usuario u) {
        Usuario saved = us.crear(u);
        saved.setPassword(null);
        return saved;
    }

    @RolesAllowed("ADMIN")
    @GetMapping
    public List<Usuario> all() {
        return us.all().stream()
                .peek(u -> u.setPassword(null))
                .toList();
    }

    @RolesAllowed("ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String nombre   = body.getOrDefault("nombre", "");
        String rol      = body.get("rol");
        if (username == null || username.isBlank() || rol == null)
            return ResponseEntity.badRequest().build();

        Usuario u = us.actualizar(id, username.trim(), nombre.trim(), rol);
        u.setPassword(null);
        return ResponseEntity.ok(u);
    }

    @RolesAllowed("ADMIN")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String pwd = body.get("password");
        if (pwd == null || pwd.isBlank())
            return ResponseEntity.badRequest().build();

        us.cambiarPassword(id, pwd);
        return ResponseEntity.ok().build();
    }

    @RolesAllowed("ADMIN")
    @PatchMapping("/{id}/activo")
    public ResponseEntity<Usuario> toggleActivo(@PathVariable String id) {
        Usuario u = us.toggleActivo(id);
        u.setPassword(null);
        return ResponseEntity.ok(u);
    }
}

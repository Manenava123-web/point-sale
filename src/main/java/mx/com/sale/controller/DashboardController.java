package mx.com.sale.controller;

import lombok.RequiredArgsConstructor;
import mx.com.sale.service.DashboardService;
import mx.com.sale.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    final DashboardService ds;
    final UsuarioService   us;

    @GetMapping("/resumen")
    public Map<String, Object> resumen(Authentication auth) {
        return ds.resumen(filtro(auth));
    }

    @GetMapping("/grafica-semana")
    public List<Map<String, Object>> graficaSemana(Authentication auth) {
        return ds.graficaSemana(filtro(auth));
    }

    @GetMapping("/top-productos")
    public List<Map<String, Object>> topProductos(Authentication auth) {
        return ds.topProductos(filtro(auth));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Set<String> filtro(Authentication auth) {
        if (isAdmin(auth)) return null;
        String username = auth.getName();
        String nombre   = us.resolveNombre(username);
        Set<String> keys = new java.util.HashSet<>();
        keys.add(username);
        keys.add(nombre);
        return keys;
    }
}

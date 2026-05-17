package mx.com.sale.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.com.sale.security.JwtUtil;
import mx.com.sale.service.UsuarioService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    @Value("${app.default.username}")
    private String defaultUser;

    @Value("${app.default.password}")
    private String defaultPass;

    @Value("${app.default.role}")
    private String defaultRole;

    @Value("${app.default.nombre:Administrador}")
    private String defaultNombre;

    final UsuarioService us;
    final JwtUtil jwt;

    @GetMapping("/me")
    public Map<String, String> me(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Map.of();
        String username = auth.getName();
        Map<String, String> res = new java.util.HashMap<>();
        res.put("user", username);
        var u = us.findByUsername(username);
        if (u.isPresent()) {
            String nombre = u.get().getNombre();
            res.put("nombre", (nombre != null && !nombre.isBlank()) ? nombre : username);
            res.put("role",   u.get().getRol());
        } else if (defaultUser.equals(username)) {
            res.put("nombre", defaultNombre);
            res.put("role",   defaultRole);
        } else {
            res.put("nombre", username);
            res.put("role",   defaultRole);
        }
        return res;
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(
            @RequestBody Map<String, String> body,
            org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String username = auth.getName();
        String password = body.getOrDefault("password", "");
        var u = us.login(username, password);
        if (u != null) return ResponseEntity.ok().build();
        if (defaultUser.equals(username) && defaultPass.equals(password))
            return ResponseEntity.ok().build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> m, HttpServletRequest request) {
        String username = m.get("username");
        String password = m.get("password");

        var u = us.login(username, password);
        String finalUsername;
        String finalRole;
        String finalNombre;

        if (u != null) {
            finalUsername = u.getUsername();
            finalRole     = u.getRol();
            finalNombre   = (u.getNombre() != null && !u.getNombre().isBlank())
                            ? u.getNombre() : u.getUsername();
        } else if (defaultUser.equals(username) && defaultPass.equals(password)) {
            finalUsername = defaultUser;
            finalRole     = defaultRole;
            finalNombre   = defaultNombre;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + finalRole));

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(finalUsername, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authToken);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Login exitoso");
        response.put("user",    finalUsername);
        response.put("nombre",  finalNombre);
        response.put("role",    finalRole);
        return response;
    }
}

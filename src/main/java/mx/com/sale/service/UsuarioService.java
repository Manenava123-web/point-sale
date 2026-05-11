package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Usuario;
import mx.com.sale.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    final UsuarioRepository repo;

    @Value("${app.default.username:admin}")
    private String defaultUsername;

    @Value("${app.default.nombre:Administrador}")
    private String defaultNombre;

    public Usuario crear(Usuario u) {
        u.setActivo(true);
        return repo.save(u);
    }

    public List<Usuario> all() {
        return repo.findAll();
    }

    public Usuario login(String username, String password) {
        return repo.findByUsernameAndPasswordAndActivoTrue(username, password)
                .orElse(null);
    }

    public java.util.Optional<Usuario> getById(String id) {
        return repo.findById(id);
    }

    public java.util.Optional<Usuario> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    @org.springframework.transaction.annotation.Transactional
    public void setNombreIfEmpty(String id, String nombre) {
        repo.findById(id).ifPresent(u -> {
            if (u.getNombre() == null || u.getNombre().isBlank()) {
                u.setNombre(nombre);
                repo.save(u);
            }
        });
    }

    /** Devuelve el nombre real del usuario; si no tiene nombre registrado retorna el username. */
    public String resolveNombre(String username) {
        if (username == null || username.isBlank()) return "-";
        if (username.equals(defaultUsername)) {
            return defaultNombre;
        }
        return repo.findByUsername(username)
                .map(u -> (u.getNombre() != null && !u.getNombre().isBlank()) ? u.getNombre() : u.getUsername())
                .orElse(username);
    }

    @org.springframework.transaction.annotation.Transactional
    public Usuario actualizar(String id, String username, String nombre, String rol) {
        Usuario u = repo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(id));
        u.setUsername(username);
        u.setNombre(nombre);
        u.setRol(rol);
        return repo.save(u);
    }

    @org.springframework.transaction.annotation.Transactional
    public void cambiarPassword(String id, String nuevaPassword) {
        repo.findById(id).ifPresent(u -> {
            u.setPassword(nuevaPassword);
            repo.save(u);
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public Usuario toggleActivo(String id) {
        Usuario u = repo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(id));
        u.setActivo(!u.isActivo());
        return repo.save(u);
    }
}

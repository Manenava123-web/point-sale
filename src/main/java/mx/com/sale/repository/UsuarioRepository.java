package mx.com.sale.repository;

import mx.com.sale.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByUsernameAndPasswordAndActivoTrue(String username, String password);
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}

package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.AuditEvent;
import mx.com.sale.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    final AuditRepository repo;

    public void log(String usuario, String accion) {
        repo.save(new AuditEvent(null, usuario, accion, LocalDateTime.now()));
    }
}

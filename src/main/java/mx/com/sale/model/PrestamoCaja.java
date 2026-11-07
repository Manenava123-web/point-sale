package mx.com.sale.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "prestamos_caja")
public class PrestamoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    double monto;

    @Column(nullable = false, length = 80)
    String persona;

    @Column(length = 200)
    String concepto;

    /** Quién registró el préstamo. El corte de ese usuario lo toma en cuenta. */
    String usuario;

    LocalDateTime fecha;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0", nullable = false)
    boolean devuelto;

    LocalDateTime fechaDevolucion;

    String usuarioDevolucion;
}

package mx.com.sale.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "negocio_config")
public class NegocioConfig {

    @Id
    Long id;

    String nombre;
    String direccion;
    String telefono;

    @Column(columnDefinition = "LONGTEXT")
    String logo;
}

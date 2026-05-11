package mx.com.sale.model;

import jakarta.persistence.*;
import lombok.*;
import mx.com.sale.config.ProductosConverter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    /* Guardamos los items como JSON para preservar precio histórico */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ProductosConverter.class)
    List<Producto> items;

    double total;

    LocalDateTime fecha;

    String usuario;
}

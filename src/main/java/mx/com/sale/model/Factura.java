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
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    long numero;

    @Column(unique = true, nullable = false)
    String ventaId;

    @Column(nullable = false, length = 120)
    String clienteNombre;

    @Column(length = 13)
    String clienteRfc;

    @Column(length = 5)
    String clienteCodigoPostal;

    @Column(length = 3)
    String clienteRegimen;

    @Column(length = 4)
    String usoCfdi;

    @Column(length = 2)
    String formaPago;

    @Column(length = 3)
    String metodoPago;

    @Column(length = 13)
    String emisorRfc;

    @Column(length = 180)
    String emisorRazon;

    @Column(length = 3)
    String emisorRegimen;

    @Column(length = 5)
    String emisorCp;

    @Column(length = 8)
    String claveProdServ;

    @Column(length = 3)
    String claveUnidad;

    @Column(length = 40)
    String numeroCertificado;

    double total;

    LocalDateTime fecha;

    String usuario;

    public String getFolio() {
        return String.format("F-%05d", numero);
    }

    public String getVentaFolio() {
        if (ventaId == null || ventaId.length() < 8) return ventaId;
        return ventaId.substring(0, 8).toUpperCase();
    }
}

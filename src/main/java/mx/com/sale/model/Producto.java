package mx.com.sale.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @JsonAlias("nombre")
    String name;

    @JsonAlias("precio")
    double price;

    @Column(unique = true)
    String code;

    int stock;

    @JsonAlias({"precioProveedor", "supplierPrice"})
    @Column(columnDefinition = "double not null default 0")
    double supplierPrice;

    @JsonAlias({"utilidadPorcentaje", "utilidad", "utilityPercent"})
    @Column(columnDefinition = "double not null default 0")
    double utilityPercent;

    /** Precio de venta = precio del proveedor × (1 + utilidad % / 100). */
    public static double precioVenta(double costo, double porcentaje) {
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(porcentaje)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        double precio = BigDecimal.valueOf(Math.max(0, costo))
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return Math.max(0, precio);
    }

    public static double costoDesdePrecio(double precioVenta, double porcentaje) {
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(porcentaje)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if (factor.compareTo(BigDecimal.ZERO) == 0) return 0;
        return BigDecimal.valueOf(Math.max(0, precioVenta))
                .divide(factor, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public void aplicarPrecioCalculado() {
        if (supplierPrice < 0) supplierPrice = 0;
        this.price = precioVenta(supplierPrice, utilityPercent);
    }

    public boolean tieneDesglose() {
        return supplierPrice > 0 || utilityPercent != 0;
    }

    public double utilidadImporte() {
        return BigDecimal.valueOf(price)
                .subtract(BigDecimal.valueOf(supplierPrice))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

package mx.com.sale.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_fiscal")
public class ConfigFiscal {

    @Id
    Long id;

    @Column(length = 13)
    String rfc;

    @Column(length = 180)
    String razonSocial;

    @Column(length = 3)
    String regimenFiscal;

    @Column(length = 5)
    String codigoPostal;

    @Column(length = 8)
    String claveProdServ;

    @Column(length = 3)
    String claveUnidad;

    @Column(length = 80)
    String pacNombre;

    @Column(length = 80)
    String pacUsuario;

    @Column(length = 20)
    String pacAmbiente;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 200)
    String pacPassword;

    @JsonIgnore
    @Column(columnDefinition = "LONGTEXT")
    String certificadoCer;

    @JsonIgnore
    @Column(columnDefinition = "LONGTEXT")
    String certificadoKey;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 200)
    String certificadoPassword;

    @Column(length = 120)
    String certificadoNombre;

    @Column(length = 120)
    String llaveNombre;

    @Column(length = 40)
    String numeroCertificado;
}

package mx.com.sale.model;

import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorteCaja {
 String id;
 LocalDate fecha;
 double total;
 int ventas;
}

package mx.com.sale.controller;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.CorteCaja;
import mx.com.sale.model.Venta;
import mx.com.sale.pdf.TicketPdfService;
import mx.com.sale.service.CajaService;
import mx.com.sale.service.ProductoService;
import mx.com.sale.service.UsuarioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class CajaController {

    final CajaService      cs;
    final ProductoService  ps;
    final TicketPdfService pdf;
    final UsuarioService   us;

    private static final DateTimeFormatter FFECHA = DateTimeFormatter.ofPattern("ddMMyyyy");

    @GetMapping("/daily")
    public ResponseEntity<byte[]> corte(Authentication auth) throws Exception {
        List<Venta> ventas = isAdmin(auth) ? cs.ventasHoy() : cs.ventasHoy(keysOf(auth));
        CorteCaja   corte  = cs.corteDeVentas(ventas);
        return pdfResponse(pdf.generarCorte(corte, ventas),
                "corte-" + LocalDate.now().format(FFECHA) + ".pdf");
    }

    @GetMapping("/weekly")
    public ResponseEntity<byte[]> semanal(Authentication auth) throws Exception {
        LocalDate   inicio = cs.inicioSemana();
        LocalDate   fin    = inicio.plusDays(6);
        List<Venta> ventas = isAdmin(auth) ? cs.ventasSemana() : cs.ventasSemana(keysOf(auth));
        return pdfResponse(pdf.generarReporteVentas("REPORTE SEMANAL DE VENTAS", ventas, inicio, fin),
                "ventas-semana-" + inicio.format(FFECHA) + ".pdf");
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> mensual(Authentication auth) throws Exception {
        LocalDate   inicio = cs.inicioMes();
        LocalDate   fin    = YearMonth.now().atEndOfMonth();
        List<Venta> ventas = isAdmin(auth) ? cs.ventasMes() : cs.ventasMes(keysOf(auth));
        return pdfResponse(pdf.generarReporteVentas("REPORTE MENSUAL DE VENTAS", ventas, inicio, fin),
                "ventas-mes-" + inicio.format(DateTimeFormatter.ofPattern("MMyyyy")) + ".pdf");
    }

    @GetMapping("/low-stock")
    public ResponseEntity<byte[]> bajoStock() throws Exception {
        byte[] bytes = pdf.generarBajoStock(ps.bajoStock(10));
        return pdfResponse(bytes, "bajo-stock-" + LocalDate.now().format(FFECHA) + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Username y nombre del usuario en sesión — cubre ventas antiguas (username) y nuevas (nombre). */
    private Set<String> keysOf(Authentication auth) {
        String username = auth.getName();
        String nombre   = us.resolveNombre(username);
        Set<String> keys = new java.util.HashSet<>();
        keys.add(username);
        keys.add(nombre);
        return keys;
    }
}

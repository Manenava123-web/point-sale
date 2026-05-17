package mx.com.sale.controller;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Venta;
import mx.com.sale.pdf.TicketPdfService;
import mx.com.sale.service.VentaService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class VentaController {

    final VentaService     vs;
    final TicketPdfService pdf;

    @PostMapping
    public Venta crear(@RequestBody Venta v) {
        return vs.crear(v);
    }

    @RolesAllowed("ADMIN")
    @GetMapping
    public Page<Venta> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false)    String desde,
            @RequestParam(required = false)    String hasta) {
        LocalDate d = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde) : null;
        LocalDate h = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta) : null;
        return vs.listar(page, size, d, h);
    }

    @RolesAllowed("ADMIN")
    @PostMapping("/{id}/cancelar")
    public Venta cancelar(@PathVariable String id, Authentication auth) {
        return vs.cancelar(id, auth.getName());
    }

    @GetMapping("/ticket/{id}")
    public ResponseEntity<byte[]> ticket(@PathVariable String id) throws Exception {
        Venta venta = vs.get(id);
        if (venta == null) return ResponseEntity.notFound().build();

        byte[] bytes  = pdf.generar(venta);
        String folio  = venta.getId().substring(0, 8).toUpperCase();
        String filename = "ticket-" + folio + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}

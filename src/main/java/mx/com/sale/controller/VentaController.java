package mx.com.sale.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Venta;
import mx.com.sale.pdf.TicketPdfService;
import mx.com.sale.service.VentaService;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class VentaController {

    final VentaService vs;
    final TicketPdfService pdf;

    @PostMapping
    public Venta crear(@RequestBody Venta v) {
        return vs.crear(v);
    }

    @GetMapping("/ticket/{id}")
    public ResponseEntity<byte[]> ticket(@PathVariable String id) throws Exception {
        Venta venta = vs.get(id);
        if (venta == null) return ResponseEntity.notFound().build();

        byte[] bytes    = pdf.generar(venta);
        String folio    = venta.getId().substring(0, 8).toUpperCase();
        String filename = "ticket-" + folio + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}

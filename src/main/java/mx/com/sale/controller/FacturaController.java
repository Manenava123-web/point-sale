package mx.com.sale.controller;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Factura;
import mx.com.sale.model.Venta;
import mx.com.sale.pdf.TicketPdfService;
import mx.com.sale.service.FacturaService;
import mx.com.sale.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    final FacturaService svc;
    final TicketPdfService pdf;
    final UsuarioService usuarios;

    @GetMapping
    public Page<Factura> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return svc.listar(page, size);
    }

    @GetMapping("/venta/{folio}")
    public Map<String, Object> porVenta(@PathVariable String folio) {
        Venta v = svc.venta(folio);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ventaId", v.getId());
        body.put("folioVenta", v.getId() == null || v.getId().length() < 8
                ? v.getId() : v.getId().substring(0, 8).toUpperCase());
        body.put("fecha", v.getFecha());
        body.put("total", v.getTotal());
        body.put("cancelada", v.isCancelada());
        body.put("cajero", v.getUsuario() == null ? "—" : usuarios.resolveNombre(v.getUsuario()));
        body.put("lineas", FacturaService.lineas(v));
        body.put("factura", svc.porVenta(v.getId()).orElse(null));
        return body;
    }

    @PostMapping
    public Factura crear(@RequestBody Factura body, Authentication auth) {
        String venta = body.getVentaId() == null ? "" : body.getVentaId().trim();
        String cliente = body.getClienteNombre() == null ? "" : body.getClienteNombre().trim();
        if (venta.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el folio de la venta");
        if (cliente.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el nombre del cliente");
        if (cliente.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del cliente es demasiado largo");
        String rfc = normalizarRfc(body.getClienteRfc());
        if (rfc == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RFC del cliente es obligatorio");
        String cp = body.getClienteCodigoPostal() == null ? "" : body.getClienteCodigoPostal().trim();
        if (!cp.isBlank() && !cp.matches("\\d{5}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código postal debe tener 5 dígitos");
        Factura datos = new Factura();
        datos.setVentaId(venta);
        datos.setClienteNombre(cliente);
        datos.setClienteRfc(rfc);
        datos.setClienteCodigoPostal(cp.isBlank() ? null : cp);
        datos.setClienteRegimen(codigo(body.getClienteRegimen()));
        datos.setUsoCfdi(codigo(body.getUsoCfdi()));
        datos.setFormaPago(codigo(body.getFormaPago()));
        datos.setMetodoPago(codigo(body.getMetodoPago()));
        String usuario = auth == null ? "-" : usuarios.resolveNombre(auth.getName());
        return svc.crear(datos, usuario);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String id) throws Exception {
        Factura f = svc.get(id);
        Venta v = svc.venta(f.getVentaId());
        byte[] bytes = pdf.generarFactura(f, v);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"factura-" + f.getFolio() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "No se pudo completar la operación";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", reason));
    }

    private String codigo(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private String normalizarRfc(String rfc) {
        if (rfc == null || rfc.isBlank()) return null;
        String limpio = rfc.trim().toUpperCase().replace(" ", "").replace("-", "");
        if (limpio.length() != 12 && limpio.length() != 13)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RFC debe tener 12 o 13 caracteres");
        return limpio;
    }
}

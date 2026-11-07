package mx.com.sale.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mx.com.sale.fiscal.CatalogoFiscal;
import mx.com.sale.model.ConfigFiscal;
import mx.com.sale.model.Factura;
import mx.com.sale.model.Producto;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.FacturaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacturaService {

    final FacturaRepository repo;
    final VentaService ventas;
    final ConfigFiscalService fiscal;
    final AuditService audit;

    public Page<Factura> listar(int page, int size) {
        return repo.findAllByOrderByFechaDesc(PageRequest.of(page, size));
    }

    public Factura get(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
    }

    public Optional<Factura> porVenta(String ventaId) {
        return repo.findByVentaId(ventaId);
    }

    public Venta venta(String folio) {
        Venta v = ventas.get(folio);
        if (v == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        return v;
    }

    @Transactional
    public synchronized Factura crear(Factura datos, String usuario) {
        Venta venta = venta(datos.getVentaId());
        if (venta.isCancelada())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede facturar una venta cancelada");

        Optional<Factura> previa = repo.findByVentaId(venta.getId());
        if (previa.isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta venta ya tiene la factura " + previa.get().getFolio());

        validarCatalogo(datos.getClienteRegimen(), CatalogoFiscal.REGIMEN, "El régimen fiscal del cliente no es válido");
        validarCatalogo(datos.getUsoCfdi(), CatalogoFiscal.USO, "El uso del CFDI no es válido");
        validarCatalogo(datos.getFormaPago(), CatalogoFiscal.FORMA, "La forma de pago no es válida");
        validarCatalogo(datos.getMetodoPago(), CatalogoFiscal.METODO, "El método de pago no es válido");

        ConfigFiscal emisor = fiscal.obtener();
        String cp = datos.getClienteCodigoPostal();
        if ((cp == null || cp.isBlank()) && "XAXX010101000".equals(datos.getClienteRfc()))
            cp = emisor.getCodigoPostal();
        if (cp == null || !cp.matches("\\d{5}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código postal del cliente debe tener 5 dígitos. Para público en general se usa el del negocio.");

        Factura f = new Factura();
        f.setNumero(repo.maxNumero() + 1);
        f.setVentaId(venta.getId());
        f.setClienteNombre(datos.getClienteNombre().trim());
        f.setClienteRfc(datos.getClienteRfc());
        f.setClienteCodigoPostal(cp);
        f.setClienteRegimen(datos.getClienteRegimen());
        f.setUsoCfdi(datos.getUsoCfdi());
        f.setFormaPago(datos.getFormaPago());
        f.setMetodoPago(datos.getMetodoPago());
        f.setEmisorRfc(emisor.getRfc());
        f.setEmisorRazon(emisor.getRazonSocial());
        f.setEmisorRegimen(emisor.getRegimenFiscal());
        f.setEmisorCp(emisor.getCodigoPostal());
        f.setClaveProdServ(emisor.getClaveProdServ() == null ? "01010101" : emisor.getClaveProdServ());
        f.setClaveUnidad(emisor.getClaveUnidad() == null ? "H87" : emisor.getClaveUnidad());
        f.setNumeroCertificado(emisor.getNumeroCertificado());
        f.setTotal(venta.getTotal());
        f.setFecha(LocalDateTime.now());
        f.setUsuario(usuario);
        Factura saved = repo.save(f);
        audit.log(usuario, "FACTURA " + saved.getFolio() + " venta " + saved.getVentaFolio());
        return saved;
    }

    private void validarCatalogo(String clave, Map<String, String> catalogo, String mensaje) {
        if (clave == null || !catalogo.containsKey(clave))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static List<Linea> lineas(Venta venta) {
        Map<String, Linea> map = new LinkedHashMap<>();
        if (venta == null || venta.getItems() == null) return List.of();
        for (Producto p : venta.getItems()) {
            String nombre = p.getName() == null || p.getName().isBlank() ? "Producto" : p.getName();
            String key = nombre + "|" + String.format("%.2f", p.getPrice());
            Linea linea = map.get(key);
            if (linea == null) {
                linea = new Linea(nombre, 0, p.getPrice(), 0);
                map.put(key, linea);
            }
            linea.cantidad++;
            linea.importe = BigDecimal.valueOf(linea.precio)
                    .multiply(BigDecimal.valueOf(linea.cantidad))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new ArrayList<>(map.values());
    }

    @Data
    @AllArgsConstructor
    public static class Linea {
        String nombre;
        int cantidad;
        double precio;
        double importe;
    }
}

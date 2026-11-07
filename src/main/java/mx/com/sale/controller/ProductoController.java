package mx.com.sale.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Producto;
import mx.com.sale.service.ProductoService;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductoController {

    final ProductoService s;

    @RolesAllowed("ADMIN")
    @PostMapping
    public Producto crear(@RequestBody Producto p) {
        if (p.getName() == null || p.getName().isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        validarCosto(p.getSupplierPrice(), p.getUtilityPercent());
        return s.crear(p);
    }

    @RolesAllowed("ADMIN")
    @PutMapping("/{id}/precio")
    public Producto actualizarPrecio(@PathVariable String id, @RequestBody Producto p) {
        validarCosto(p.getSupplierPrice(), p.getUtilityPercent());
        return s.actualizarPrecio(id, p.getSupplierPrice(), p.getUtilityPercent());
    }

    @GetMapping
    public Page<Producto> all(
            @RequestParam(defaultValue = "")  String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return s.buscar(search, page, size);
    }

    @RolesAllowed("ADMIN")
    @PatchMapping("/{id}/stock")
    public Producto actualizarStock(@PathVariable String id, @RequestBody java.util.Map<String, Integer> body) {
        Integer stock = body.get("stock");
        if (stock == null || stock < 0) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "El stock debe ser 0 o mayor");
        return s.actualizarStock(id, stock);
    }

    private void validarCosto(double supplier, double utility) {
        if (Double.isNaN(supplier) || Double.isInfinite(supplier) || supplier < 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El precio del proveedor no puede ser negativo");
        if (Double.isNaN(utility) || Double.isInfinite(utility) || utility < 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "La utilidad porcentual no puede ser negativa");
    }

    @GetMapping("/{code}")
    public ResponseEntity<Producto> porCodigo(@PathVariable String code) {
        return s.getByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

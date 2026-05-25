package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.ProductoRepository;
import mx.com.sale.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    final VentaRepository    ventaRepo;
    final ProductoRepository productoRepo;

    public Map<String, Object> resumen(Collection<String> filtro) {
        LocalDate hoy   = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Venta> ventasHoy    = fetch(hoy.atStartOfDay(),   finDia(hoy),   filtro);
        List<Venta> ventasSemana = fetch(lunes.atStartOfDay(), finDia(hoy),   filtro);

        double totalHoy    = ventasHoy.stream().mapToDouble(Venta::getTotal).sum();
        double totalSemana = ventasSemana.stream().mapToDouble(Venta::getTotal).sum();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalHoy",             totalHoy);
        r.put("transaccionesHoy",     ventasHoy.size());
        r.put("promedioHoy",          ventasHoy.isEmpty() ? 0 : totalHoy / ventasHoy.size());
        r.put("totalSemana",          totalSemana);
        r.put("transaccionesSemana",  ventasSemana.size());
        r.put("bajoStock",            productoRepo.findByStockLessThanEqualOrderByStockAsc(10).size());
        return r;
    }

    public List<Map<String, Object>> graficaSemana(Collection<String> filtro) {
        LocalDate hoy    = LocalDate.now();
        LocalDate inicio = hoy.minusDays(6);

        List<Venta> ventas = fetch(inicio.atStartOfDay(), finDia(hoy), filtro);

        Map<LocalDate, List<Venta>> porDia = ventas.stream()
                .filter(v -> v.getFecha() != null)
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia    = hoy.minusDays(i);
            List<Venta> dias = porDia.getOrDefault(dia, List.of());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", dia.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("es", "MX")) + " " + dia.getDayOfMonth());
            entry.put("total", dias.stream().mapToDouble(Venta::getTotal).sum());
            entry.put("count", dias.size());
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> topProductos(Collection<String> filtro) {
        LocalDate hoy    = LocalDate.now();
        LocalDate inicio = hoy.minusDays(29);

        List<Venta> ventas = fetch(inicio.atStartOfDay(), finDia(hoy), filtro);

        Map<String, long[]>  conteo  = new LinkedHashMap<>();
        Map<String, String>  nombres = new HashMap<>();

        for (Venta v : ventas) {
            if (v.getItems() == null) continue;
            v.getItems().forEach(p -> {
                String key = (p.getId() != null && !p.getId().isBlank()) ? p.getId() : p.getName();
                conteo.computeIfAbsent(key, k -> new long[]{0, 0});
                conteo.get(key)[0]++;
                conteo.get(key)[1] += Math.round(p.getPrice() * 100);
                nombres.putIfAbsent(key, p.getName());
            });
        }

        /* Resolver nombres actuales desde la tabla de productos */
        List<String> ids = conteo.keySet().stream()
                .filter(k -> k.contains("-"))
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            productoRepo.findAllById(ids).forEach(p -> nombres.put(p.getId(), p.getName()));
        }

        return conteo.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(7)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nombre",   nombres.getOrDefault(e.getKey(), e.getKey()));
                    m.put("cantidad", e.getValue()[0]);
                    m.put("total",    e.getValue()[1] / 100.0);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Venta> fetch(LocalDateTime desde, LocalDateTime hasta, Collection<String> filtro) {
        if (filtro == null || filtro.isEmpty())
            return ventaRepo.findByCanceladaFalseAndFechaBetween(desde, hasta);
        return ventaRepo.findByCanceladaFalseAndUsuarioInAndFechaBetween(filtro, desde, hasta);
    }

    private static LocalDateTime finDia(LocalDate d) {
        return d.atTime(23, 59, 59, 999_999_999);
    }
}

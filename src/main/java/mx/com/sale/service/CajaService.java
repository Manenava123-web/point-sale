package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.CorteCaja;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CajaService {

    final VentaRepository repo;

    public List<Venta> ventasHoy() {
        LocalDate hoy = LocalDate.now();
        return repo.findByFechaBetween(hoy.atStartOfDay(), hoy.atTime(23, 59, 59));
    }

    public List<Venta> ventasHoy(java.util.Collection<String> usuarios) {
        LocalDate hoy = LocalDate.now();
        return repo.findByUsuarioInAndFechaBetween(usuarios, hoy.atStartOfDay(), hoy.atTime(23, 59, 59));
    }

    public List<Venta> ventasSemana() {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repo.findByFechaBetween(lunes.atStartOfDay(), lunes.plusDays(6).atTime(23, 59, 59));
    }

    public List<Venta> ventasSemana(java.util.Collection<String> usuarios) {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repo.findByUsuarioInAndFechaBetween(usuarios, lunes.atStartOfDay(), lunes.plusDays(6).atTime(23, 59, 59));
    }

    public LocalDate inicioSemana() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public List<Venta> ventasMes() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = YearMonth.now().atEndOfMonth();
        return repo.findByFechaBetween(inicio.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    public List<Venta> ventasMes(java.util.Collection<String> usuarios) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = YearMonth.now().atEndOfMonth();
        return repo.findByUsuarioInAndFechaBetween(usuarios, inicio.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    public LocalDate inicioMes() {
        return LocalDate.now().withDayOfMonth(1);
    }

    public CorteCaja corteHoy() {
        return corteDeVentas(ventasHoy());
    }

    public CorteCaja corteDeVentas(List<Venta> ventas) {
        return new CorteCaja(
                UUID.randomUUID().toString(),
                LocalDate.now(),
                ventas.stream().mapToDouble(Venta::getTotal).sum(),
                ventas.size());
    }
}

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
        return repo.findByCanceladaFalseAndFechaBetween(hoy.atStartOfDay(), finDia(hoy));
    }

    public List<Venta> ventasHoy(java.util.Collection<String> usuarios) {
        LocalDate hoy = LocalDate.now();
        return repo.findByCanceladaFalseAndUsuarioInAndFechaBetween(usuarios, hoy.atStartOfDay(), finDia(hoy));
    }

    public List<Venta> ventasSemana() {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repo.findByCanceladaFalseAndFechaBetween(lunes.atStartOfDay(), finDia(lunes.plusDays(6)));
    }

    public List<Venta> ventasSemana(java.util.Collection<String> usuarios) {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repo.findByCanceladaFalseAndUsuarioInAndFechaBetween(usuarios, lunes.atStartOfDay(), finDia(lunes.plusDays(6)));
    }

    public LocalDate inicioSemana() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public List<Venta> ventasMes() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = YearMonth.now().atEndOfMonth();
        return repo.findByCanceladaFalseAndFechaBetween(inicio.atStartOfDay(), finDia(fin));
    }

    public List<Venta> ventasMes(java.util.Collection<String> usuarios) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = YearMonth.now().atEndOfMonth();
        return repo.findByCanceladaFalseAndUsuarioInAndFechaBetween(usuarios, inicio.atStartOfDay(), finDia(fin));
    }

    public LocalDate inicioMes() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private static LocalDateTime finDia(LocalDate d) {
        return d.atTime(23, 59, 59, 999_999_999);
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

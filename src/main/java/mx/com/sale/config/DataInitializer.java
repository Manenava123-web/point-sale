package mx.com.sale.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.model.Producto;
import mx.com.sale.model.Usuario;
import mx.com.sale.model.Venta;
import mx.com.sale.repository.ProductoRepository;
import mx.com.sale.service.NegocioConfigService;
import mx.com.sale.service.ProductoService;
import mx.com.sale.service.UsuarioService;
import mx.com.sale.service.VentaService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    final ProductoService      ps;
    final UsuarioService       us;
    final VentaService         vs;
    final ProductoRepository   productoRepo;
    final NegocioConfigService configSvc;

    private static final String[] NOMBRES_POOL = {
        "Ana Ramírez Cruz",      "Carlos Méndez Reyes",  "María Flores López",
        "José García Torres",    "Laura Sánchez Vega",   "Roberto Morales Díaz",
        "Sofía Hernández Lima",  "Miguel Ángel Ruiz",    "Carmen Ortega Jiménez",
        "Eduardo Vargas Mora",   "Patricia Luna Soto",   "Alejandro Ríos Peña"
    };

    @Override
    public void run(ApplicationArguments args) {
        try {
            configSvc.initIfAbsent(
                    "ABARROTES LA CASETA",
                    "Calle 9 oriente #1606, Barrio de San José",
                    "(756) 115-6436");

            patchNombresVacios();

            if (productoRepo.count() > 0) {
                log.info("Datos ya cargados ({} productos). Saltando inicialización.", productoRepo.count());
                return;
            }
            log.info("Iniciando carga de datos demo...");
            List<Producto> productos = seedProductos();
            seedUsuarios();
            seedVentas(productos);
            log.info("Datos demo cargados: {} productos, 2 cajeros, 8 ventas.", productos.size());
        } catch (Exception e) {
            log.warn("No se pudo inicializar datos demo: {}", e.getMessage());
        }
    }

    private List<Producto> seedProductos() {
        Object[][] data = {
            {"ARR001",  "Arroz El Toro 1kg",               28.50,  80},
            {"FRIJ001", "Frijol Negro 1kg",                32.00,  60},
            {"ACE001",  "Aceite Vegetal 1L",               45.00,  50},
            {"AZU001",  "Azúcar Refinada 1kg",             22.00,  70},
            {"LECH001", "Leche Lala Entera 1L",            24.50, 100},
            {"PAN001",  "Pan Bimbo Blanco Grande",         45.00,  40},
            {"COCA001", "Coca-Cola 2L",                    42.00,  60},
            {"PEPSI001","Pepsi 2L",                        38.00,  50},
            {"AGUA001", "Agua Ciel 1.5L",                  18.00,  80},
            {"JAB001",  "Jabón Roma 500g",                 15.00,  90},
            {"PAPE001", "Papel Higiénico Pétalo 4pz",      52.00,  45},
            {"DET001",  "Detergente Ariel 1kg",            78.00,  30},
            {"ATUN001", "Atún Dolores en Agua 140g",       22.00, 100},
            {"SARD001", "Sardinas La Sirena 425g",         18.00,  70},
            {"SOPA001", "Sopa Maruchan Res",               12.00, 150},
            {"SAL001",  "Salsa Valentina 370ml",           28.00,  55},
            {"CREM001", "Crema Lala 500g",                 45.00,  35},
            {"QUES001", "Queso Oaxaca 400g",               85.00,  25},
            {"TORT001", "Tortillas 1kg",                   22.00,  60},
            {"HUEV001", "Huevo Blanco 12pzas",             42.00,  80},
            {"CAFE001", "Café Nescafé Clásico 200g",       95.00,  30},
            {"GALL001", "Galletas Marías 400g",            32.00,  50},
            {"CHOC001", "Chocolate Abuelita 540g",         65.00,  40},
            {"MAYO001", "Mayonesa Hellmann's 390g",        48.00,  45},
            {"KET001",  "Catsup Heinz 397g",               38.00,  40},
        };

        return Arrays.stream(data)
                .map(row -> ps.crear(new Producto(
                        null,
                        (String) row[1],
                        ((Number) row[2]).doubleValue(),
                        (String) row[0],
                        (int) row[3])))
                .toList();
    }

    private void seedUsuarios() {
        us.crear(new Usuario(null, "cajera1", "Cajera Uno",  "cajera123", "CAJA", true));
        us.crear(new Usuario(null, "cajera2", "Cajera Dos",  "cajera123", "CAJA", true));
    }

    private void seedVentas(List<Producto> p) {
        Map<String, Producto> idx = new HashMap<>();
        p.forEach(pr -> idx.put(pr.getCode(), pr));

        crearVenta("cajera1", idx, "ARR001", "FRIJ001", "ACE001", "AZU001");
        crearVenta("cajera1", idx, "LECH001", "LECH001", "HUEV001", "CREM001");
        crearVenta("cajera2", idx, "COCA001", "PEPSI001", "PAN001");
        crearVenta("cajera2", idx, "SOPA001", "SOPA001", "SOPA001", "AGUA001", "SAL001");
        crearVenta("cajera1", idx, "QUES001", "CREM001", "TORT001", "TORT001");
        crearVenta("cajera2", idx, "DET001", "JAB001", "PAPE001");
        crearVenta("admin",   idx, "CAFE001", "GALL001", "CHOC001");
        crearVenta("cajera1", idx, "MAYO001", "KET001", "HUEV001", "SAL001", "ATUN001", "ATUN001");
    }

    private void patchNombresVacios() {
        var rnd = new java.util.Random();
        us.all().stream()
            .filter(u -> u.getNombre() == null || u.getNombre().isBlank())
            .forEach(u -> {
                String nombre = NOMBRES_POOL[rnd.nextInt(NOMBRES_POOL.length)];
                us.setNombreIfEmpty(u.getId(), nombre);
                log.info("Nombre asignado a usuario '{}': {}", u.getUsername(), nombre);
            });
    }

    private void crearVenta(String usuario, Map<String, Producto> idx, String... codigos) {
        try {
            List<Producto> items = Arrays.stream(codigos)
                    .map(idx::get)
                    .filter(pr -> pr != null)
                    .toList();
            if (items.isEmpty()) return;
            Venta v = new Venta();
            v.setUsuario(usuario);
            v.setItems(items);
            vs.crear(v);
        } catch (Exception e) {
            log.warn("Error al crear venta demo: {}", e.getMessage());
        }
    }
}

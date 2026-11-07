package mx.com.sale.fiscal;

import java.util.Map;

public final class CatalogoFiscal {

    public static final Map<String, String> REGIMEN = Map.ofEntries(
            Map.entry("601", "General de Ley Personas Morales"),
            Map.entry("603", "Personas Morales con Fines no Lucrativos"),
            Map.entry("605", "Sueldos y Salarios e Ingresos Asimilados a Salarios"),
            Map.entry("606", "Arrendamiento"),
            Map.entry("612", "Personas Físicas con Actividades Empresariales y Profesionales"),
            Map.entry("616", "Sin obligaciones fiscales"),
            Map.entry("621", "Incorporación Fiscal"),
            Map.entry("625", "Actividades Empresariales a través de Plataformas Tecnológicas"),
            Map.entry("626", "Régimen Simplificado de Confianza")
    );

    public static final Map<String, String> USO = Map.of(
            "G01", "Adquisición de mercancías",
            "G02", "Devoluciones, descuentos o bonificaciones",
            "G03", "Gastos en general",
            "S01", "Sin efectos fiscales",
            "CP01", "Pagos"
    );

    public static final Map<String, String> FORMA = Map.of(
            "01", "Efectivo",
            "02", "Cheque nominativo",
            "03", "Transferencia electrónica de fondos",
            "04", "Tarjeta de crédito",
            "28", "Tarjeta de débito",
            "99", "Por definir"
    );

    public static final Map<String, String> METODO = Map.of(
            "PUE", "Pago en una sola exhibición",
            "PPD", "Pago en parcialidades o diferido"
    );

    private CatalogoFiscal() {}

    public static String texto(Map<String, String> catalogo, String clave) {
        if (clave == null || clave.isBlank()) return "";
        String nombre = catalogo.get(clave);
        return nombre == null ? clave : clave + " " + nombre;
    }
}

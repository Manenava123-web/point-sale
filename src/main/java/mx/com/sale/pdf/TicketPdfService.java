package mx.com.sale.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import mx.com.sale.fiscal.CatalogoFiscal;
import mx.com.sale.model.CorteCaja;
import mx.com.sale.model.Factura;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.model.PrestamoCaja;
import mx.com.sale.model.Producto;
import mx.com.sale.model.Venta;
import mx.com.sale.model.ConfigFiscal;
import mx.com.sale.service.ConfigFiscalService;
import mx.com.sale.service.FacturaService;
import mx.com.sale.service.NegocioConfigService;
import mx.com.sale.service.ProductoService;
import mx.com.sale.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketPdfService {

    final NegocioConfigService configSvc;
    final UsuarioService        userSvc;
    final ProductoService       productoSvc;
    final ConfigFiscalService   fiscalSvc;

    /* BaseFont con CP1252 para renderizar correctamente caracteres acentuados */
    private static final BaseFont BF_COURIER;
    private static final BaseFont BF_HELVETICA;

    static {
        try {
            BF_COURIER   = BaseFont.createFont(BaseFont.COURIER,   BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BF_HELVETICA = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /* Ticket (térmica 80mm) – fuentes fijas, sin mutación */
    private static final Font TK_TITLE  = new Font(BF_COURIER, 13, Font.BOLD);
    private static final Font TK_NORMAL = new Font(BF_COURIER,  9);
    private static final Font TK_SMALL  = new Font(BF_COURIER,  8);
    private static final Font TK_TOTAL  = new Font(BF_COURIER, 11, Font.BOLD);

    /* Reportes A4 – factory methods: cada llamada produce una nueva instancia */
    private static Font fTitle()   { return new Font(BF_HELVETICA, 15, Font.BOLD); }
    private static Font fSection() { return new Font(BF_HELVETICA, 10, Font.BOLD); }
    private static Font fNormal()  { return new Font(BF_HELVETICA, 10); }
    private static Font fSmall()   { return new Font(BF_HELVETICA,  9); }
    private static Font fTotal()   { return new Font(BF_HELVETICA, 11, Font.BOLD); }
    private static Font fHeader()  { return new Font(BF_HELVETICA, 10, Font.BOLD, BaseColor.WHITE); }

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Ticket de venta ───────────────────────────────────────────────────────
    public byte[] generar(Venta v) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(new Rectangle(216f, 800f), 12, 12, 14, 14);
        PdfWriter.getInstance(doc, out);
        doc.open();

        NegocioConfig cfg = configSvc.get();
        if (ok(cfg.getLogo())) {
            try {
                Image img = Image.getInstance(decodeLogo(cfg.getLogo()));
                img.scaleToFit(120f, 60f);
                img.setAlignment(Element.ALIGN_CENTER);
                doc.add(img);
                doc.add(gap(3f));
            } catch (Exception ignored) {}
        }
        doc.add(pCenter(cfg.getNombre(), TK_TITLE));
        if (ok(cfg.getDireccion())) doc.add(pCenter(cfg.getDireccion(), TK_SMALL));
        if (ok(cfg.getTelefono()))  doc.add(pCenter("Tel. " + cfg.getTelefono(), TK_SMALL));
        doc.add(tkLine());
        doc.add(gap(2f));

        doc.add(new Paragraph("Folio : " + folio(v.getId()), TK_SMALL));
        if (v.getFecha()   != null) doc.add(new Paragraph("Fecha : " + v.getFecha().format(FMT), TK_SMALL));
        if (v.getUsuario() != null) doc.add(new Paragraph("Cajero: " + userSvc.resolveNombre(v.getUsuario()), TK_SMALL));
        doc.add(tkLine());

        for (Producto p : v.getItems())
            doc.add(new Paragraph(
                    String.format("%-20s %7.2f", cut(p.getName(), 20), p.getPrice()), TK_NORMAL));

        doc.add(tkLine());
        doc.add(new Paragraph(String.format("TOTAL:         $%8.2f", v.getTotal()), TK_TOTAL));
        doc.add(tkLine());
        doc.add(gap(4f));
        doc.add(pCenter("Gracias por su compra", TK_SMALL));
        doc.add(pCenter("Vuelva pronto", TK_SMALL));

        doc.close();
        return out.toByteArray();
    }

    // ── Factura de una venta ─────────────────────────────────────────────────
    public byte[] generarFactura(Factura f, Venta v) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = a4();
        PdfWriter.getInstance(doc, out);
        doc.open();

        String emitida = f.getFecha() != null ? f.getFecha().format(FMT) : "-";
        header(doc, "FACTURA " + f.getFolio(), "Emitida: " + emitida);

        ConfigFiscal cfg = fiscalSvc.obtener();
        String emisorRfc = primero(f.getEmisorRfc(), cfg.getRfc());
        String emisorRazon = primero(f.getEmisorRazon(), cfg.getRazonSocial());
        String emisorRegimen = primero(f.getEmisorRegimen(), cfg.getRegimenFiscal());
        String emisorCp = primero(f.getEmisorCp(), cfg.getCodigoPostal());
        String certificado = primero(f.getNumeroCertificado(), cfg.getNumeroCertificado());
        String clave = primero(f.getClaveProdServ(), cfg.getClaveProdServ());
        String unidad = primero(f.getClaveUnidad(), cfg.getClaveUnidad());
        if (clave.isBlank()) clave = "01010101";
        if (unidad.isBlank()) unidad = "H87";

        doc.add(new Paragraph("EMISOR", fSection()));
        doc.add(new Paragraph(emisorRazon.isBlank() ? configSvc.get().getNombre() : emisorRazon, fNormal()));
        doc.add(new Paragraph("RFC: " + (emisorRfc.isBlank() ? "Pendiente" : emisorRfc), fNormal()));
        doc.add(new Paragraph("Régimen: " + textoCatalogo(CatalogoFiscal.REGIMEN, emisorRegimen), fNormal()));
        doc.add(new Paragraph("Lugar de expedición: " + (emisorCp.isBlank() ? "Pendiente" : emisorCp), fNormal()));
        doc.add(new Paragraph("No. certificado: " + (certificado.isBlank() ? "Pendiente de cargar" : certificado), fSmall()));
        if (ok(cfg.getPacNombre()))
            doc.add(new Paragraph("PAC: " + cfg.getPacNombre(), fSmall()));

        doc.add(gap(4f));
        doc.add(new Paragraph("RECEPTOR", fSection()));
        doc.add(new Paragraph(ok(f.getClienteNombre()) ? f.getClienteNombre() : "-", fNormal()));
        doc.add(new Paragraph("RFC: " + (ok(f.getClienteRfc()) ? f.getClienteRfc() : "-"), fNormal()));
        doc.add(new Paragraph("Régimen: " + textoCatalogo(CatalogoFiscal.REGIMEN, f.getClienteRegimen()), fNormal()));
        doc.add(new Paragraph("Código postal: " + (ok(f.getClienteCodigoPostal()) ? f.getClienteCodigoPostal() : "-"), fNormal()));
        doc.add(new Paragraph("Uso CFDI: " + textoCatalogo(CatalogoFiscal.USO, f.getUsoCfdi()), fNormal()));
        doc.add(new Paragraph("Forma de pago: " + textoCatalogo(CatalogoFiscal.FORMA, f.getFormaPago()), fNormal()));
        doc.add(new Paragraph("Método de pago: " + textoCatalogo(CatalogoFiscal.METODO, f.getMetodoPago()), fNormal()));

        String ventaFolio = folio(v.getId());
        String ventaFecha = v.getFecha() != null ? v.getFecha().format(FMT) : "-";
        doc.add(new Paragraph("Venta: " + ventaFolio + "    Fecha de venta: " + ventaFecha, fSmall()));
        doc.add(new Paragraph("Clave de producto SAT: " + clave + "    Unidad: " + unidad, fSmall()));

        PdfPTable tabla = tabla(new float[]{0.8f, 3.4f, 1.3f, 1.4f});
        for (String h : new String[]{"Cant.", "Descripción", "Precio", "Importe"})
            tabla.addCell(th(h));
        for (FacturaService.Linea linea : FacturaService.lineas(v)) {
            td(tabla, String.valueOf(linea.getCantidad()));
            td(tabla, linea.getNombre());
            td(tabla, money(linea.getPrecio()));
            td(tabla, money(linea.getImporte()));
        }
        doc.add(tabla);

        PdfPTable res = resTable();
        row(res, "Total:", money(f.getTotal()));
        doc.add(res);
        Paragraph nota = new Paragraph(
                "Datos fiscales preparados para CFDI. El sello del SAT se obtiene con el PAC cuando el certificado queda cargado.",
                fSmall());
        nota.setSpacingBefore(8f);
        doc.add(nota);
        doc.add(footer());

        doc.close();
        return out.toByteArray();
    }

    // ── Corte diario ─────────────────────────────────────────────────────────
    public byte[] generarCorte(CorteCaja corte, List<Venta> ventas,
                                List<PrestamoCaja> prestamosHoy,
                                List<PrestamoCaja> devolucionesHoy,
                                List<PrestamoCaja> pendientes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = a4();
        PdfWriter.getInstance(doc, out);
        doc.open();

        header(doc, "CORTE DIARIO DE CAJA",
                "Fecha: " + corte.getFecha().format(FMT_DATE));

        PdfPTable tabla = tabla(new float[]{2f, 1.5f, 2f, 2f});
        for (String h : new String[]{"Folio", "Hora", "Cajero", "Total"}) tabla.addCell(th(h));
        for (Venta v : ventas) {
            td(tabla, folio(v.getId()));
            td(tabla, v.getFecha()   != null ? v.getFecha().format(FMT_HORA) : "-");
            td(tabla, userSvc.resolveNombre(v.getUsuario()));
            td(tabla, String.format("$%.2f", v.getTotal()));
        }
        doc.add(tabla);
        doc.add(hr());

        if (prestamosHoy == null) prestamosHoy = List.of();
        if (devolucionesHoy == null) devolucionesHoy = List.of();
        if (pendientes == null) pendientes = List.of();

        double prestado = prestamosHoy.stream().mapToDouble(PrestamoCaja::getMonto).sum();
        double devuelto = devolucionesHoy.stream().mapToDouble(PrestamoCaja::getMonto).sum();

        PdfPTable res = resTable();
        row(res, "Total ventas:",      String.valueOf(corte.getVentas()));
        row(res, "Importe ventas:",    money(corte.getTotal()));
        row(res, "Préstamos:",         money(prestado));
        row(res, "Dinero devuelto:",   money(devuelto));
        row(res, "Efectivo esperado:", money(corte.getTotal() - prestado + devuelto));
        doc.add(res);

        Paragraph nota = new Paragraph(
                "Efectivo esperado = ventas - préstamos + dinero devuelto.", fSmall());
        nota.setAlignment(Element.ALIGN_RIGHT);
        nota.setSpacingBefore(2f);
        doc.add(nota);

        agregarMovimientosCaja(doc, prestamosHoy, devolucionesHoy, pendientes);
        agregarDesglose(doc, ventas);
        doc.add(footer());

        doc.close();
        return out.toByteArray();
    }

    // ── Reporte semanal / mensual ─────────────────────────────────────────────
    public byte[] generarReporteVentas(String titulo, List<Venta> ventas,
                                        LocalDate inicio, LocalDate fin) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = a4();
        PdfWriter.getInstance(doc, out);
        doc.open();

        header(doc, titulo,
                "Período: " + inicio.format(FMT_DATE) + " al " + fin.format(FMT_DATE));

        PdfPTable tabla = tabla(new float[]{1.8f, 1.8f, 1.2f, 2f, 2f});
        for (String h : new String[]{"Folio", "Fecha", "Hora", "Cajero", "Total"}) tabla.addCell(th(h));
        for (Venta v : ventas) {
            td(tabla, folio(v.getId()));
            td(tabla, v.getFecha() != null ? v.getFecha().format(FMT_DATE) : "-");
            td(tabla, v.getFecha() != null ? v.getFecha().format(FMT_HORA) : "-");
            td(tabla, userSvc.resolveNombre(v.getUsuario()));
            td(tabla, String.format("$%.2f", v.getTotal()));
        }
        doc.add(tabla);
        doc.add(hr());

        double total = ventas.stream().mapToDouble(Venta::getTotal).sum();
        double prom  = ventas.isEmpty() ? 0 : total / ventas.size();

        PdfPTable res = resTable();
        row(res, "Total ventas:",   String.valueOf(ventas.size()));
        row(res, "Importe total:",  String.format("$%.2f", total));
        row(res, "Promedio/venta:", String.format("$%.2f", prom));
        doc.add(res);
        agregarDesglose(doc, ventas);
        doc.add(footer());

        doc.close();
        return out.toByteArray();
    }

    // ── Reporte bajo stock ───────────────────────────────────────────────────
    public byte[] generarBajoStock(List<Producto> productos) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = a4();
        PdfWriter.getInstance(doc, out);
        doc.open();

        header(doc, "ALERTA DE BAJO STOCK",
                "Productos con 10 o menos unidades  ·  " + LocalDate.now().format(FMT_DATE));

        PdfPTable tabla = tabla(new float[]{2.4f, 1.15f, 0.7f, 1.15f, 0.95f, 1.05f, 1.05f});
        for (String h : new String[]{"Nombre", "Código", "Stock", "P. proveedor", "Utilidad %", "Utilidad $", "Precio"})
            tabla.addCell(th(h));

        for (Producto p : productos) {
            td(tabla, p.getName());
            td(tabla, ok(p.getCode()) ? p.getCode() : "-");

            Font sf = p.getStock() <= 5
                    ? new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(200, 50, 50))
                    : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(160, 110, 0));
            PdfPCell sc = new PdfPCell(new Phrase(String.valueOf(p.getStock()), sf));
            sc.setPadding(5);
            sc.setHorizontalAlignment(Element.ALIGN_CENTER);
            sc.setBorderColor(new BaseColor(210, 210, 210));
            tabla.addCell(sc);

            if (p.tieneDesglose()) {
                td(tabla, money(p.getSupplierPrice()));
                td(tabla, pct(p.getUtilityPercent()));
                td(tabla, money(p.utilidadImporte()));
            } else {
                td(tabla, "-");
                td(tabla, "-");
                td(tabla, "-");
            }
            td(tabla, money(p.getPrice()));
        }
        doc.add(tabla);
        doc.add(footer());

        doc.close();
        return out.toByteArray();
    }

    // ── Préstamos y devoluciones del corte ───────────────────────────────────
    private void agregarMovimientosCaja(Document doc,
                                         List<PrestamoCaja> prestamosHoy,
                                         List<PrestamoCaja> devolucionesHoy,
                                         List<PrestamoCaja> pendientes) throws DocumentException {
        class Linea {
            LocalDateTime cuando;
            String persona;
            String concepto;
            String tipo;
            double monto;
        }
        List<Linea> lineas = new ArrayList<>();
        for (PrestamoCaja p : prestamosHoy) {
            Linea l = new Linea();
            l.cuando = p.getFecha();
            l.persona = p.getPersona();
            l.concepto = p.getConcepto();
            l.tipo = "Préstamo";
            l.monto = p.getMonto();
            lineas.add(l);
        }
        for (PrestamoCaja p : devolucionesHoy) {
            Linea l = new Linea();
            l.cuando = p.getFechaDevolucion();
            l.persona = p.getPersona();
            l.concepto = p.getConcepto();
            l.tipo = "Dinero devuelto";
            l.monto = p.getMonto();
            lineas.add(l);
        }
        lineas.sort(Comparator.comparing(l -> l.cuando, Comparator.nullsLast(Comparator.naturalOrder())));

        if (!lineas.isEmpty()) {
            doc.add(hr());
            Paragraph titulo = new Paragraph("PRÉSTAMOS Y DEVOLUCIONES DEL DÍA", fSection());
            titulo.setSpacingBefore(4f);
            titulo.setSpacingAfter(2f);
            doc.add(titulo);

            PdfPTable tabla = tabla(new float[]{1.1f, 2.1f, 2.6f, 1.7f, 1.2f});
            for (String h : new String[]{"Hora", "Persona", "Concepto", "Movimiento", "Monto"})
                tabla.addCell(th(h));
            for (Linea l : lineas) {
                td(tabla, l.cuando != null ? l.cuando.format(FMT_HORA) : "-");
                td(tabla, ok(l.persona) ? l.persona : "-");
                td(tabla, ok(l.concepto) ? l.concepto : "-");
                td(tabla, l.tipo);
                td(tabla, money(l.monto));
            }
            doc.add(tabla);
        }

        if (pendientes == null || pendientes.isEmpty()) return;

        doc.add(hr());
        Paragraph pend = new Paragraph("PENDIENTE POR DEVOLVER", fSection());
        pend.setSpacingBefore(4f);
        pend.setSpacingAfter(2f);
        doc.add(pend);

        PdfPTable tabla = tabla(new float[]{1.7f, 2.2f, 2.8f, 1.3f});
        for (String h : new String[]{"Fecha", "Persona", "Concepto", "Monto"})
            tabla.addCell(th(h));
        double totalPendiente = 0;
        for (PrestamoCaja p : pendientes) {
            td(tabla, p.getFecha() != null ? p.getFecha().format(FMT_DATE) : "-");
            td(tabla, ok(p.getPersona()) ? p.getPersona() : "-");
            td(tabla, ok(p.getConcepto()) ? p.getConcepto() : "-");
            td(tabla, money(p.getMonto()));
            totalPendiente += p.getMonto();
        }
        doc.add(tabla);

        PdfPTable res = resTable();
        row(res, "Por devolver:", money(totalPendiente));
        doc.add(res);
    }

    // ── Desglose de costo y utilidad ─────────────────────────────────────────
    private void agregarDesglose(Document doc, List<Venta> ventas) throws DocumentException {
        if (ventas == null || ventas.isEmpty()) return;

        Map<String, Producto> catalogo = new HashMap<>();
        for (Producto p : productoSvc.all()) {
            if (p.getId() != null) catalogo.put(p.getId(), p);
        }

        Map<String, Agg> lineas = new LinkedHashMap<>();
        boolean incompleto = false;

        for (Venta v : ventas) {
            if (v.getItems() == null) continue;
            for (Producto item : v.getItems()) {
                double supplier = item.getSupplierPrice();
                double utilidad = item.getUtilityPercent();
                boolean tiene = item.tieneDesglose();
                if (!tiene && item.getId() != null) {
                    Producto cat = catalogo.get(item.getId());
                    if (cat != null && cat.tieneDesglose()) {
                        supplier = cat.getSupplierPrice();
                        utilidad = cat.getUtilityPercent();
                        tiene = true;
                    }
                }
                if (!tiene) incompleto = true;

                String nombre = item.getName() != null ? item.getName() : "-";
                String key = (item.getId() != null ? item.getId() : nombre)
                        + String.format("|%.2f|%.2f|%.2f", item.getPrice(), supplier, utilidad);
                Agg agg = lineas.get(key);
                if (agg == null) {
                    agg = new Agg(nombre, supplier, utilidad, item.getPrice(), tiene);
                    lineas.put(key, agg);
                }
                agg.cantidad++;
            }
        }
        if (lineas.isEmpty()) return;

        doc.add(hr());
        Paragraph titulo = new Paragraph("DESGLOSE DE PRECIO Y UTILIDAD", fSection());
        titulo.setSpacingBefore(4f);
        titulo.setSpacingAfter(2f);
        doc.add(titulo);

        PdfPTable tabla = tabla(new float[]{2.3f, 0.7f, 1.2f, 1.0f, 1.05f, 1.15f, 1.15f});
        for (String h : new String[]{"Producto", "Cant.", "P. proveedor", "Utilidad %", "Precio", "Utilidad $", "Importe"})
            tabla.addCell(th(h));

        double costoTotal = 0;
        double utilidadTotal = 0;
        for (Agg a : lineas.values()) {
            td(tabla, a.nombre);
            td(tabla, String.valueOf(a.cantidad));
            double importe = a.price * a.cantidad;
            if (a.tiene) {
                double utilLinea = (a.price - a.supplier) * a.cantidad;
                td(tabla, money(a.supplier));
                td(tabla, pct(a.pct));
                td(tabla, money(a.price));
                td(tabla, money(utilLinea));
                td(tabla, money(importe));
                costoTotal += a.supplier * a.cantidad;
                utilidadTotal += utilLinea;
            } else {
                td(tabla, "-");
                td(tabla, "-");
                td(tabla, money(a.price));
                td(tabla, "-");
                td(tabla, money(importe));
            }
        }
        doc.add(tabla);

        PdfPTable res = resTable();
        row(res, "Costo proveedor:", money(costoTotal));
        row(res, "Utilidad:", money(utilidadTotal));
        doc.add(res);

        if (incompleto) {
            Paragraph nota = new Paragraph(
                    "Los productos sin precio de proveedor no entran al costo ni a la utilidad.",
                    fSmall());
            nota.setSpacingBefore(2f);
            doc.add(nota);
        }
    }

    private String money(double v) {
        return String.format("$%.2f", v);
    }

    private String pct(double v) {
        return String.format("%.2f%%", v);
    }

    private static final class Agg {
        final String nombre;
        int cantidad;
        final double supplier;
        final double pct;
        final double price;
        final boolean tiene;

        Agg(String nombre, double supplier, double pct, double price, boolean tiene) {
            this.nombre = nombre;
            this.supplier = supplier;
            this.pct = pct;
            this.price = price;
            this.tiene = tiene;
        }
    }

    // ── Helpers de layout ────────────────────────────────────────────────────

    private void header(Document doc, String titulo, String sub) throws DocumentException {
        NegocioConfig cfg = configSvc.get();

        if (ok(cfg.getLogo())) {
            try {
                Image img = Image.getInstance(decodeLogo(cfg.getLogo()));
                img.scaleToFit(110f, 55f);
                img.setAlignment(Element.ALIGN_CENTER);
                doc.add(img);
                doc.add(new Paragraph(" ") {{ setSpacingAfter(3f); }});
            } catch (Exception ignored) {}
        }

        Paragraph nom = new Paragraph(cfg.getNombre(), fTitle());
        nom.setAlignment(Element.ALIGN_CENTER);
        nom.setSpacingAfter(2f);
        doc.add(nom);

        String info = ok(cfg.getDireccion()) ? cfg.getDireccion() : "";
        if (ok(cfg.getTelefono())) info += (info.isEmpty() ? "" : "  ·  Tel. ") + cfg.getTelefono();
        if (!info.isEmpty()) {
            Paragraph pi = new Paragraph(info, fSmall());
            pi.setAlignment(Element.ALIGN_CENTER);
            pi.setSpacingAfter(4f);
            doc.add(pi);
        }

        doc.add(hr());

        Paragraph pt = new Paragraph(titulo, fSection());
        pt.setAlignment(Element.ALIGN_CENTER);
        pt.setSpacingBefore(6f);
        pt.setSpacingAfter(3f);
        doc.add(pt);

        Paragraph ps = new Paragraph(sub, fNormal());
        ps.setAlignment(Element.ALIGN_CENTER);
        ps.setSpacingAfter(6f);
        doc.add(ps);

        doc.add(hr());
    }

    private Document a4() {
        return new Document(PageSize.A4, 45, 45, 50, 50);
    }

    private PdfPTable tabla(float[] widths) throws DocumentException {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8f);
        return t;
    }

    private PdfPCell th(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fHeader()));
        c.setBackgroundColor(new BaseColor(40, 44, 52));
        c.setPadding(6f);
        c.setBorderColor(new BaseColor(60, 64, 72));
        return c;
    }

    private void td(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", fNormal()));
        c.setPadding(5f);
        c.setBorderColor(new BaseColor(210, 210, 210));
        t.addCell(c);
    }

    private PdfPTable resTable() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{3f, 2f});
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(8f);
        return t;
    }

    private void row(PdfPTable t, String label, String valor) {
        PdfPCell l = new PdfPCell(new Phrase(label, fSection()));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(4f);
        t.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(valor, fTotal()));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(4f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(v);
    }

    private Paragraph hr() {
        Paragraph p = new Paragraph(new Chunk(
                new LineSeparator(0.5f, 100f, new BaseColor(180, 180, 180), Element.ALIGN_CENTER, -2f)));
        p.setSpacingBefore(4f);
        p.setSpacingAfter(4f);
        return p;
    }

    private Paragraph tkLine() {
        return new Paragraph("--------------------------------", TK_SMALL);
    }

    private Paragraph footer() {
        Paragraph p = new Paragraph("Generado el " + LocalDateTime.now().format(FMT), fSmall());
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingBefore(10f);
        return p;
    }

    private Paragraph pCenter(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private Paragraph gap(float pts) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(pts);
        return p;
    }

    private String folio(String id) {
        if (id == null || id.length() < 8) return id != null ? id.toUpperCase() : "-";
        return id.substring(0, 8).toUpperCase();
    }

    private String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private boolean ok(String s) {
        return s != null && !s.isBlank();
    }

    private String primero(String preferido, String alterno) {
        if (ok(preferido)) return preferido;
        return alterno == null ? "" : alterno;
    }

    private String textoCatalogo(java.util.Map<String, String> catalogo, String clave) {
        String texto = CatalogoFiscal.texto(catalogo, clave);
        return texto.isBlank() ? "-" : texto;
    }

    private byte[] decodeLogo(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        String b64 = comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl;
        return java.util.Base64.getDecoder().decode(b64);
    }
}

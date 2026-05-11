package mx.com.sale.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import mx.com.sale.model.CorteCaja;
import mx.com.sale.model.NegocioConfig;
import mx.com.sale.model.Producto;
import mx.com.sale.model.Venta;
import mx.com.sale.service.NegocioConfigService;
import mx.com.sale.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPdfService {

    final NegocioConfigService configSvc;
    final UsuarioService        userSvc;

    /* Ticket (térmica 80mm) – fuentes fijas, sin mutación */
    private static final Font TK_TITLE  = new Font(Font.FontFamily.COURIER, 13, Font.BOLD);
    private static final Font TK_NORMAL = new Font(Font.FontFamily.COURIER,  9);
    private static final Font TK_SMALL  = new Font(Font.FontFamily.COURIER,  8);
    private static final Font TK_TOTAL  = new Font(Font.FontFamily.COURIER, 11, Font.BOLD);

    /* Reportes A4 – factory methods: cada llamada produce una nueva instancia */
    private static Font fTitle()   { return new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD); }
    private static Font fSection() { return new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD); }
    private static Font fNormal()  { return new Font(Font.FontFamily.HELVETICA, 10); }
    private static Font fSmall()   { return new Font(Font.FontFamily.HELVETICA,  9); }
    private static Font fTotal()   { return new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD); }
    private static Font fHeader()  { return new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE); }

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

    // ── Corte diario ─────────────────────────────────────────────────────────
    public byte[] generarCorte(CorteCaja corte, List<Venta> ventas) throws Exception {
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

        PdfPTable res = resTable();
        row(res, "Total ventas:",  String.valueOf(corte.getVentas()));
        row(res, "Importe total:", String.format("$%.2f", corte.getTotal()));
        doc.add(res);
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

        PdfPTable tabla = tabla(new float[]{3.5f, 1.5f, 1f, 1.8f});
        for (String h : new String[]{"Nombre", "Código", "Stock", "Precio"}) tabla.addCell(th(h));

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

            td(tabla, String.format("$%.2f", p.getPrice()));
        }
        doc.add(tabla);
        doc.add(footer());

        doc.close();
        return out.toByteArray();
    }

    // ── Helpers de layout ────────────────────────────────────────────────────

    private void header(Document doc, String titulo, String sub) throws DocumentException {
        NegocioConfig cfg = configSvc.get();

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
}

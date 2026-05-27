let ventasPage    = 0;
const VENTAS_SIZE = 25;
let ventasTotalPages = 0;

const FMT_MONEY = v => "$" + Number(v).toLocaleString("es-MX",
    { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const FMT_DATE = iso => {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("es-MX", { day:"2-digit", month:"2-digit", year:"numeric" })
        + " " + d.toLocaleTimeString("es-MX", { hour:"2-digit", minute:"2-digit" });
};

function limpiarFiltrosVentas() {
    document.getElementById("ventas-desde").value = "";
    document.getElementById("ventas-hasta").value = "";
    loadVentas(0);
}

function loadVentas(page) {
    if (page !== undefined) ventasPage = page;
    document.getElementById("ventas-loading").style.display = "block";
    document.getElementById("ventas-table-wrap").style.display = "none";
    document.getElementById("ventas-empty").style.display = "none";

    const desde = document.getElementById("ventas-desde")?.value || "";
    const hasta = document.getElementById("ventas-hasta")?.value || "";
    let url = "/sales?page=" + ventasPage + "&size=" + VENTAS_SIZE;
    if (desde) url += "&desde=" + encodeURIComponent(desde);
    if (hasta) url += "&hasta=" + encodeURIComponent(hasta);

    apiGet(url, function(data) {
        document.getElementById("ventas-loading").style.display = "none";
        const list = data.content || [];
        ventasTotalPages = data.totalPages || 0;

        if (list.length === 0) {
            document.getElementById("ventas-empty").style.display = "block";
            document.getElementById("ventas-count").textContent = "";
            return;
        }

        const total = data.totalElements || list.length;
        document.getElementById("ventas-count").textContent = total + " venta" + (total !== 1 ? "s" : "");

        const tbody = document.getElementById("ventas-list");
        tbody.innerHTML = list.map(v => {
            const folio   = v.id ? v.id.substring(0, 8).toUpperCase() : "—";
            const items   = (v.items || []).length;
            const cancelBadge = v.cancelada
                ? '<span class="badge" style="background:rgba(239,68,68,.15);color:var(--danger);font-size:.7rem;">Cancelada</span>'
                : '<span class="badge" style="background:rgba(34,197,94,.15);color:var(--accent);font-size:.7rem;">Activa</span>';
            const cancelBtn = !v.cancelada
                ? `<button class="btn btn-outline-danger btn-sm" style="font-size:.72rem;padding:.15rem .45rem;"
                           onclick="solicitarCancelacion('${v.id}','${folio}')">
                     <i class="bi bi-x-circle"></i> cancelar
                   </button>`
                : `<span style="font-size:.72rem;color:var(--text-subtle);">—</span>`;

            return `<tr style="${v.cancelada ? 'opacity:.5;' : ''}">
              <td><code style="font-size:.75rem;">${folio}</code></td>
              <td style="font-size:.8rem;">${FMT_DATE(v.fecha)}</td>
              <td style="font-size:.82rem;">${v.usuario || "—"}</td>
              <td class="text-center" style="font-size:.82rem;">${items}</td>
              <td class="text-end" style="font-weight:600;">${FMT_MONEY(v.total)}</td>
              <td class="text-end" style="font-size:.82rem;">${v.montoPagado != null ? FMT_MONEY(v.montoPagado) : "—"}</td>
              <td class="text-end" style="font-size:.82rem;">${v.cambio != null ? FMT_MONEY(v.cambio) : "—"}</td>
              <td class="text-center">${cancelBadge}</td>
              <td class="text-center">${cancelBtn}</td>
            </tr>`;
        }).join("");

        const desde = ventasPage * VENTAS_SIZE + 1;
        const hasta = Math.min((ventasPage + 1) * VENTAS_SIZE, total);
        document.getElementById("ventas-pag-info").textContent = desde + "–" + hasta + " de " + total;
        document.getElementById("ventas-prev").disabled = ventasPage === 0;
        document.getElementById("ventas-next").disabled = ventasPage >= ventasTotalPages - 1;
        document.getElementById("ventas-table-wrap").style.display = "block";
    });
}

function ventasPaginar(dir) {
    const next = ventasPage + dir;
    if (next < 0 || next >= ventasTotalPages) return;
    loadVentas(next);
}

function solicitarCancelacion(id, folio) {
    pedirPassword(
        "Ingresa tu contraseña para cancelar la venta " + folio + ". Se repondrá el stock de los productos.",
        function() { ejecutarCancelacion(id); }
    );
}

function ejecutarCancelacion(id) {
    apiPost("/sales/" + id + "/cancelar", {}, function(v) {
        if (window.showToast) showToast("Venta cancelada — stock repuesto");
        loadVentas();
    });
}

/* Init: llamado desde loadView en index.html */

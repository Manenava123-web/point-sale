let facturasPage = 0;
let facturasTotalPages = 0;
let facturaVenta = null;

function loadFacturas(page) {
    mostrarDatosFiscales();
    if (page !== undefined) facturasPage = page;
    const loading = document.getElementById("facturas-loading");
    const table = document.getElementById("facturas-table-wrap");
    const empty = document.getElementById("facturas-empty");
    const pag = document.getElementById("facturas-pagination");
    if (!loading) return;
    loading.style.display = "block";
    table.style.display = "none";
    empty.style.display = "none";
    pag.style.display = "none";

    apiGet("/facturas?page=" + facturasPage + "&size=8", function(data) {
        const list = data.content || [];
        facturasTotalPages = data.totalPages || 0;
        const total = data.totalElements || list.length;
        loading.style.display = "none";
        document.getElementById("facturas-count").textContent =
            total + " factura" + (total === 1 ? "" : "s");

        const tbody = document.getElementById("facturas-list");
        tbody.innerHTML = "";
        list.forEach(f => tbody.appendChild(filaFactura(f)));

        table.style.display = list.length ? "block" : "none";
        empty.style.display = list.length ? "none" : "block";
        if (facturasTotalPages > 1) {
            pag.style.display = "block";
            const size = data.size || 8;
            const desde = facturasPage * size + 1;
            const hasta = Math.min((facturasPage + 1) * size, total);
            document.getElementById("facturas-pag-info").textContent = desde + "–" + hasta + " de " + total;
            document.getElementById("facturas-prev").disabled = facturasPage === 0;
            document.getElementById("facturas-next").disabled = facturasPage >= facturasTotalPages - 1;
        }
    });
}

function facturasPaginar(dir) {
    const next = facturasPage + dir;
    if (next < 0 || next >= facturasTotalPages) return;
    loadFacturas(next);
}

function buscarVentaFactura() {
    const folio = document.getElementById("factura-folio").value.trim();
    if (!folio) {
        if (window.showToast) showToast("Escribe el folio de la venta", "error");
        return;
    }
    apiGet("/facturas/venta/" + encodeURIComponent(folio), mostrarVentaFactura, function(msg, xhr) {
        facturaVenta = null;
        document.getElementById("factura-preview").style.display = "none";
        document.getElementById("factura-guardar").disabled = true;
        if (window.showToast) showToast(detalleError(msg, xhr), "error");
    });
}

function mostrarVentaFactura(data) {
    facturaVenta = data;
    const preview = document.getElementById("factura-preview");
    const aviso = document.getElementById("factura-preview-aviso");
    const btn = document.getElementById("factura-guardar");
    preview.style.display = "block";

    document.getElementById("factura-preview-meta").textContent =
        "Venta " + (data.folioVenta || "") + " · " + fmtFechaFactura(data.fecha) + " · " + (data.cajero || "");

    const lineas = document.getElementById("factura-preview-lineas");
    lineas.innerHTML = "";
    (data.lineas || []).forEach(l => {
        const div = document.createElement("div");
        div.style.cssText = "display:flex;justify-content:space-between;gap:.5rem;margin-bottom:.15rem;";
        const nombre = document.createElement("span");
        nombre.textContent = l.cantidad + " × " + (l.nombre || "");
        const importe = document.createElement("span");
        importe.textContent = moneyFactura(l.importe);
        div.appendChild(nombre);
        div.appendChild(importe);
        lineas.appendChild(div);
    });
    document.getElementById("factura-preview-total").textContent = "Total " + moneyFactura(data.total);

    aviso.style.display = "none";
    aviso.textContent = "";
    if (data.cancelada) {
        aviso.style.display = "block";
        aviso.style.color = "var(--danger)";
        aviso.textContent = "Esta venta está cancelada.";
        btn.disabled = true;
    } else if (data.factura) {
        aviso.style.display = "block";
        aviso.style.color = "var(--warning)";
        aviso.textContent = "Ya tiene la factura " + data.factura.folio + ".";
        const reimprimir = document.createElement("button");
        reimprimir.className = "btn btn-outline-secondary btn-sm";
        reimprimir.style.marginLeft = ".4rem";
        reimprimir.textContent = "Reimprimir";
        reimprimir.onclick = () => abrirFactura(data.factura.id);
        aviso.appendChild(reimprimir);
        document.getElementById("factura-cliente").value = data.factura.clienteNombre || "";
        document.getElementById("factura-rfc").value = data.factura.clienteRfc || "";
        document.getElementById("factura-cp").value = data.factura.clienteCodigoPostal || "";
        document.getElementById("factura-regimen").value = data.factura.clienteRegimen || "";
        document.getElementById("factura-uso").value = data.factura.usoCfdi || "G03";
        document.getElementById("factura-forma").value = data.factura.formaPago || "01";
        document.getElementById("factura-metodo").value = data.factura.metodoPago || "PUE";
        btn.disabled = true;
    } else {
        btn.disabled = false;
    }
}

function guardarFactura() {
    if (!facturaVenta || facturaVenta.cancelada || facturaVenta.factura) return;
    const cliente = document.getElementById("factura-cliente").value.trim();
    const rfc = document.getElementById("factura-rfc").value.trim();
    const cp = document.getElementById("factura-cp").value.trim();
    const regimen = document.getElementById("factura-regimen").value;
    if (!cliente || !rfc || !regimen) {
        if (window.showToast) showToast("Indica cliente, RFC y régimen fiscal", "error");
        return;
    }
    confirmar(
        "Generar factura",
        "¿Generar factura de la venta " + facturaVenta.folioVenta + " por "
            + moneyFactura(facturaVenta.total) + " a nombre de " + cliente + "?",
        function() {
            apiPost("/facturas", {
                ventaId: facturaVenta.ventaId,
                clienteNombre: cliente,
                clienteRfc: rfc,
                clienteCodigoPostal: cp,
                clienteRegimen: regimen,
                usoCfdi: document.getElementById("factura-uso").value,
                formaPago: document.getElementById("factura-forma").value,
                metodoPago: document.getElementById("factura-metodo").value
            }, function(f) {
                if (window.showToast) showToast("Factura " + f.folio + " generada");
                abrirFactura(f.id);
                document.getElementById("factura-cliente").value = "";
                document.getElementById("factura-rfc").value = "";
                document.getElementById("factura-cp").value = "";
                document.getElementById("factura-regimen").value = "";
                buscarVentaFactura();
                loadFacturas(0);
            }, function(msg, xhr) {
                if (window.showToast) showToast(detalleError(msg, xhr), "error");
            });
        }
    );
}

function publicoGeneral() {
    document.getElementById("factura-cliente").value = "PUBLICO EN GENERAL";
    document.getElementById("factura-rfc").value = "XAXX010101000";
    document.getElementById("factura-regimen").value = "616";
    document.getElementById("factura-uso").value = "S01";
    document.getElementById("factura-forma").value = "01";
    document.getElementById("factura-metodo").value = "PUE";
    document.getElementById("factura-cp").value = "";
}

function abrirFactura(id) {
    window.open("/facturas/" + encodeURIComponent(id) + "/pdf");
}

function filaFactura(f) {
    const tr = document.createElement("tr");
    tr.innerHTML = ""
        + "<td style=\"font-weight:600;\">" + escFactura(f.folio) + "</td>"
        + "<td style=\"white-space:nowrap;\">" + escFactura(fmtFechaFactura(f.fecha)) + "</td>"
        + "<td><div>" + escFactura(f.clienteNombre || "—") + "</div>"
        + "<div style=\"font-size:.72rem;color:var(--text-subtle);\">" + escFactura(f.clienteRfc || "") + "</div></td>"
        + "<td><code style=\"font-size:.75rem;\">" + escFactura(f.ventaFolio || "") + "</code></td>"
        + "<td class=\"text-end\" style=\"font-weight:600;\">" + moneyFactura(f.total) + "</td>";
    const td = document.createElement("td");
    td.className = "text-center";
    const btn = document.createElement("button");
    btn.className = "btn btn-outline-secondary btn-sm";
    btn.style.cssText = "padding:.2rem .5rem;font-size:.75rem;";
    btn.innerHTML = '<i class="bi bi-printer"></i>';
    btn.title = "Reimprimir";
    btn.onclick = () => abrirFactura(f.id);
    td.appendChild(btn);
    tr.appendChild(td);
    return tr;
}

function detalleError(msg, xhr) {
    const json = xhr && xhr.responseJSON;
    if (json && (json.detail || json.message)) return json.detail || json.message;
    return msg || "No se pudo completar la operación";
}

function moneyFactura(n) {
    return "$" + Number(n || 0).toLocaleString("es-MX",
        { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtFechaFactura(iso) {
    if (!iso) return "—";
    const texto = String(iso);
    const partes = texto.slice(0, 10).split("-");
    if (partes.length < 3) return texto;
    const hora = texto.length >= 16 ? texto.slice(11, 16) : "";
    return partes[2] + "/" + partes[1] + "/" + partes[0] + (hora ? " " + hora : "");
}

function escFactura(s) {
    return String(s ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

let fiscalCargado = false;

function mostrarDatosFiscales() {
    if (localStorage.getItem("role") !== "ADMIN") return;
    const el = document.getElementById("config-fiscal");
    if (!el) return;
    el.style.display = "block";
    if (!fiscalCargado) {
        fiscalCargado = true;
        cargarDatosFiscales();
    }
}

function cargarDatosFiscales() {
    apiGet("/config/fiscal", pintarFiscal, function() {
        fiscalCargado = false;
        if (window.showToast) showToast("No se pudieron cargar los datos fiscales", "error");
    });
}

function pintarFiscal(c) {
    document.getElementById("fis-rfc").value = c.rfc || "";
    document.getElementById("fis-razon").value = c.razonSocial || "";
    document.getElementById("fis-regimen").value = c.regimenFiscal || "";
    document.getElementById("fis-cp").value = c.codigoPostal || "";
    document.getElementById("fis-clave").value = c.claveProdServ || "01010101";
    document.getElementById("fis-unidad").value = c.claveUnidad || "H87";
    document.getElementById("fis-nocert").value = c.numeroCertificado || "";
    document.getElementById("fis-pac").value = c.pacNombre || "";
    document.getElementById("fis-pac-user").value = c.pacUsuario || "";
    document.getElementById("fis-pac-pass").value = "";
    document.getElementById("fis-key-pass").value = "";
    document.getElementById("fis-pac-pass").placeholder = c.pacPasswordGuardada
        ? "Guardada. Escríbela solo para cambiarla" : "No se vuelve a mostrar";
    document.getElementById("fis-key-pass").placeholder = c.keyPasswordGuardada
        ? "Guardada. Escríbela solo para cambiarla" : "La entrega el contador";
    archivoFiscal("fis-cer-nombre", c.cerCargado, c.certificadoNombre, "cer");
    archivoFiscal("fis-key-nombre", c.keyCargada, c.llaveNombre, "key");

    const box = document.getElementById("fiscal-faltantes");
    const faltan = c.faltantes || [];
    if (faltan.length === 0) {
        box.textContent = "Datos fiscales completos. El certificado y el PAC ya están guardados para emitir el CFDI.";
    } else {
        box.textContent = "Pídele al contador: " + faltan.join(", ") + ".";
    }
}

function archivoFiscal(id, cargado, nombre, tipo) {
    const el = document.getElementById(id);
    el.innerHTML = "";
    if (!cargado) {
        el.textContent = "Sin archivo";
        return;
    }
    const span = document.createElement("span");
    span.textContent = nombre || "Archivo cargado";
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn btn-outline-danger btn-sm";
    btn.style.cssText = "margin-left:.5rem;padding:.05rem .4rem;font-size:.7rem;";
    btn.textContent = "Quitar";
    btn.onclick = () => quitarCertificado(tipo);
    el.appendChild(span);
    el.appendChild(btn);
}

function subirCertificado(tipo, input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 256 * 1024) {
        if (window.showToast) showToast("El archivo no puede superar 256 KB", "error");
        input.value = "";
        return;
    }
    const fd = new FormData();
    fd.append("file", file);
    fetch("/config/fiscal/" + tipo, { method: "POST", body: fd })
        .then(respuestaFiscal)
        .then(c => {
            pintarFiscal(c);
            input.value = "";
            if (window.showToast) showToast(tipo === "cer" ? "Certificado guardado" : "Llave guardada");
        })
        .catch(e => { if (window.showToast) showToast(e.message || "No se pudo guardar el archivo", "error"); });
}

function quitarCertificado(tipo) {
    const titulo = tipo === "cer" ? "Quitar certificado" : "Quitar llave";
    confirmar(titulo, "¿Deseas eliminar el archivo guardado?", function() {
        fetch("/config/fiscal/" + tipo, { method: "DELETE" })
            .then(respuestaFiscal)
            .then(c => {
                pintarFiscal(c);
                if (window.showToast) showToast("Archivo eliminado");
            })
            .catch(e => { if (window.showToast) showToast(e.message || "No se pudo quitar el archivo", "error"); });
    }, true);
}

function guardarFiscal() {
    confirmar(
        "Guardar datos fiscales",
        "Estos datos se usan en las facturas. Las contraseñas y los archivos del certificado no se vuelven a mostrar.",
        function() {
            fetch("/config/fiscal", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    rfc: document.getElementById("fis-rfc").value.trim(),
                    razonSocial: document.getElementById("fis-razon").value.trim(),
                    regimenFiscal: document.getElementById("fis-regimen").value,
                    codigoPostal: document.getElementById("fis-cp").value.trim(),
                    claveProdServ: document.getElementById("fis-clave").value.trim(),
                    claveUnidad: document.getElementById("fis-unidad").value.trim(),
                    numeroCertificado: document.getElementById("fis-nocert").value.trim(),
                    pacNombre: document.getElementById("fis-pac").value.trim(),
                    pacUsuario: document.getElementById("fis-pac-user").value.trim(),
                    pacAmbiente: "PRODUCCION",
                    pacPassword: document.getElementById("fis-pac-pass").value,
                    certificadoPassword: document.getElementById("fis-key-pass").value
                })
            }).then(respuestaFiscal)
              .then(c => {
                  pintarFiscal(c);
                  if (window.showToast) showToast("Datos fiscales guardados");
              })
              .catch(e => { if (window.showToast) showToast(e.message || "Error al guardar", "error"); });
        }
    );
}

function respuestaFiscal(r) {
    if (r.status === 401 || r.status === 403) {
        window.location.href = "/login";
        return Promise.reject(new Error("Sesión expirada"));
    }
    return r.json().then(j => {
        if (!r.ok) throw new Error(j.message || ("Error " + r.status));
        return j;
    });
}

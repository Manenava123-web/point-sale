function loadPrestamos() {
    const persona = document.getElementById("prestamo-persona");
    if (persona && !persona.value.trim()) {
        persona.value = localStorage.getItem("nombre") || localStorage.getItem("user") || "";
    }

    const loading = document.getElementById("prestamos-loading");
    const table   = document.getElementById("prestamos-table-wrap");
    const empty   = document.getElementById("prestamos-empty");
    if (!loading) return;
    loading.style.display = "block";
    table.style.display   = "none";
    empty.style.display   = "none";

    apiGet("/prestamos", renderPrestamos);
}

function guardarPrestamo() {
    const persona  = document.getElementById("prestamo-persona").value.trim();
    const monto    = parseFloat(document.getElementById("prestamo-monto").value);
    const concepto = document.getElementById("prestamo-concepto").value.trim();

    if (!persona || isNaN(monto) || monto <= 0) {
        if (window.showToast) showToast("Indica quién retira y un monto mayor a cero", "error");
        return;
    }

    confirmar(
        "Registrar préstamo",
        "¿Registrar préstamo de $" + monto.toFixed(2) + " a " + persona
            + "? Sale de la caja en el corte de hoy.",
        function() {
            apiPost("/prestamos", { persona, monto, concepto }, function() {
                document.getElementById("prestamo-monto").value = "";
                document.getElementById("prestamo-concepto").value = "";
                if (window.showToast) showToast("Préstamo registrado");
                loadPrestamos();
            });
        }
    );
}

function devolverPrestamo(id, persona, monto) {
    confirmar(
        "Dinero devuelto",
        "¿Registrar la devolución de $" + Number(monto).toFixed(2) + " de " + persona
            + "? Entra al corte de hoy como dinero devuelto.",
        function() {
            apiPost("/prestamos/" + id + "/devolver", {}, function() {
                if (window.showToast) showToast("Devolución registrada");
                loadPrestamos();
            });
        }
    );
}

function anularPrestamo(id, persona, monto) {
    confirmar(
        "Anular préstamo",
        "¿Anular el préstamo de $" + Number(monto).toFixed(2) + " de " + persona
            + "? Dejará de aparecer en el corte.",
        function() {
            fetch("/prestamos/" + id, { method: "DELETE" }).then(function(res) {
                if (res.status === 401 || res.status === 403) {
                    window.location.href = "/login";
                    return;
                }
                if (!res.ok) throw new Error(res.status);
                if (window.showToast) showToast("Préstamo anulado");
                loadPrestamos();
            }).catch(function() {
                if (window.showToast) showToast("No se pudo anular el préstamo", "error");
            });
        },
        true
    );
}

function renderPrestamos(data) {
    const loading = document.getElementById("prestamos-loading");
    const table   = document.getElementById("prestamos-table-wrap");
    const empty   = document.getElementById("prestamos-empty");
    const tbody   = document.getElementById("prestamos-list");
    const count   = document.getElementById("prestamos-count");
    if (!tbody) return;

    const pendientes = data.pendientes || [];
    const devueltos  = data.devueltos || [];
    const hoy = fechaHoy();

    const prestadoHoy = sumar(pendientes.filter(p => diaDe(p.fecha) === hoy))
        + sumar(devueltos.filter(p => diaDe(p.fecha) === hoy));
    const devueltoHoy = sumar(devueltos.filter(p => diaDe(p.fechaDevolucion) === hoy));

    document.getElementById("prestamo-por-devolver").textContent = money(sumar(pendientes));
    document.getElementById("prestamo-hoy").textContent = money(prestadoHoy);
    document.getElementById("prestamo-devuelto-hoy").textContent = money(devueltoHoy);

    tbody.innerHTML = "";
    const filas = pendientes.concat(devueltos);
    filas.forEach(p => tbody.appendChild(filaPrestamo(p)));

    loading.style.display = "none";
    table.style.display = filas.length ? "block" : "none";
    empty.style.display = filas.length ? "none" : "block";
    const n = pendientes.length;
    count.textContent = n + " pendiente" + (n === 1 ? "" : "s");
}

function filaPrestamo(p) {
    const tr = document.createElement("tr");
    const persona = p.persona || "—";
    const devuelto = !!p.devuelto;
    const estado = devuelto
        ? '<span class="badge" style="background:rgba(63,185,80,.15);color:var(--success);font-size:.72rem;">Devuelto '
            + esc(fmtFecha(p.fechaDevolucion)) + "</span>"
        : '<span class="badge" style="background:rgba(210,153,34,.15);color:var(--warning);font-size:.72rem;">Pendiente</span>';

    tr.innerHTML = ""
        + '<td style="white-space:nowrap;">' + esc(fmtFecha(p.fecha)) + "</td>"
        + "<td><div style=\"font-weight:500;\">" + esc(persona) + "</div>"
        + '<div style="font-size:.72rem;color:var(--text-subtle);">Registró ' + esc(p.usuario || "—") + "</div></td>"
        + "<td>" + esc(p.concepto || "—") + "</td>"
        + '<td class="text-end" style="font-weight:600;">' + money(p.monto) + "</td>"
        + '<td class="text-center">' + estado + "</td>";

    const acciones = document.createElement("td");
    acciones.className = "text-center";
    acciones.style.whiteSpace = "nowrap";
    if (!devuelto) {
        const devolver = document.createElement("button");
        devolver.className = "btn btn-outline-success btn-sm";
        devolver.style.cssText = "padding:.2rem .5rem;font-size:.75rem;";
        devolver.innerHTML = '<i class="bi bi-arrow-return-left me-1"></i>Devolver';
        devolver.onclick = () => devolverPrestamo(p.id, persona, p.monto);

        const anular = document.createElement("button");
        anular.className = "btn btn-outline-danger btn-sm";
        anular.style.cssText = "padding:.2rem .5rem;font-size:.75rem;margin-left:.35rem;";
        anular.innerHTML = '<i class="bi bi-x-lg"></i>';
        anular.title = "Anular";
        anular.onclick = () => anularPrestamo(p.id, persona, p.monto);

        acciones.appendChild(devolver);
        acciones.appendChild(anular);
    } else {
        acciones.textContent = "—";
    }
    tr.appendChild(acciones);
    return tr;
}

function sumar(lista) {
    return lista.reduce((s, p) => s + Number(p.monto || 0), 0);
}

function money(n) {
    return "$" + Number(n || 0).toLocaleString("es-MX",
        { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fechaHoy() {
    const d = new Date();
    return d.getFullYear() + "-"
        + String(d.getMonth() + 1).padStart(2, "0") + "-"
        + String(d.getDate()).padStart(2, "0");
}

function diaDe(iso) {
    return iso ? String(iso).slice(0, 10) : "";
}

function fmtFecha(iso) {
    if (!iso) return "—";
    const texto = String(iso);
    const partes = texto.slice(0, 10).split("-");
    if (partes.length < 3) return texto;
    const hora = texto.length >= 16 ? texto.slice(11, 16) : "";
    return partes[2] + "/" + partes[1] + "/" + partes[0] + (hora ? " " + hora : "");
}

function esc(s) {
    return String(s ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function rptDaily()    { window.open("/reports/daily");     }
function rptWeekly()   { window.open("/reports/weekly");    }
function rptMonthly()  { window.open("/reports/monthly");   }
function rptLowStock() { window.open("/reports/low-stock"); }

function rptTicket() {
    const id = document.getElementById("ticket-id").value.trim();
    if (!id) return;
    window.open("/sales/ticket/" + encodeURIComponent(id));
}

/* ── Configuración del negocio ─────────────────────────────── */
function cargarConfig() {
    apiGet("/config/negocio", function(c) {
        document.getElementById("cfg-nombre").value    = c.nombre    || "";
        document.getElementById("cfg-direccion").value = c.direccion || "";
        document.getElementById("cfg-telefono").value  = c.telefono  || "";
        const wrap = document.getElementById("logo-preview-wrap");
        const img  = document.getElementById("logo-preview");
        if (wrap && img) {
            if (c.logo) { img.src = c.logo; wrap.style.display = "flex"; }
            else        { img.src = "";     wrap.style.display = "none"; }
        }
    });
}

function subirLogo(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 1024 * 1024) {
        if (window.showToast) showToast("El logo no puede superar 1 MB", "error");
        input.value = "";
        return;
    }
    const fd = new FormData();
    fd.append("file", file);
    fetch("/config/negocio/logo", { method: "POST", body: fd })
        .then(r => { if (!r.ok) throw r; return r.json(); })
        .then(cfg => {
            if (cfg.logo) {
                document.getElementById("logo-preview").src = cfg.logo;
                document.getElementById("logo-preview-wrap").style.display = "flex";
            }
            if (window.showToast) showToast("Logo guardado");
        })
        .catch(() => { if (window.showToast) showToast("Error al subir logo", "error"); });
}

function eliminarLogo() {
    confirmar("Quitar logo", "¿Deseas eliminar el logo del negocio?", function() {
        fetch("/config/negocio/logo", { method: "DELETE" })
            .then(r => { if (!r.ok) throw r; })
            .then(() => {
                document.getElementById("logo-preview-wrap").style.display = "none";
                document.getElementById("logo-preview").src = "";
                document.getElementById("logo-file").value = "";
                if (window.showToast) showToast("Logo eliminado");
            })
            .catch(() => { if (window.showToast) showToast("Error al eliminar logo", "error"); });
    }, true);
}

function guardarConfig() {
    const nombre = document.getElementById("cfg-nombre").value.trim();
    if (!nombre) { if (window.showToast) showToast("El nombre no puede estar vacío", "error"); return; }

    confirmar(
        "Guardar datos del negocio",
        "Los cambios se aplicarán en todos los reportes y tickets generados a partir de ahora.",
        function() {
            fetch("/config/negocio", {
                method:  "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nombre,
                    direccion: document.getElementById("cfg-direccion").value.trim(),
                    telefono:  document.getElementById("cfg-telefono").value.trim()
                })
            }).then(r => { if (!r.ok) throw r; return r.json(); })
              .then(() => { if (window.showToast) showToast("Datos del negocio guardados"); })
              .catch(() => { if (window.showToast) showToast("Error al guardar", "error"); });
        }
    );
}

function initReports() {
    const dateEl = document.getElementById("today-date");
    if (dateEl) dateEl.textContent = new Date().toLocaleDateString("es-MX",
        { weekday:"long", year:"numeric", month:"long", day:"numeric" });
    if (localStorage.getItem("role") === "ADMIN") {
        const el = document.getElementById("config-negocio");
        if (el) { el.style.display = "block"; cargarConfig(); }
    }
}

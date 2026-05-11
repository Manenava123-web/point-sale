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
    });
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

/* Mostrar sección y cargar datos si es ADMIN */
(function() {
    if (localStorage.getItem("role") === "ADMIN") {
        const el = document.getElementById("config-negocio");
        if (el) { el.style.display = "block"; cargarConfig(); }
    }
})();

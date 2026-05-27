function initDashboard() {

  /* ── Saludo y fecha ── */
  const nombre = localStorage.getItem("nombre") || localStorage.getItem("user") || "Usuario";
  const hora   = new Date().getHours();
  const saludo = hora < 12 ? "Buenos días" : hora < 19 ? "Buenas tardes" : "Buenas noches";
  document.getElementById("dash-greeting").textContent = saludo + ", " + nombre;

  const DIAS   = ["domingo","lunes","martes","miércoles","jueves","viernes","sábado"];
  const MESES  = ["enero","febrero","marzo","abril","mayo","junio",
                  "julio","agosto","septiembre","octubre","noviembre","diciembre"];
  const ahora  = new Date();
  document.getElementById("dash-date").textContent =
    DIAS[ahora.getDay()] + ", " + ahora.getDate() + " de " +
    MESES[ahora.getMonth()] + " de " + ahora.getFullYear();

  const money = v => "$" + Number(v).toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  /* ── KPIs ── */
  apiFetch("/dashboard/resumen", function (d) {
    document.getElementById("kpi-total-hoy").textContent  = money(d.totalHoy);
    document.getElementById("kpi-trans-hoy").textContent  = d.transaccionesHoy;
    document.getElementById("kpi-promedio").textContent   = money(d.promedioHoy);
    document.getElementById("kpi-bajo-stock").textContent = d.bajoStock;

    document.getElementById("kpi-sub-hoy").textContent   =
      "Esta semana " + money(d.totalSemana);
    document.getElementById("kpi-sub-trans").textContent =
      "Esta semana " + d.transaccionesSemana;

    /* Color de alerta en bajo stock */
    if (d.bajoStock > 0) {
      document.getElementById("kpi-bajo-stock").style.color = "var(--danger)";
    }
  });

  /* ── Gráfica semanal ── */
  apiFetch("/dashboard/grafica-semana", function (data) {
    const ctx = document.getElementById("chart-semana").getContext("2d");

    new Chart(ctx, {
      type: "bar",
      data: {
        labels: data.map(d => d.label),
        datasets: [{
          label: "Total ($)",
          data:  data.map(d => d.total),
          backgroundColor: data.map((_, i) =>
            i === data.length - 1
              ? "rgba(34,197,94,.85)"
              : "rgba(34,197,94,.3)"
          ),
          borderColor: data.map((_, i) =>
            i === data.length - 1 ? "#22c55e" : "rgba(34,197,94,.5)"
          ),
          borderWidth: 1,
          borderRadius: 5,
          borderSkipped: false,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: "#0f1729",
            borderColor: "#253044",
            borderWidth: 1,
            titleColor: "#e2e8f0",
            bodyColor: "#94a3b8",
            callbacks: {
              label: ctx => " " + money(ctx.parsed.y),
              afterLabel: (ctx) => {
                const n = data[ctx.dataIndex].count;
                return " " + n + " ticket" + (n !== 1 ? "s" : "");
              }
            }
          }
        },
        scales: {
          x: {
            grid:  { color: "rgba(255,255,255,.04)" },
            ticks: { color: "#64748b", font: { size: 11 } }
          },
          y: {
            grid:  { color: "rgba(255,255,255,.04)" },
            ticks: {
              color: "#64748b",
              font: { size: 11 },
              callback: v => "$" + (v >= 1000 ? (v/1000).toFixed(1) + "k" : v)
            },
            beginAtZero: true
          }
        }
      }
    });
  });

  /* ── Top productos ── */
  apiFetch("/dashboard/top-productos", function (data) {
    const el = document.getElementById("top-productos-list");
    if (!data || data.length === 0) {
      el.innerHTML = '<div style="color:var(--text-subtle);font-size:.82rem;text-align:center;padding:1rem;">Sin ventas registradas</div>';
      return;
    }

    const max = data[0].cantidad;
    el.innerHTML = data.map((p, i) => {
      const pct  = max > 0 ? Math.round((p.cantidad / max) * 100) : 0;
      const rank = ["🥇","🥈","🥉"][i] || (i + 1);
      return `
        <div class="top-item">
          <div class="top-rank">${rank}</div>
          <div class="top-info">
            <div class="top-name">${p.nombre}</div>
            <div class="top-bar-wrap">
              <div class="top-bar" style="width:${pct}%"></div>
            </div>
          </div>
          <div class="top-qty">${p.cantidad}x</div>
        </div>`;
    }).join("");
  });

}

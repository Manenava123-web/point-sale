/* ── Estado ─────────────────────────────────────────────────── */
let prodPage   = 0;
let prodSearch = "";
let searchTimer;
let surtirId   = null;

/* ── Guardar producto ────────────────────────────────────────── */
function saveProduct() {
    const name  = document.getElementById("pname").value.trim();
    const code  = document.getElementById("pcode").value.trim();
    const price = parseFloat(document.getElementById("pprice").value);

    if (!name || isNaN(price)) return;

    confirmar(
        "Guardar producto",
        '¿Deseas guardar el producto "' + name + '" con precio $' + price.toFixed(2) + '?',
        function() {
            apiPost("/products", { name, code, price }, () => {
                document.getElementById("pname").value  = "";
                document.getElementById("pcode").value  = "";
                document.getElementById("pprice").value = "";
                if (window.showToast) showToast("Producto guardado");
                prodPage = 0;
                loadProducts();
            });
        }
    );
}

/* ── Búsqueda con debounce ───────────────────────────────────── */
function onSearch(val) {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        prodSearch = val.trim();
        prodPage   = 0;
        loadProducts();
    }, 320);
}

/* ── Cargar página ───────────────────────────────────────────── */
function loadProducts(page) {
    if (page !== undefined) prodPage = page;

    document.getElementById("prod-loading").style.display   = "block";
    document.getElementById("prod-table-wrap").style.display = "none";
    document.getElementById("prod-empty").style.display      = "none";

    const url = "/products?page=" + prodPage
              + "&size=8"
              + "&search=" + encodeURIComponent(prodSearch);

    apiGet(url, data => renderProducts(data));
}

/* ── Renderizar tabla + paginación ──────────────────────────── */
function renderProducts(data) {
    const tbody    = document.getElementById("plist");
    const loading  = document.getElementById("prod-loading");
    const tableWrap= document.getElementById("prod-table-wrap");
    const empty    = document.getElementById("prod-empty");
    const count    = document.getElementById("prod-count");
    const pagBar   = document.getElementById("prod-pagination");

    tbody.innerHTML = "";

    const list = data.content || [];

    list.forEach(p => {
        const price = parseFloat(p.price).toLocaleString("es-MX",
            { minimumFractionDigits: 2, maximumFractionDigits: 2 });

        const stockColor = p.stock <= 5
            ? "color:#f85149;"
            : p.stock <= 15
                ? "color:var(--warning);"
                : "color:var(--accent-hover);";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td style="font-weight:500;">${p.name}</td>
            <td><code style="font-size:.78rem;color:var(--text-subtle);">${p.code || "—"}</code></td>
            <td class="text-center">
              <span style="font-size:.82rem;font-weight:600;${stockColor}">${p.stock}</span>
            </td>
            <td class="text-end">$${price}</td>
            <td class="text-center">
              <button class="btn btn-outline-secondary btn-sm"
                      style="padding:.2rem .55rem;font-size:.75rem;"
                      onclick='abrirSurtir("${p.id}","${p.name.replace(/'/g,"\\'")}",${p.stock})'>
                <i class="bi bi-box-arrow-in-down me-1"></i>Surtir
              </button>
            </td>`;
        tbody.appendChild(tr);
    });

    const total = data.totalElements || 0;
    const pages = data.totalPages    || 0;

    loading.style.display    = "none";
    tableWrap.style.display  = list.length > 0 ? "block" : "none";
    empty.style.display      = list.length === 0 ? "block" : "none";
    count.textContent        = total + " producto" + (total !== 1 ? "s" : "");

    /* Paginación */
    if (pages <= 1) {
        pagBar.style.setProperty("display", "none", "important");
    } else {
        pagBar.style.removeProperty("display");
        renderPaginacion(data.number, pages, total, data.size);
    }
}

function renderPaginacion(currentPage, totalPages, totalElements, pageSize) {
    const info  = document.getElementById("pag-info");
    const ul    = document.getElementById("pag-controls");

    const desde = currentPage * pageSize + 1;
    const hasta = Math.min((currentPage + 1) * pageSize, totalElements);
    info.textContent = desde + "–" + hasta + " de " + totalElements;

    ul.innerHTML = "";

    /* Botón anterior */
    ul.appendChild(pagBtn("&#8592;", currentPage - 1, currentPage === 0));

    /* Páginas con ventana deslizante */
    const ventana = paginasVentana(currentPage, totalPages);
    let anterior  = null;
    ventana.forEach(n => {
        if (anterior !== null && n - anterior > 1) {
            const li = document.createElement("li");
            li.className = "page-item disabled";
            li.innerHTML = '<span class="page-link" style="background:transparent;border-color:var(--border);color:var(--text-subtle);">…</span>';
            ul.appendChild(li);
        }
        ul.appendChild(pagBtn(n + 1, n, false, n === currentPage));
        anterior = n;
    });

    /* Botón siguiente */
    ul.appendChild(pagBtn("&#8594;", currentPage + 1, currentPage >= totalPages - 1));
}

function pagBtn(label, targetPage, disabled, active) {
    const li = document.createElement("li");
    li.className = "page-item" + (disabled ? " disabled" : "") + (active ? " active" : "");

    const a = document.createElement(disabled ? "span" : "a");
    a.className = "page-link";
    a.innerHTML = label;
    a.style.cssText = [
        "background:" + (active ? "var(--accent)" : "var(--surface-2)"),
        "border-color:" + (active ? "var(--accent)" : "var(--border)"),
        "color:" + (active ? "#fff" : "var(--text-muted)"),
        "min-width:32px",
        "text-align:center",
        "cursor:" + (disabled ? "default" : "pointer"),
    ].join(";");

    if (!disabled && !active) {
        a.addEventListener("mouseover", () => { a.style.background = "var(--surface)"; });
        a.addEventListener("mouseout",  () => { a.style.background = "var(--surface-2)"; });
        a.onclick = (e) => { e.preventDefault(); loadProducts(targetPage); };
    }
    li.appendChild(a);
    return li;
}

function paginasVentana(current, total) {
    const delta = 2;
    const pages = [];
    for (let i = 0; i < total; i++) {
        if (i === 0 || i === total - 1 || (i >= current - delta && i <= current + delta)) {
            pages.push(i);
        }
    }
    return pages;
}

/* ── Modal surtir ───────────────────────────────────────────── */
function abrirSurtir(id, nombre, stockActual) {
    surtirId = id;
    document.getElementById("surtir-nombre").textContent       = nombre;
    document.getElementById("surtir-stock-actual").textContent  = "Stock actual: " + stockActual + " unidades";
    document.getElementById("surtir-cantidad").value            = stockActual;
    document.getElementById("modal-surtir").style.display       = "block";
    const input = document.getElementById("surtir-cantidad");
    setTimeout(() => { input.focus(); input.select(); }, 50);
}

function cerrarSurtir() {
    document.getElementById("modal-surtir").style.display = "none";
    surtirId = null;
}

function confirmarSurtir() {
    const nuevoStock = parseInt(document.getElementById("surtir-cantidad").value);
    if (!surtirId || isNaN(nuevoStock) || nuevoStock < 0) return;

    const idCapturado = surtirId;
    const nombre = document.getElementById("surtir-nombre").textContent;
    cerrarSurtir();

    confirmar(
        "Actualizar stock",
        '¿Confirmas actualizar el stock de "' + nombre + '" a ' + nuevoStock + ' unidades?',
        function() {
            fetch("/products/" + idCapturado + "/stock", {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ stock: nuevoStock })
            }).then(res => {
                if (!res.ok) throw new Error(res.status);
                return res.json();
            }).then(() => {
                if (window.showToast) showToast("Stock actualizado correctamente");
                loadProducts();
            }).catch(() => {
                if (window.showToast) showToast("Error al actualizar stock", "error");
            });
        }
    );
}

/* ── Init: llamado desde loadView en index.html ─────────────── */

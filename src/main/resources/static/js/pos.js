let cart       = [];
let searchTimer = null;
let ddProducts  = [];

/* ── Agregar por código (scanner / Enter) ─────────────────────── */
function addProduct() {
    const code = $("#code").val().trim();
    if (!code) return;
    hideDropdown();

    apiGet("/products/" + code,
        function(p) {
            addToCart(p);
            $("#code").val("").focus();
            showFeedback(p.name, "success");
        },
        function() {
            showFeedback("Producto no encontrado: " + code, "error");
            $("#code").select();
        }
    );
}

/* ── Agregar producto al carrito (lógica compartida) ──────────── */
function addToCart(p) {
    const existing = cart.find(i => i.id === p.id);
    if (existing) {
        existing.qty += 1;
    } else {
        cart.push({ ...p, qty: 1 });
    }
    render();
}

/* ── Búsqueda en tiempo real ──────────────────────────────────── */
function onPosInput(val) {
    clearTimeout(searchTimer);
    val = val.trim();
    if (val.length === 0) { hideDropdown(); return; }
    searchTimer = setTimeout(function() { posSearch(val); }, 260);
}

function handlePosKey(e) {
    if (e.key === "Enter") {
        const dd = document.getElementById("pos-dropdown");
        if (ddProducts.length > 0 && dd.style.display !== "none") {
            selectProduct(ddProducts[0]);
        } else {
            addProduct();
        }
    } else if (e.key === "Escape") {
        hideDropdown();
        document.getElementById("code").focus();
    } else if (e.key === "ArrowDown") {
        e.preventDefault();
        const first = document.querySelector(".pos-dd-row");
        if (first) first.focus();
    }
}

function posSearch(q) {
    apiGet("/products?search=" + encodeURIComponent(q) + "&page=0&size=7",
        function(data) {
            const list = data.content || [];
            if (list.length === 0) { hideDropdown(); return; }
            ddProducts = list;
            renderDropdown(list);
        },
        function() { hideDropdown(); }
    );
}

function renderDropdown(list) {
    const dd = document.getElementById("pos-dropdown");
    dd.innerHTML = list.map(function(p, i) {
        const stockStyle = p.stock <= 5
            ? "color:#f85149;"
            : p.stock <= 15
                ? "color:var(--warning);"
                : "color:var(--success);";
        const codeHtml = p.code
            ? '<span style="font-size:.7rem;color:var(--text-subtle);margin-left:.35rem;">' + p.code + '</span>'
            : "";
        return '<div class="pos-dd-row" tabindex="0"'
            + ' onclick="selectProduct(ddProducts[' + i + '])"'
            + ' onkeydown="ddRowKey(event,' + i + ')">'
            + '<div style="flex:1;min-width:0;">'
            + '<span style="font-weight:500;">' + p.name + '</span>'
            + codeHtml
            + '</div>'
            + '<div style="text-align:right;flex-shrink:0;margin-left:.75rem;">'
            + '<div style="font-weight:600;font-size:.85rem;">$' + parseFloat(p.price).toFixed(2) + '</div>'
            + '<div style="font-size:.7rem;' + stockStyle + '">stock: ' + p.stock + '</div>'
            + '</div>'
            + '</div>';
    }).join("");
    dd.style.display = "block";
}

function ddRowKey(e, idx) {
    if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        selectProduct(ddProducts[idx]);
    } else if (e.key === "ArrowDown") {
        e.preventDefault();
        const rows = document.querySelectorAll(".pos-dd-row");
        if (rows[idx + 1]) rows[idx + 1].focus();
    } else if (e.key === "ArrowUp") {
        e.preventDefault();
        const rows = document.querySelectorAll(".pos-dd-row");
        if (idx === 0) document.getElementById("code").focus();
        else if (rows[idx - 1]) rows[idx - 1].focus();
    } else if (e.key === "Escape") {
        hideDropdown();
        document.getElementById("code").focus();
    }
}

function selectProduct(p) {
    hideDropdown();
    document.getElementById("code").value = "";
    addToCart(p);
    document.getElementById("code").focus();
    showFeedback(p.name, "success");
}

function hideDropdown() {
    ddProducts = [];
    const dd = document.getElementById("pos-dropdown");
    if (dd) dd.style.display = "none";
}

/* ── Quitar item del carrito ──────────────────────────────────── */
function removeItem(idx) {
    const item = cart[idx];
    confirmar(
        "Quitar producto",
        '¿Deseas quitar "' + item.name + '" del carrito?',
        function() {
            cart.splice(idx, 1);
            render();
            if (window.showToast) showToast('"' + item.name + '" eliminado del carrito', 'warning');
        },
        true
    );
}

/* ── Vaciar carrito ───────────────────────────────────────────── */
function clearCart() {
    if (cart.length === 0) return;
    confirmar(
        "Vaciar carrito",
        "Se eliminarán todos los productos del carrito. ¿Continuar?",
        function() {
            cart = [];
            render();
            if (window.showToast) showToast("Carrito vaciado", "warning");
        },
        true
    );
}

function fmt(n) {
    return parseFloat(n).toLocaleString("es-MX",
        { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/* ── Render del carrito ───────────────────────────────────────── */
function render() {
    const tbody = $("#cart");
    const empty = document.getElementById("cart-empty");
    const table = document.getElementById("cart-table");
    const btn   = document.getElementById("checkout-btn");

    tbody.empty();
    let total    = 0;
    let totalQty = 0;

    cart.forEach((p, i) => {
        const sub = p.price * p.qty;
        total    += sub;
        totalQty += p.qty;
        tbody.append(`
          <tr>
            <td>
              <div style="font-weight:500;">${p.name}</div>
              <div style="font-size:.75rem;color:var(--text-subtle);">$${fmt(p.price)} c/u</div>
            </td>
            <td class="text-center">
              <span style="background:var(--surface-2);border:1px solid var(--border);border-radius:4px;
                           padding:.1rem .5rem;font-size:.85rem;color:var(--text);">${p.qty}</span>
            </td>
            <td class="text-end" style="font-weight:600;">$${fmt(sub)}</td>
            <td>
              <button onclick="removeItem(${i})" class="btn btn-sm p-0"
                      style="color:var(--text-subtle);background:none;border:none;width:28px;height:28px;"
                      title="Quitar">
                <i class="bi bi-x-lg" style="font-size:.75rem;"></i>
              </button>
            </td>
          </tr>`);
    });

    const hasItems = cart.length > 0;
    if (empty) empty.style.display = hasItems ? "none"  : "block";
    if (table) table.style.display = hasItems ? "table" : "none";
    if (btn)   btn.disabled        = !hasItems;

    $("#total").text(fmt(total));
    $("#stat-items").text(totalQty);
    $("#stat-sub").text("$" + fmt(total));
}

function showFeedback(text, type) {
    const fb = document.getElementById("product-feedback");
    if (!fb) return;
    fb.innerHTML = type === "success"
        ? `<div class="pos-alert pos-alert-success"><i class="bi bi-check-circle me-1"></i>${text} agregado</div>`
        : `<div class="pos-alert pos-alert-error"><i class="bi bi-x-circle me-1"></i>${text}</div>`;
    fb.style.display = "block";
    clearTimeout(fb._t);
    fb._t = setTimeout(() => { fb.style.display = "none"; }, 2500);
}

/* ── Cobrar ───────────────────────────────────────────────────── */
function checkout() {
    if (cart.length === 0) return;
    const total = cart.reduce((s, p) => s + p.price * p.qty, 0);
    document.getElementById("cobro-total").textContent = "$" + fmt(total);
    document.getElementById("cobro-input").value = "";
    document.getElementById("cobro-cambio-wrap").style.display = "none";
    document.getElementById("cobro-ok").disabled = true;
    document.getElementById("modal-cobro").style.display = "block";
    setTimeout(() => document.getElementById("cobro-input").focus(), 80);
}

window.cerrarCobro = function() {
    document.getElementById("modal-cobro").style.display = "none";
};

window.setBillete = function(monto) {
    document.getElementById("cobro-input").value = monto;
    actualizarCambio();
};

window.actualizarCambio = function() {
    const total    = cart.reduce((s, p) => s + p.price * p.qty, 0);
    const recibido = parseFloat(document.getElementById("cobro-input").value) || 0;
    const cambio   = recibido - total;
    const wrap     = document.getElementById("cobro-cambio-wrap");
    const cambioEl = document.getElementById("cobro-cambio");
    const label    = document.getElementById("cobro-cambio-label");
    const btn      = document.getElementById("cobro-ok");

    wrap.style.display = "block";
    if (cambio >= 0) {
        wrap.style.background  = "rgba(34,197,94,.12)";
        wrap.style.border      = "1px solid rgba(34,197,94,.25)";
        cambioEl.style.color   = "var(--accent)";
        cambioEl.textContent   = "$" + fmt(cambio);
        label.textContent      = "Cambio";
        btn.disabled           = false;
    } else {
        wrap.style.background  = "rgba(239,68,68,.1)";
        wrap.style.border      = "1px solid rgba(239,68,68,.25)";
        cambioEl.style.color   = "var(--danger)";
        cambioEl.textContent   = "Faltan $" + fmt(Math.abs(cambio));
        label.textContent      = "Monto insuficiente";
        btn.disabled           = true;
    }
};

window.confirmarCobro = function() {
    const recibido = parseFloat(document.getElementById("cobro-input").value) || 0;
    document.getElementById("modal-cobro").style.display = "none";
    _doCheckout(recibido);
};

function _doCheckout(montoPagado) {
    const btn = document.getElementById("checkout-btn");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Procesando…';

    const ticketWin = window.open("", "_blank");

    const expanded = [];
    cart.forEach(item => {
        for (let i = 0; i < item.qty; i++) {
            expanded.push({ id: item.id, name: item.name, price: item.price,
                            code: item.code, stock: item.stock });
        }
    });

    const usuario = localStorage.getItem("nombre") || localStorage.getItem("user") || "";

    apiPost("/sales", { items: expanded, usuario, montoPagado },
        function(r) {
            ticketWin.location = "/sales/ticket/" + r.id;
            cart = [];
            render();
            btn.innerHTML = '<i class="bi bi-cash-coin me-1"></i> Cobrar y generar ticket';
            if (window.showToast) showToast("Venta completada exitosamente");
        },
        function(msg) {
            ticketWin.close();
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-cash-coin me-1"></i> Cobrar y generar ticket';
            if (window.showToast) showToast("Error al registrar la venta: " + msg, "error");
        }
    );
}

/* Cerrar dropdown al hacer clic fuera */
document.addEventListener("click", function(e) {
    const dd    = document.getElementById("pos-dropdown");
    const input = document.getElementById("code");
    if (dd && !dd.contains(e.target) && e.target !== input) hideDropdown();
});

render();
/* Chrome autofills asynchronously — clear after it runs */
const _codeInput = document.getElementById("code");
if (_codeInput) {
    _codeInput.value = "";
    setTimeout(() => { _codeInput.value = ""; }, 300);
}

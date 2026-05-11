let cart = [];

function addProduct() {
    const code = $("#code").val().trim();
    if (!code) return;

    apiGet("/products/" + code,
        function(p) {
            const existing = cart.find(i => i.code === p.code);
            if (existing) {
                existing.qty += 1;
            } else {
                cart.push({ ...p, qty: 1 });
            }
            render();
            $("#code").val("").focus();
            showFeedback(p.name, "success");
        },
        function(msg) {
            showFeedback("Producto no encontrado: " + code, "error");
            $("#code").select();
        }
    );
}

function removeItem(idx) {
    cart.splice(idx, 1);
    render();
}

function clearCart() {
    if (cart.length === 0) return;
    confirmar(
        "Vaciar carrito",
        "Se eliminarán todos los productos del carrito. ¿Continuar?",
        function() { cart = []; render(); },
        true
    );
}

function fmt(n) {
    return parseFloat(n).toLocaleString("es-MX",
        { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

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
                           padding:.1rem .5rem;font-size:.85rem;">${p.qty}</span>
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

function checkout() {
    if (cart.length === 0) return;

    const total = cart.reduce((s, p) => s + p.price * p.qty, 0);
    const items = cart.reduce((s, p) => s + p.qty, 0);

    confirmar(
        "Confirmar venta",
        items + " producto(s) — Total: $" + fmt(total) + ". Se generará el ticket de venta.",
        _doCheckout
    );
}

function _doCheckout() {
    const btn = document.getElementById("checkout-btn");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Procesando…';

    /* Abrir ventana antes del fetch para evitar bloqueo de popups */
    const ticketWin = window.open("", "_blank");

    const expanded = [];
    cart.forEach(item => {
        for (let i = 0; i < item.qty; i++) {
            expanded.push({ id: item.id, name: item.name, price: item.price,
                            code: item.code, stock: item.stock });
        }
    });

    const usuario = localStorage.getItem("nombre") || localStorage.getItem("user") || "";

    apiPost("/sales", { items: expanded, usuario },
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

render();

let editId = null;
let passId = null;

function loadUsers() {
  document.getElementById("users-loading").style.display  = "block";
  document.getElementById("users-table-wrap").style.display = "none";
  document.getElementById("users-empty").style.display    = "none";

  apiFetch("/users", data => {
    document.getElementById("users-loading").style.display = "none";
    if (!data || data.length === 0) {
      document.getElementById("users-empty").style.display = "block";
      document.getElementById("users-count").textContent   = "";
    } else {
      document.getElementById("users-count").textContent    = data.length + " usuario" + (data.length !== 1 ? "s" : "");
      document.getElementById("users-table-wrap").style.display = "block";
      renderUsers(data);
    }
  });
}

function renderUsers(data) {
  const tbody = document.getElementById("ulist");
  tbody.innerHTML = data.map(u => {
    const displayName = u.nombre && u.nombre.trim() ? u.nombre : u.username;

    const rolBadge = u.rol === "ADMIN"
      ? '<span class="badge" style="background:rgba(56,139,253,.15);color:var(--info);font-size:.75rem;">ADMIN</span>'
      : '<span class="badge" style="background:rgba(247,197,68,.15);color:var(--warning);font-size:.75rem;">CAJA</span>';

    const estadoBadge = u.activo
      ? '<span class="badge" style="background:rgba(63,185,80,.15);color:var(--success);font-size:.75rem;">Activo</span>'
      : '<span class="badge" style="background:rgba(248,81,73,.15);color:var(--danger);font-size:.75rem;">Inactivo</span>';

    const toggleLabel = u.activo ? "Desactivar" : "Activar";
    const toggleIcon  = u.activo ? "bi-person-dash" : "bi-person-check";
    const toggleStyle = u.activo
      ? "btn btn-outline-secondary btn-sm"
      : "btn btn-outline-success btn-sm";

    return `<tr>
      <td style="vertical-align:middle;">
        <div style="font-weight:500;">${esc(displayName)}</div>
        ${u.nombre && u.nombre.trim() ? `<div style="font-size:.75rem;color:var(--text-subtle);">@${esc(u.username)}</div>` : ''}
      </td>
      <td class="text-center" style="vertical-align:middle;">${rolBadge}</td>
      <td class="text-center" style="vertical-align:middle;">${estadoBadge}</td>
      <td class="text-center" style="vertical-align:middle;">
        <div style="display:flex;gap:.35rem;justify-content:center;flex-wrap:wrap;">
          <button class="btn btn-outline-primary btn-sm"
                  onclick='abrirEditarUsuario("${esc(u.id)}","${esc(u.username)}","${esc(u.nombre||"")}","${esc(u.rol)}")'>
            <i class="bi bi-pencil"></i>
          </button>
          <button class="btn btn-outline-warning btn-sm"
                  onclick='abrirPassword("${esc(u.id)}","${esc(displayName)}")'>
            <i class="bi bi-key"></i>
          </button>
          <button class="${toggleStyle}"
                  onclick='cambiarActivo("${esc(u.id)}","${esc(displayName)}",${u.activo})'>
            <i class="bi ${toggleIcon} me-1"></i>${toggleLabel}
          </button>
        </div>
      </td>
    </tr>`;
  }).join("");
}

function esc(s) {
  return String(s ?? "").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

/* ── Crear usuario ── */
function saveUser() {
  const nombre    = document.getElementById("unombre").value.trim();
  const username  = document.getElementById("uuser").value.trim();
  const password  = document.getElementById("upass").value;
  const password2 = document.getElementById("upass2").value;
  const role      = document.getElementById("urole").value;
  const mismatch  = document.getElementById("upass-mismatch");

  if (!username || !password) return;

  if (password !== password2) {
    mismatch.style.display = "block";
    document.getElementById("upass2").focus();
    return;
  }
  mismatch.style.display = "none";

  const label = nombre || username;
  confirmar(
    "Crear usuario",
    'Se creará el usuario "' + label + '" con rol ' + role + '. ¿Continuar?',
    function() {
      apiPost("/users", { username, nombre, password, rol: role }, () => {
        document.getElementById("unombre").value = "";
        document.getElementById("uuser").value   = "";
        document.getElementById("upass").value   = "";
        document.getElementById("upass2").value  = "";
        document.getElementById("urole").value   = "CAJA";
        if (window.showToast) showToast('Usuario "' + label + '" creado correctamente');
        loadUsers();
      });
    }
  );
}

/* ── Editar usuario ── */
function abrirEditarUsuario(id, username, nombre, rol) {
  editId = id;
  document.getElementById("edit-nombre").value   = nombre;
  document.getElementById("edit-username").value = username;
  document.getElementById("edit-rol").value      = rol;
  document.getElementById("modal-edit-user").style.display = "block";
  setTimeout(() => document.getElementById("edit-nombre").focus(), 50);
}

function cerrarEditarUsuario() {
  editId = null;
  document.getElementById("modal-edit-user").style.display = "none";
}

function guardarEdicion() {
  const nombre   = document.getElementById("edit-nombre").value.trim();
  const username = document.getElementById("edit-username").value.trim();
  const rol      = document.getElementById("edit-rol").value;
  if (!username) return;

  const idCapturado = editId;
  cerrarEditarUsuario();

  const label = nombre || username;
  confirmar(
    "Guardar cambios",
    'Se actualizará a "' + label + '" (@' + username + ') con rol ' + rol + '. ¿Continuar?',
    function() {
      apiPut("/users/" + idCapturado, { username, nombre, rol }, () => {
        if (window.showToast) showToast("Usuario actualizado correctamente");
        loadUsers();
      });
    }
  );
}

/* ── Cambiar contraseña ── */
function abrirPassword(id, username) {
  passId = id;
  document.getElementById("pass-username-label").textContent = "Usuario: " + username;
  document.getElementById("pass-nueva").value      = "";
  document.getElementById("pass-confirmar").value  = "";
  document.getElementById("pass-mismatch").style.display = "none";
  document.getElementById("modal-pass").style.display = "block";
  setTimeout(() => document.getElementById("pass-nueva").focus(), 50);
}

function cerrarPassword() {
  passId = null;
  document.getElementById("modal-pass").style.display    = "none";
  document.getElementById("pass-mismatch").style.display = "none";
}

function guardarPassword() {
  const pwd  = document.getElementById("pass-nueva").value;
  const pwd2 = document.getElementById("pass-confirmar").value;
  const mismatch = document.getElementById("pass-mismatch");

  if (!pwd) return;

  if (pwd !== pwd2) {
    mismatch.style.display = "block";
    document.getElementById("pass-confirmar").focus();
    return;
  }
  mismatch.style.display = "none";

  const idCapturado = passId;
  cerrarPassword();

  confirmar(
    "Cambiar contraseña",
    "Se cambiará la contraseña del usuario. ¿Continuar?",
    function() {
      apiPatch("/users/" + idCapturado + "/password", { password: pwd }, () => {
        if (window.showToast) showToast("Contraseña actualizada correctamente");
      });
    }
  );
}

/* ── Activar / Desactivar ── */
function cambiarActivo(id, username, activo) {
  const accion = activo ? "desactivar" : "activar";
  confirmar(
    (activo ? "Desactivar" : "Activar") + " usuario",
    '¿Deseas ' + accion + ' al usuario "' + username + '"?',
    function() {
      apiPatch("/users/" + id + "/activo", {}, () => {
        if (window.showToast) showToast('Usuario "' + username + '" ' + (activo ? "desactivado" : "activado"));
        loadUsers();
      });
    },
    activo
  );
}

loadUsers();

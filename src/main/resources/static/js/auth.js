function login() {
    if (typeof setLoginLoading === 'function') setLoginLoading(true);

    apiPost("/auth/login", {
        username: $("#user").val().trim(),
        password: $("#pass").val()
    },
    function(res) {
        localStorage.setItem("role",   res.role   || "");
        localStorage.setItem("user",   res.user   || "");
        localStorage.setItem("nombre", res.nombre || res.user || "");
        window.location.href = "/index";
    },
    function() {
        if (typeof showLoginError === 'function') {
            showLoginError("Usuario o contraseña incorrectos");
        } else if (typeof setLoginLoading === 'function') {
            setLoginLoading(false);
        }
    });
}

function requireRole(role) {
    const stored = localStorage.getItem("role");
    if (stored !== role) {
        if (window.showToast) {
            showToast("Acceso denegado — se requiere rol " + role, "error");
        }
        setTimeout(() => { window.location.href = "/index"; }, 800);
    }
}

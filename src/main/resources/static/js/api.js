/* Base URL vacío = mismo origen, las cookies de sesión se envían automáticamente */
const API = "";

function apiGet(url, cb, errCb) {
    $.ajax({
        url: url,
        method: "GET",
        success: cb,
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                window.location.href = "/login";
                return;
            }
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : "Error " + xhr.status;
            if (errCb) {
                errCb(msg, xhr);
            } else if (window.showToast) {
                showToast(msg, "error");
            }
        }
    });
}

function apiFetch(url, cb, errCb) {
    apiGet(url, cb, errCb);
}

function apiPut(url, data, cb, errCb) {
    $.ajax({
        url: url,
        method: "PUT",
        contentType: "application/json",
        data: JSON.stringify(data),
        success: cb,
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                window.location.href = "/login";
                return;
            }
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : "Error " + xhr.status;
            if (errCb) {
                errCb(msg, xhr);
            } else if (window.showToast) {
                showToast(msg, "error");
            }
        }
    });
}

function apiPatch(url, data, cb, errCb) {
    $.ajax({
        url: url,
        method: "PATCH",
        contentType: "application/json",
        data: JSON.stringify(data),
        success: cb,
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                window.location.href = "/login";
                return;
            }
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : "Error " + xhr.status;
            if (errCb) {
                errCb(msg, xhr);
            } else if (window.showToast) {
                showToast(msg, "error");
            }
        }
    });
}

function apiPost(url, data, cb, errCb) {
    $.ajax({
        url: url,
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify(data),
        success: cb,
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                window.location.href = "/login";
                return;
            }
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : "Error " + xhr.status;
            if (errCb) {
                errCb(msg, xhr);
            } else if (window.showToast) {
                showToast(msg, "error");
            }
        }
    });
}

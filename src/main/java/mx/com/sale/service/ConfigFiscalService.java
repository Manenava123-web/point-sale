package mx.com.sale.service;

import lombok.RequiredArgsConstructor;
import mx.com.sale.model.ConfigFiscal;
import mx.com.sale.repository.ConfigFiscalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigFiscalService {

    final ConfigFiscalRepository repo;
    final AuditService audit;

    private static final Long ID = 1L;

    public ConfigFiscal obtener() {
        return repo.findById(ID).orElseGet(() -> {
            ConfigFiscal c = new ConfigFiscal();
            c.setId(ID);
            c.setClaveProdServ("01010101");
            c.setClaveUnidad("H87");
            c.setPacAmbiente("PRODUCCION");
            return c;
        });
    }

    public Map<String, Object> vista() {
        return vistaDe(obtener());
    }

    @Transactional
    public Map<String, Object> actualizar(ConfigFiscal body, String usuario) {
        ConfigFiscal actual = obtener();
        actual.setId(ID);
        actual.setRfc(rfc(body.getRfc()));
        actual.setRazonSocial(texto(body.getRazonSocial(), 180));
        actual.setRegimenFiscal(codigo(body.getRegimenFiscal(), 3));
        actual.setCodigoPostal(cp(body.getCodigoPostal()));
        actual.setClaveProdServ(vacio(body.getClaveProdServ()) ? "01010101" : texto(body.getClaveProdServ(), 8));
        actual.setClaveUnidad(vacio(body.getClaveUnidad()) ? "H87" : texto(body.getClaveUnidad(), 3));
        actual.setPacNombre(texto(body.getPacNombre(), 80));
        actual.setPacUsuario(texto(body.getPacUsuario(), 80));
        actual.setPacAmbiente("PRODUCCION");
        actual.setNumeroCertificado(texto(body.getNumeroCertificado(), 40));
        if (!vacio(body.getPacPassword())) actual.setPacPassword(body.getPacPassword().trim());
        if (!vacio(body.getCertificadoPassword())) actual.setCertificadoPassword(body.getCertificadoPassword().trim());
        ConfigFiscal saved = repo.save(actual);
        audit.log(usuario, "CONFIG FISCAL");
        return vistaDe(saved);
    }

    @Transactional
    public Map<String, Object> guardarArchivo(String tipo, byte[] bytes, String nombre, String usuario) {
        if (bytes == null || bytes.length == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        if (bytes.length > 256 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no puede superar 256 KB");
        String limpio = nombre == null ? "" : nombre.toLowerCase();
        ConfigFiscal actual = obtener();
        actual.setId(ID);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        if ("cer".equals(tipo)) {
            if (!limpio.endsWith(".cer"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El certificado debe ser un archivo .cer");
            actual.setCertificadoCer(b64);
            actual.setCertificadoNombre(texto(nombre, 120));
        } else if ("key".equals(tipo)) {
            if (!limpio.endsWith(".key"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La llave debe ser un archivo .key");
            actual.setCertificadoKey(b64);
            actual.setLlaveNombre(texto(nombre, 120));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no reconocido");
        }
        ConfigFiscal saved = repo.save(actual);
        audit.log(usuario, "CONFIG FISCAL ARCHIVO " + tipo);
        return vistaDe(saved);
    }

    @Transactional
    public Map<String, Object> quitarArchivo(String tipo, String usuario) {
        ConfigFiscal actual = obtener();
        if ("cer".equals(tipo)) {
            actual.setCertificadoCer(null);
            actual.setCertificadoNombre(null);
        } else if ("key".equals(tipo)) {
            actual.setCertificadoKey(null);
            actual.setLlaveNombre(null);
            actual.setCertificadoPassword(null);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no reconocido");
        }
        ConfigFiscal saved = repo.save(actual);
        audit.log(usuario, "CONFIG FISCAL QUITAR " + tipo);
        return vistaDe(saved);
    }

    private Map<String, Object> vistaDe(ConfigFiscal c) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rfc", nulo(c.getRfc()));
        body.put("razonSocial", nulo(c.getRazonSocial()));
        body.put("regimenFiscal", nulo(c.getRegimenFiscal()));
        body.put("codigoPostal", nulo(c.getCodigoPostal()));
        body.put("claveProdServ", nulo(c.getClaveProdServ()));
        body.put("claveUnidad", nulo(c.getClaveUnidad()));
        body.put("pacNombre", nulo(c.getPacNombre()));
        body.put("pacUsuario", nulo(c.getPacUsuario()));
        body.put("pacAmbiente", "PRODUCCION");
        body.put("numeroCertificado", nulo(c.getNumeroCertificado()));
        body.put("certificadoNombre", nulo(c.getCertificadoNombre()));
        body.put("llaveNombre", nulo(c.getLlaveNombre()));
        body.put("cerCargado", !vacio(c.getCertificadoCer()));
        body.put("keyCargada", !vacio(c.getCertificadoKey()));
        body.put("pacPasswordGuardada", !vacio(c.getPacPassword()));
        body.put("keyPasswordGuardada", !vacio(c.getCertificadoPassword()));
        body.put("faltantes", faltantes(c));
        return body;
    }

    private List<String> faltantes(ConfigFiscal c) {
        List<String> faltan = new ArrayList<>();
        if (vacio(c.getRfc())) faltan.add("RFC del emisor");
        if (vacio(c.getRazonSocial())) faltan.add("Razón social");
        if (vacio(c.getRegimenFiscal())) faltan.add("Régimen fiscal");
        if (vacio(c.getCodigoPostal())) faltan.add("Código postal");
        if (vacio(c.getCertificadoCer())) faltan.add("Certificado .cer");
        if (vacio(c.getCertificadoKey())) faltan.add("Llave .key");
        if (vacio(c.getCertificadoPassword())) faltan.add("Contraseña de la llave");
        if (vacio(c.getPacNombre())) faltan.add("Nombre del PAC");
        if (vacio(c.getPacUsuario())) faltan.add("Usuario del PAC");
        if (vacio(c.getPacPassword())) faltan.add("Contraseña del PAC");
        return faltan;
    }

    private String rfc(String valor) {
        if (vacio(valor)) return null;
        String limpio = valor.trim().toUpperCase().replace(" ", "").replace("-", "");
        if (limpio.length() != 12 && limpio.length() != 13)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RFC del emisor debe tener 12 o 13 caracteres");
        return limpio;
    }

    private String cp(String valor) {
        if (vacio(valor)) return null;
        String limpio = valor.trim();
        if (!limpio.matches("\\d{5}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código postal debe tener 5 dígitos");
        return limpio;
    }

    private String codigo(String valor, int max) {
        return texto(valor == null ? null : valor.trim().toUpperCase(), max);
    }

    private String texto(String valor, int max) {
        if (vacio(valor)) return null;
        String limpio = valor.trim();
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }

    private String nulo(String valor) {
        return valor == null ? "" : valor;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}

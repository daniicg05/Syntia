package com.syntia.mvp.service;

import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de integración con la API pública de la BDNS
 * (Base de Datos Nacional de Subvenciones).
 * <p>
 * Endpoint real descubierto de la SPA Angular del portal:
 * GET https://www.infosubvenciones.es/bdnstrans/api/convocatorias/busqueda?vpn=GE&vln=es&numPag={pagina}&tamPag={tamano}
 * <p>
 * La API devuelve un JSON paginado con el campo {@code content} (lista de convocatorias)
 * y {@code totalElements} (total de registros en BDNS, ~615.000).
 * <p>
 * El certificado SSL del servidor gubernamental no está en el truststore por defecto de Java,
 * por lo que se configura un SSLContext permisivo para estas peticiones.
 */
@Slf4j
@Service
public class BdnsClientService {

    private static final String BDNS_BUSQUEDA =
            "https://www.infosubvenciones.es/bdnstrans/api/convocatorias/busqueda";

    private final RestClient restClient;

    public BdnsClientService() {
        this.restClient = RestClient.builder()
                .requestFactory(createSslPermissiveFactory())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Importa convocatorias reales desde la API pública de la BDNS.
     *
     * @param pagina número de página (0-indexed)
     * @param tamano registros por página (máximo 50 recomendado por la API)
     * @return lista de ConvocatoriaDTO mapeados desde la respuesta de BDNS
     * @throws BdnsException si la API no está disponible o devuelve error
     */
    public List<ConvocatoriaDTO> importar(int pagina, int tamano) {
        log.info("Consultando API BDNS real: pagina={} tamano={}", pagina, tamano);

        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = restClient.get()
                .uri(BDNS_BUSQUEDA + "?vpn=GE&vln=es&numPag={pag}&tamPag={tam}",
                     pagina, Math.min(tamano, 50))
                .retrieve()
                .body(Map.class);

        if (respuesta == null) {
            throw new BdnsException("La API de BDNS devolvió una respuesta vacía");
        }

        Object totalObj = respuesta.get("totalElements");
        log.info("BDNS: totalElements={}", totalObj);

        return mapearRespuesta(respuesta);
    }

    /**
     * Busca convocatorias en toda la BDNS (615.000+) filtrando por palabras clave.
     * Devuelve hasta {@code tamano} resultados relevantes por página.
     * <p>
     * Parámetro API: {@code descripcion} = texto de búsqueda,
     * {@code descripcionTipoBusqueda} = 1 (contiene todas las palabras).
     *
     * @param keywords palabras clave de búsqueda (p.ej. "digitalizacion pyme tecnologia")
     * @param pagina   número de página (0-indexed)
     * @param tamano   registros por página (máximo 50)
     * @return lista de ConvocatoriaDTO que coinciden con la búsqueda
     */
    public List<ConvocatoriaDTO> buscarPorTexto(String keywords, int pagina, int tamano) {
        log.info("BDNS búsqueda por texto: '{}' pagina={} tamano={}", keywords, pagina, tamano);

        // vigente=true → solo convocatorias con plazo abierto
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = restClient.get()
                .uri(BDNS_BUSQUEDA + "?vpn=GE&vln=es&numPag={pag}&tamPag={tam}" +
                     "&descripcion={desc}&descripcionTipoBusqueda=1&vigente=true",
                     pagina, Math.min(tamano, 50), keywords)
                .retrieve()
                .body(Map.class);

        if (respuesta == null) {
            throw new BdnsException("BDNS devolvió respuesta vacía para búsqueda: " + keywords);
        }

        Object totalObj = respuesta.get("totalElements");
        log.info("BDNS búsqueda '{}': totalElements={}", keywords, totalObj);

        return mapearRespuesta(respuesta);
    }

    // ── Detalle enriquecido de una convocatoria ──────────────────────────────

    /**
     * Obtiene el texto enriquecido de una convocatoria BDNS a partir de su ID interno.
     * Llama al endpoint de detalle de la API BDNS y extrae todos los campos de texto
     * relevantes (objeto, beneficiarios, bases reguladoras, requisitos, dotación...).
     * Este texto se pasa a OpenAI para que la guía sea precisa y específica.
     *
     * @param idBdns ID interno de la convocatoria en BDNS (campo "id" del JSON)
     * @return texto concatenado con todos los campos relevantes, o null si no disponible
     */
    public String obtenerDetalleTexto(String idBdns) {
        if (idBdns == null || idBdns.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> detalle = restClient.get()
                    .uri("https://www.infosubvenciones.es/bdnstrans/api/convocatorias/" + idBdns)
                    .retrieve()
                    .body(Map.class);

            if (detalle == null) return null;

            StringBuilder texto = new StringBuilder();
            // Campos de texto enriquecido que devuelve la API de detalle BDNS
            appendCampo(texto, "Objeto",          detalle, "objeto", "descripcionObjeto", "finalidad");
            appendCampo(texto, "Beneficiarios",   detalle, "beneficiarios", "tiposBeneficiarios");
            appendCampo(texto, "Requisitos",      detalle, "requisitos", "condicionesAcceso", "requisitosParticipacion");
            appendCampo(texto, "Dotación",        detalle, "dotacion", "presupuestoTotal", "importeTotal");
            appendCampo(texto, "Bases reguladoras", detalle, "basesReguladoras", "normativa");
            appendCampo(texto, "Plazo solicitud", detalle, "plazoSolicitudes", "plazoPresentacion");
            appendCampo(texto, "Procedimiento",   detalle, "procedimiento", "formaPresentacion");
            appendCampo(texto, "Documentación",   detalle, "documentacion", "documentosRequeridos");

            String resultado = texto.toString().trim();
            log.debug("BDNS detalle id={}: {} chars extraídos", idBdns, resultado.length());
            return resultado.isEmpty() ? null : resultado;

        } catch (Exception e) {
            log.debug("BDNS detalle no disponible para id={}: {}", idBdns, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void appendCampo(StringBuilder sb, String etiqueta, Map<String, Object> mapa, String... claves) {
        for (String clave : claves) {
            Object val = mapa.get(clave);
            if (val == null) continue;
            String texto = extraerTexto(val);
            if (!texto.isBlank()) {
                sb.append(etiqueta).append(": ").append(texto.trim()).append("\n");
                return; // con el primer campo encontrado es suficiente
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerTexto(Object val) {
        if (val instanceof String s) return s;
        if (val instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            list.forEach(item -> {
                if (item instanceof String s) sb.append(s).append("; ");
                else if (item instanceof Map<?,?> m) {
                    // Intentar campos de texto comunes en objetos anidados
                    for (String campo : new String[]{"descripcion", "nombre", "texto", "valor"}) {
                        Object v = m.get(campo);
                        if (v instanceof String s && !s.isBlank()) { sb.append(s).append("; "); break; }
                    }
                }
            });
            return sb.toString();
        }
        if (val instanceof Map<?,?> m) {
            for (String campo : new String[]{"descripcion", "nombre", "texto", "valor"}) {
                Object v = m.get(campo);
                if (v instanceof String s && !s.isBlank()) return s;
            }
        }
        return val.toString();
    }



    @SuppressWarnings("unchecked")
    private List<ConvocatoriaDTO> mapearRespuesta(Map<String, Object> respuesta) {
        List<ConvocatoriaDTO> resultado = new ArrayList<>();

        Object contenido = respuesta.get("content");
        if (!(contenido instanceof List<?> lista)) {
            log.warn("BDNS: respuesta sin campo 'content'. Keys: {}", respuesta.keySet());
            return resultado;
        }

        for (Object item : lista) {
            if (!(item instanceof Map<?, ?> conv)) continue;
            try {
                resultado.add(mapearConvocatoria((Map<String, Object>) conv));
            } catch (Exception e) {
                log.debug("BDNS: error mapeando item: {}", e.getMessage());
            }
        }

        log.info("BDNS API: {} convocatorias mapeadas de esta página", resultado.size());
        return resultado;
    }

    /**
     * Mapea un objeto JSON de la BDNS a ConvocatoriaDTO.
     * <p>
     * Campos de la API BDNS:
     * - id: ID interno BDNS
     * - descripcion: título/descripción de la convocatoria
     * - numeroConvocatoria: código BDNS
     * - nivel1: ámbito (ESTADO, AUTONOMICA, LOCAL, OTROS)
     * - nivel2: organismo / comunidad
     * - nivel3: sub-organismo
     * - fechaRecepcion: fecha de registro en BDNS
     */
    private ConvocatoriaDTO mapearConvocatoria(Map<String, Object> c) {
        ConvocatoriaDTO dto = new ConvocatoriaDTO();

        // Título: usar descripcion (campo principal de la BDNS)
        dto.setTitulo(getString(c, "descripcion",
                      getString(c, "descripcionLeng", "Sin título")));

        // Tipo: derivar del ámbito (nivel1)
        String nivel1 = getString(c, "nivel1", "");
        dto.setTipo(mapearTipo(nivel1));

        // Sector: la API BDNS no devuelve sector directamente
        dto.setSector(null);

        // Ubicación: nivel2 contiene la comunidad/organismo
        dto.setUbicacion(mapearUbicacion(nivel1, getString(c, "nivel2", null)));

        // Fuente
        String organismo = getString(c, "nivel3",
                           getString(c, "nivel2", "BDNS"));
        dto.setFuente("BDNS – " + organismo);

        // ID interno BDNS — necesario para obtener el detalle completo
        String idBdns = getString(c, "id", null);
        String numConv = getString(c, "numeroConvocatoria", null);
        if (idBdns != null) {
            dto.setIdBdns(idBdns);
        }
        if (numConv != null && !numConv.isBlank()) {
            dto.setNumeroConvocatoria(numConv);
            // La SPA Angular del portal usa el numeroConvocatoria en la URL de la ficha
            dto.setUrlOficial("https://www.infosubvenciones.es/bdnstrans/GE/es/convocatoria/" + numConv);
        } else if (idBdns != null) {
            // Fallback: buscar por ID interno (puede dar "Error al obtener datos" en la SPA)
            dto.setUrlOficial("https://www.infosubvenciones.es/bdnstrans/GE/es/convocatoria/" + idBdns);
        }

        // Fecha de cierre: intentar campos reales de plazo (fechaRecepcion es la de REGISTRO, no cierre)
        String fechaCierre = getString(c, "fechaFinSolicitud",
                             getString(c, "fechaCierre",
                             getString(c, "plazoSolicitudes", null)));
        if (fechaCierre != null) {
            parsearFecha(dto, fechaCierre);
        }
        // Si no hay fecha de cierre conocida, dejar null (convocatoria sin plazo definido = abierta)

        log.debug("BDNS conv: titulo='{}' fechaCierre={} nivel1={} idBdns={} numConv={}",
                dto.getTitulo(), dto.getFechaCierre(), nivel1, idBdns, numConv);
        return dto;
    }

    private String mapearTipo(String nivel1) {
        return switch (nivel1.toUpperCase()) {
            case "ESTADO" -> "Estatal";
            case "AUTONOMICA" -> "Autonómica";
            case "LOCAL" -> "Local";
            case "OTROS" -> "Otros organismos";
            default -> "Subvención";
        };
    }

    private String mapearUbicacion(String nivel1, String nivel2) {
        if ("ESTADO".equalsIgnoreCase(nivel1)) return "Nacional";
        if (nivel2 != null && !nivel2.isBlank()) return nivel2;
        return "Nacional";
    }

    private void parsearFecha(ConvocatoriaDTO dto, String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return;
        try {
            // Formato yyyy-MM-dd (lo que devuelve la API)
            dto.setFechaCierre(LocalDate.parse(fechaStr.substring(0, 10)));
        } catch (Exception e) {
            log.debug("BDNS: no se pudo parsear fecha: {}", fechaStr);
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        String s = val.toString().trim();
        return s.isBlank() ? defaultVal : s;
    }

    // ── SSL permisivo para el certificado del gobierno ───────────────────────

    private SimpleClientHttpRequestFactory createSslPermissiveFactory() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String t) {}
                    public void checkServerTrusted(X509Certificate[] c, String t) {}
                }
            };

            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, trustAll, new java.security.SecureRandom());

            return new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                        throws java.io.IOException {
                    if (connection instanceof HttpsURLConnection httpsConn) {
                        httpsConn.setSSLSocketFactory(sslCtx.getSocketFactory());
                        httpsConn.setHostnameVerifier((h, s) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
        } catch (Exception e) {
            log.error("Error configurando SSL para BDNS: {}", e.getMessage());
            return new SimpleClientHttpRequestFactory();
        }
    }

    // ── Excepción propia ─────────────────────────────────────────────────────

    public static class BdnsException extends RuntimeException {
        public BdnsException(String message) { super(message); }
        public BdnsException(String message, Throwable cause) { super(message, cause); }
    }
}

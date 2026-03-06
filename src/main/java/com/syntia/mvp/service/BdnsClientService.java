package com.syntia.mvp.service;

import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de integración con la API pública de la Base de Datos Nacional de Subvenciones (BDNS).
 * <p>
 * Endpoint base: https://www.infosubvenciones.es/bdnstrans/GE/es/api
 * <p>
 * Si {@code bdns.mock=true} en application.properties, devuelve datos de ejemplo
 * para desarrollo sin necesidad de conectividad real a la API de BDNS.
 * <p>
 * Documentación oficial de la API:
 * https://www.pap.hacienda.gob.es/bdnstrans/GE/es/convocatorias
 */
@Slf4j
@Service
public class BdnsClientService {

    private static final String BDNS_BASE_URL = "https://www.infosubvenciones.es/bdnstrans/GE/es/api";
    private static final DateTimeFormatter BDNS_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient restClient;

    @Value("${bdns.mock:false}")
    private boolean mockMode;

    public BdnsClientService() {
        this.restClient = RestClient.builder()
                .baseUrl(BDNS_BASE_URL)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Importa convocatorias de la API pública de BDNS.
     * <p>
     * Parámetros de la API BDNS:
     * - {@code page}     número de página (0-indexed)
     * - {@code pageSize} registros por página (max 50 recomendado)
     * - {@code estado}   "A" = abiertas (activas)
     *
     * @param pagina  número de página (0-indexed)
     * @param tamano  número de registros por página (1-50)
     * @return lista de ConvocatoriaDTO mapeados desde la respuesta BDNS
     * @throws BdnsException si la API no está disponible o devuelve un error
     */
    public List<ConvocatoriaDTO> importar(int pagina, int tamano) {
        if (mockMode) {
            log.info("BDNS mock activo — devolviendo datos de ejemplo");
            return generarDatosMock();
        }

        try {
            log.info("Consultando BDNS pagina={} tamano={}", pagina, tamano);

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/convocatorias")
                            .queryParam("page",     pagina)
                            .queryParam("pageSize", Math.min(tamano, 50))
                            .queryParam("estado",   "A")
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (respuesta == null) {
                throw new BdnsException("La API de BDNS devolvió una respuesta vacía");
            }

            return mapearRespuesta(respuesta);

        } catch (RestClientException e) {
            log.error("Error conectando con BDNS: {}", e.getMessage());
            throw new BdnsException("No se pudo conectar con la API de BDNS: " + e.getMessage(), e);
        }
    }

    // ── Mapeo de la respuesta ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<ConvocatoriaDTO> mapearRespuesta(Map<String, Object> respuesta) {
        List<ConvocatoriaDTO> resultado = new ArrayList<>();

        Object contenido = respuesta.get("content");
        if (!(contenido instanceof List<?> convocatorias)) {
            log.warn("BDNS: respuesta sin campo 'content' o no es lista. Keys: {}", respuesta.keySet());
            return resultado;
        }

        for (Object item : convocatorias) {
            if (!(item instanceof Map<?, ?> conv)) continue;
            try {
                resultado.add(mapearConvocatoria((Map<String, Object>) conv));
            } catch (Exception e) {
                log.warn("BDNS: error mapeando item, se omite. Error: {}", e.getMessage());
            }
        }

        log.info("BDNS: {} convocatorias mapeadas de {} en respuesta", resultado.size(), convocatorias.size());
        return resultado;
    }

    private ConvocatoriaDTO mapearConvocatoria(Map<String, Object> c) {
        ConvocatoriaDTO dto = new ConvocatoriaDTO();

        dto.setTitulo(getString(c, "descripcion", getString(c, "titulo", "Sin título")));
        dto.setTipo(getString(c, "tipoConvocatoria", "Subvención"));
        dto.setSector(getString(c, "sectorActividad", null));
        dto.setUbicacion(getString(c, "ambitoGeografico", "Nacional"));
        dto.setFuente("BDNS");

        // URL oficial: preferir enlace directo a la ficha BDNS
        String idBdns = getString(c, "idConvocatoria", null);
        if (idBdns != null) {
            dto.setUrlOficial("https://www.infosubvenciones.es/bdnstrans/GE/es/convocatoria/" + idBdns);
        } else {
            dto.setUrlOficial(getString(c, "urlBases", null));
        }

        // Fecha de cierre
        String fechaStr = getString(c, "fechaFinSolicitudes", null);
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                dto.setFechaCierre(LocalDate.parse(fechaStr, BDNS_DATE_FMT));
            } catch (Exception e) {
                log.debug("BDNS: no se pudo parsear fechaFinSolicitudes: {}", fechaStr);
            }
        }

        return dto;
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        String s = val.toString().trim();
        return s.isBlank() ? defaultVal : s;
    }

    // ── Datos de mock para desarrollo ────────────────────────────────────────

    private List<ConvocatoriaDTO> generarDatosMock() {
        List<ConvocatoriaDTO> mock = new ArrayList<>();

        String[][] datos = {
            {"Kit Digital – Pequeñas empresas 2026",     "Subvención", "Tecnología",     "Nacional",  "https://www.acelerapyme.gob.es/kit-digital",        "BDNS"},
            {"Programa NEOTEC – Startups tecnológicas",  "Subvención", "Tecnología",     "Nacional",  "https://www.cdti.es/index.asp?idpagina=4236",        "BDNS"},
            {"Ayudas FEADER – Modernización agraria",    "Ayuda",      "Agricultura",    "Nacional",  "https://www.mapa.gob.es/es/desarrollo-rural/temas/", "BDNS"},
            {"Horizonte Europa – Salud global 2026",     "Europeo",    "Salud",          "Europeo",   "https://research-and-innovation.ec.europa.eu/",      "BDNS"},
            {"Plan Impulsa – Industria valenciana",      "Subvención", "Industria",      "Valencia",  "https://www.ivace.es/",                              "BDNS"},
            {"Ayudas CDTI I+D – Proyectos individuales","Ayuda",      "Tecnología",     "Nacional",  "https://www.cdti.es/",                               "BDNS"},
            {"Programa Emprendimiento Cultural",         "Subvención", "Cultura",        "Nacional",  "https://www.culturaydeporte.gob.es/",                "BDNS"},
            {"Green Deal – Transición ecológica PYMES",  "Europeo",    "Medio ambiente", "Europeo",   "https://ec.europa.eu/clima/",                        "BDNS"},
        };

        for (String[] d : datos) {
            ConvocatoriaDTO dto = new ConvocatoriaDTO();
            dto.setTitulo(d[0]);
            dto.setTipo(d[1]);
            dto.setSector(d[2]);
            dto.setUbicacion(d[3]);
            dto.setUrlOficial(d[4]);
            dto.setFuente(d[5]);
            dto.setFechaCierre(LocalDate.now().plusMonths(3));
            mock.add(dto);
        }

        return mock;
    }

    // ── Excepción propia ─────────────────────────────────────────────────────

    public static class BdnsException extends RuntimeException {
        public BdnsException(String message) { super(message); }
        public BdnsException(String message, Throwable cause) { super(message, cause); }
    }
}


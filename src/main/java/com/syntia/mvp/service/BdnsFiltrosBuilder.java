package com.syntia.mvp.service;

import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.dto.FiltrosBdns;

/**
 * Clase utilitaria que construye {@link FiltrosBdns} a partir de los datos
 * estructurados de un {@link Proyecto} y un {@link Perfil}.
 * <p>
 * Reemplaza la generación de keywords por IA: en lugar de pedirle a OpenAI
 * que invente términos de búsqueda, extrae los filtros directamente de los campos
 * del modelo de datos, normalizándolos con {@link SectorNormalizador} y
 * {@link UbicacionNormalizador}.
 * <p>
 * Esto elimina la dependencia de OpenAI en la fase de búsqueda, reduce latencia
 * y garantiza que la búsqueda funcione incluso sin API key configurada.
 *
 * @see FiltrosBdns
 * @see SectorNormalizador
 * @see UbicacionNormalizador
 */
public final class BdnsFiltrosBuilder {

    private BdnsFiltrosBuilder() {
        // Clase utilitaria — no instanciable
    }

    /**
     * Construye los filtros BDNS a partir de proyecto y perfil del usuario.
     * <p>
     * Prioridad para cada campo:
     * <ul>
     *   <li><b>descripcion:</b> sector del proyecto → sector del perfil → nombre del proyecto → fallback genérico</li>
     *   <li><b>nivel2 (CCAA):</b> ubicación del proyecto → ubicación del perfil → null (sin filtro)</li>
     *   <li><b>nivel1:</b> "AUTONOMICA" si nivel2 tiene valor, null en caso contrario (la búsqueda
     *       en {@code BdnsClientService.buscarPorFiltros} hará doble búsqueda ESTADO+AUTONOMICA)</li>
     * </ul>
     *
     * @param proyecto proyecto del usuario (nunca null)
     * @param perfil   perfil de la entidad (puede ser null)
     * @return filtros estructurados para búsqueda BDNS
     */
    public static FiltrosBdns construir(Proyecto proyecto, Perfil perfil) {
        // ── Descripción (término de búsqueda) ──
        String descripcion = resolverDescripcion(proyecto, perfil);

        // ── Ubicación → CCAA normalizada ──
        String ubicacionRaw = resolverUbicacion(proyecto, perfil);
        String ccaa = UbicacionNormalizador.normalizarACcaa(ubicacionRaw);

        // nivel1 se determina en BdnsClientService.buscarPorFiltros:
        // si ccaa != null → hace doble búsqueda (ESTADO + AUTONOMICA)
        // si ccaa == null → búsqueda sin filtro territorial
        return new FiltrosBdns(descripcion, null, ccaa);
    }

    /**
     * Resuelve el término de búsqueda a partir del sector del proyecto/perfil.
     * Prioridad: sector proyecto → sector perfil → nombre proyecto → fallback genérico.
     */
    private static String resolverDescripcion(Proyecto proyecto, Perfil perfil) {
        // 1. Sector del proyecto
        String sector = proyecto.getSector();
        if (sector != null && !sector.isBlank()) {
            String normalizado = SectorNormalizador.normalizarABusqueda(sector);
            if (normalizado != null) return normalizado;
        }

        // 2. Sector del perfil (fallback)
        if (perfil != null && perfil.getSector() != null && !perfil.getSector().isBlank()) {
            String normalizado = SectorNormalizador.normalizarABusqueda(perfil.getSector());
            if (normalizado != null) return normalizado;
        }

        // 3. Nombre del proyecto como último recurso
        if (proyecto.getNombre() != null && !proyecto.getNombre().isBlank()) {
            String nombre = proyecto.getNombre().toLowerCase().trim();
            if (nombre.length() > 50) nombre = nombre.substring(0, 50);
            return "subvencion " + nombre;
        }

        // 4. Fallback genérico
        return "subvencion pyme empresa";
    }

    /**
     * Resuelve la ubicación priorizando proyecto sobre perfil.
     */
    private static String resolverUbicacion(Proyecto proyecto, Perfil perfil) {
        if (proyecto.getUbicacion() != null && !proyecto.getUbicacion().isBlank()) {
            return proyecto.getUbicacion();
        }
        if (perfil != null && perfil.getUbicacion() != null && !perfil.getUbicacion().isBlank()) {
            return perfil.getUbicacion();
        }
        return null;
    }
}


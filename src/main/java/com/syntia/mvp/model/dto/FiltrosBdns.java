package com.syntia.mvp.model.dto;

/**
 * Record inmutable que encapsula los filtros estructurados para búsqueda en la API BDNS.
 * <p>
 * Estos valores se usan directamente como parámetros de la URL de búsqueda BDNS:
 * <ul>
 *   <li>{@code descripcion} → parámetro {@code descripcion} (texto libre de búsqueda)</li>
 *   <li>{@code nivel1} → parámetro {@code nivel1} ({@code "ESTADO"} o {@code "AUTONOMICA"})</li>
 *   <li>{@code nivel2} → parámetro {@code nivel2} (nombre de CCAA, solo si nivel1=AUTONOMICA)</li>
 * </ul>
 * Campos null indican "no filtrar por ese criterio".
 *
 * @param descripcion término de búsqueda basado en sector/proyecto (null = sin filtro de texto)
 * @param nivel1      ámbito territorial: "ESTADO", "AUTONOMICA", o null (sin filtro)
 * @param nivel2      CCAA oficial para nivel1=AUTONOMICA (ej: "Comunidad Valenciana"), o null
 *
 * @see com.syntia.mvp.service.BdnsFiltrosBuilder
 * @see com.syntia.mvp.service.BdnsClientService#buscarPorFiltros(FiltrosBdns)
 */
public record FiltrosBdns(
        String descripcion,
        String nivel1,
        String nivel2
) {
    /**
     * Indica si este filtro tiene al menos un criterio definido.
     *
     * @return true si hay al menos un campo no null
     */
    public boolean tieneAlgunFiltro() {
        return descripcion != null || nivel1 != null || nivel2 != null;
    }

    /**
     * Indica si el filtro tiene restricción geográfica (nivel1 y/o nivel2 definidos).
     *
     * @return true si hay filtro territorial
     */
    public boolean tieneFiltrTerritorial() {
        return nivel1 != null;
    }

    /**
     * Crea una versión relajada del filtro eliminando la restricción de descripción.
     * Útil para el fallback progresivo cuando la búsqueda con texto devuelve pocos resultados.
     *
     * @return nuevo FiltrosBdns sin descripción pero manteniendo filtros territoriales
     */
    public FiltrosBdns sinDescripcion() {
        return new FiltrosBdns(null, nivel1, nivel2);
    }

    /**
     * Crea una versión relajada del filtro eliminando la restricción territorial.
     * Útil para el fallback progresivo cuando la búsqueda autonómica devuelve pocos resultados.
     *
     * @return nuevo FiltrosBdns sin nivel1/nivel2 pero manteniendo descripción
     */
    public FiltrosBdns sinTerritorio() {
        return new FiltrosBdns(descripcion, null, null);
    }
}


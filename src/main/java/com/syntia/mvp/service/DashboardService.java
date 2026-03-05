package com.syntia.mvp.service;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de datos para el dashboard interactivo del usuario.
 * <p>
 * Decisión arquitectónica: centralizar aquí la lógica de agregación
 * (top recomendaciones por proyecto, roadmap) mantiene el controller limpio
 * y el servicio testeable de forma independiente.
 */
@Service
public class DashboardService {

    private final ProyectoService proyectoService;
    private final RecomendacionService recomendacionService;

    public DashboardService(ProyectoService proyectoService,
                            RecomendacionService recomendacionService) {
        this.proyectoService = proyectoService;
        this.recomendacionService = recomendacionService;
    }

    /**
     * Construye el mapa proyecto → top N recomendaciones para mostrar en el dashboard.
     *
     * @param usuarioId ID del usuario autenticado
     * @param topN      número máximo de recomendaciones por proyecto
     * @return mapa ordenado: proyecto → lista de hasta topN recomendaciones
     */
    public Map<Proyecto, List<RecomendacionDTO>> obtenerTopRecomendacionesPorProyecto(
            Long usuarioId, int topN) {

        List<Proyecto> proyectos = proyectoService.obtenerProyectos(usuarioId);
        Map<Proyecto, List<RecomendacionDTO>> resultado = new LinkedHashMap<>();

        for (Proyecto proyecto : proyectos) {
            List<RecomendacionDTO> recs = recomendacionService
                    .obtenerPorProyecto(proyecto.getId());
            // Limitar al top N
            List<RecomendacionDTO> top = recs.stream()
                    .limit(topN)
                    .toList();
            resultado.put(proyecto, top);
        }
        return resultado;
    }

    /**
     * Construye el roadmap estratégico: todas las recomendaciones de todos los proyectos
     * del usuario que tienen fecha de cierre, ordenadas por fecha ascendente (más próximas primero).
     * Solo incluye convocatorias con fecha de cierre igual o posterior a hoy.
     *
     * @param usuarioId ID del usuario autenticado
     * @return lista de RoadmapItemDTO ordenada por fecha de cierre
     */
    public List<RoadmapItem> obtenerRoadmap(Long usuarioId) {
        List<Proyecto> proyectos = proyectoService.obtenerProyectos(usuarioId);
        List<RoadmapItem> roadmap = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Proyecto proyecto : proyectos) {
            List<RecomendacionDTO> recs = recomendacionService.obtenerPorProyecto(proyecto.getId());
            for (RecomendacionDTO rec : recs) {
                if (rec.getFechaCierre() != null && !rec.getFechaCierre().isBefore(hoy)) {
                    roadmap.add(new RoadmapItem(proyecto, rec));
                }
            }
        }

        // Ordenar por fecha de cierre ascendente (más urgente primero)
        roadmap.sort(Comparator.comparing(item -> item.recomendacion().getFechaCierre()));
        return roadmap;
    }

    /**
     * Cuenta el total de recomendaciones generadas para todos los proyectos del usuario.
     */
    public long contarTotalRecomendaciones(Long usuarioId) {
        return proyectoService.obtenerProyectos(usuarioId).stream()
                .mapToLong(p -> recomendacionService.contarPorProyecto(p.getId()))
                .sum();
    }

    /**
     * Registro interno que agrupa un proyecto con una recomendación para el roadmap.
     * Se usa record de Java 16+ para inmutabilidad y concisión.
     */
    public record RoadmapItem(Proyecto proyecto, RecomendacionDTO recomendacion) {}
}


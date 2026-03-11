package com.syntia.mvp.service;

import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.dto.FiltrosBdns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para las clases de la FASE 2 del pipeline BDNS-First:
 * {@link SectorNormalizador}, {@link BdnsFiltrosBuilder} y {@link FiltrosBdns}.
 * <p>
 * No requieren contexto Spring — son clases utilitarias puras.
 */
class BdnsFirstPipelineTest {

    // ═══════════════════════════════════════════════════════════════
    // SectorNormalizador
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SectorNormalizador")
    class SectorNormalizadorTest {

        @Test
        @DisplayName("Sector conocido devuelve término BDNS mapeado")
        void sectorConocido() {
            String resultado = SectorNormalizador.normalizarABusqueda("tecnología");
            assertNotNull(resultado);
            assertTrue(resultado.contains("tecnologia"));
            assertTrue(resultado.contains("subvencion"));
        }

        @Test
        @DisplayName("Sector con variante ortográfica se reconoce")
        void sectorVarianteOrtografica() {
            assertEquals(
                    SectorNormalizador.normalizarABusqueda("digitalizacion"),
                    SectorNormalizador.normalizarABusqueda("digitalización")
            );
        }

        @Test
        @DisplayName("Sector desconocido genera fallback con texto original")
        void sectorDesconocido() {
            String resultado = SectorNormalizador.normalizarABusqueda("nanotecnología espacial");
            assertNotNull(resultado);
            assertTrue(resultado.startsWith("subvencion "));
            assertTrue(resultado.contains("nanotecnolog"));
        }

        @Test
        @DisplayName("Sector null devuelve null")
        void sectorNull() {
            assertNull(SectorNormalizador.normalizarABusqueda(null));
        }

        @Test
        @DisplayName("Sector blank devuelve null")
        void sectorBlank() {
            assertNull(SectorNormalizador.normalizarABusqueda("   "));
        }

        @Test
        @DisplayName("esSectorReconocido funciona correctamente")
        void esSectorReconocido() {
            assertTrue(SectorNormalizador.esSectorReconocido("agricultura"));
            assertTrue(SectorNormalizador.esSectorReconocido("TURISMO"));
            assertFalse(SectorNormalizador.esSectorReconocido("sector inventado xyz"));
            assertFalse(SectorNormalizador.esSectorReconocido(null));
        }

        @Test
        @DisplayName("Sectores principales tienen mapeo")
        void sectoresPrincipales() {
            String[] sectores = {"tecnologia", "agricultura", "turismo", "energia", "salud",
                    "educacion", "comercio", "industria", "i+d+i", "emprendimiento"};
            for (String s : sectores) {
                assertTrue(SectorNormalizador.esSectorReconocido(s),
                        "Sector '" + s + "' debería estar reconocido");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FiltrosBdns
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FiltrosBdns")
    class FiltrosBdnsTest {

        @Test
        @DisplayName("Record con todos los campos")
        void todosLosCampos() {
            var f = new FiltrosBdns("subvencion pyme", "AUTONOMICA", "Comunidad Valenciana");
            assertEquals("subvencion pyme", f.descripcion());
            assertEquals("AUTONOMICA", f.nivel1());
            assertEquals("Comunidad Valenciana", f.nivel2());
            assertTrue(f.tieneAlgunFiltro());
        }

        @Test
        @DisplayName("Record vacío no tiene filtros")
        void sinFiltros() {
            var f = new FiltrosBdns(null, null, null);
            assertFalse(f.tieneAlgunFiltro());
        }

        @Test
        @DisplayName("sinDescripcion mantiene territorio")
        void sinDescripcion() {
            var f = new FiltrosBdns("subvencion pyme", "AUTONOMICA", "Madrid");
            var relajado = f.sinDescripcion();
            assertNull(relajado.descripcion());
            assertEquals("AUTONOMICA", relajado.nivel1());
            assertEquals("Madrid", relajado.nivel2());
        }

        @Test
        @DisplayName("sinTerritorio mantiene descripcion")
        void sinTerritorio() {
            var f = new FiltrosBdns("subvencion pyme", "AUTONOMICA", "Madrid");
            var relajado = f.sinTerritorio();
            assertEquals("subvencion pyme", relajado.descripcion());
            assertNull(relajado.nivel1());
            assertNull(relajado.nivel2());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BdnsFiltrosBuilder
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BdnsFiltrosBuilder")
    class BdnsFiltrosBuilderTest {

        @Test
        @DisplayName("Proyecto con sector y ubicación genera filtros completos")
        void proyectoCompleto() {
            Proyecto p = Proyecto.builder()
                    .nombre("Mi proyecto tech")
                    .sector("tecnología")
                    .ubicacion("Valencia")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, null);
            assertNotNull(f.descripcion());
            assertTrue(f.descripcion().contains("tecnologia"));
            assertEquals("Comunidad Valenciana", f.nivel2());
        }

        @Test
        @DisplayName("Proyecto sin ubicación usa perfil como fallback")
        void fallbackUbicacionPerfil() {
            Proyecto p = Proyecto.builder()
                    .nombre("Proyecto")
                    .sector("turismo")
                    .build();
            Perfil perfil = Perfil.builder()
                    .sector("hostelería")
                    .ubicacion("Catalunya")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, perfil);
            assertNotNull(f.descripcion());
            assertTrue(f.descripcion().contains("turismo"));
            assertEquals("Cataluña", f.nivel2());
        }

        @Test
        @DisplayName("Proyecto sin sector usa perfil.sector como fallback")
        void fallbackSectorPerfil() {
            Proyecto p = Proyecto.builder()
                    .nombre("Mi startup")
                    .ubicacion("Madrid")
                    .build();
            Perfil perfil = Perfil.builder()
                    .sector("i+d+i")
                    .ubicacion("Madrid")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, perfil);
            assertNotNull(f.descripcion());
            assertTrue(f.descripcion().contains("investigacion") || f.descripcion().contains("innovacion"));
            assertEquals("Madrid", f.nivel2());
        }

        @Test
        @DisplayName("Proyecto sin sector ni perfil usa nombre como fallback")
        void fallbackNombreProyecto() {
            Proyecto p = Proyecto.builder()
                    .nombre("Tienda online de productos artesanales")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, null);
            assertNotNull(f.descripcion());
            assertTrue(f.descripcion().startsWith("subvencion "));
            assertTrue(f.descripcion().contains("tienda"));
            assertNull(f.nivel2()); // Sin ubicación
        }

        @Test
        @DisplayName("Ubicación nacional devuelve ccaa null")
        void ubicacionNacional() {
            Proyecto p = Proyecto.builder()
                    .nombre("Proyecto nacional")
                    .sector("tecnología")
                    .ubicacion("España")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, null);
            assertNull(f.nivel2()); // "España" se normaliza a null
        }

        @Test
        @DisplayName("Perfil null no causa error")
        void perfilNull() {
            Proyecto p = Proyecto.builder()
                    .nombre("Proyecto básico")
                    .build();

            FiltrosBdns f = BdnsFiltrosBuilder.construir(p, null);
            assertNotNull(f);
            assertNotNull(f.descripcion());
        }
    }
}


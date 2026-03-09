# Registro de Cambios (Changelog): Syntia

Formato de cada entrada:
- **Fecha**
- **Versión** (MAJOR.MINOR.PATCH)
- **Cambios realizados**
- **Autor**

---

## [2.3.0] – 2026-03-09

### Mejora de Prompts IA + Optimizaciones de Velocidad

#### Archivos modificados
- `OpenAiMatchingService.java` — `SYSTEM_PROMPT` reescrito con criterios de puntuación explícitos por rango (90-100 / 70-89 / 50-69 / 30-49 / 0-29) y estructura de explicación en 2 partes (punto fuerte + condición a verificar). `KEYWORDS_SYSTEM_PROMPT` mejorado: ahora permite tildes en español (mejor cobertura BDNS), genera 4-6 búsquedas (antes 3-6). Parseo migrado de extracción manual de strings a Jackson `ObjectMapper` — más robusto ante JSON con caracteres especiales. Errata "espangla" corregida.
- `OpenAiClient.java` — añadido `response_format: {type: json_object}` (OpenAI devuelve JSON puro, sin preámbulos). Timeout 10s conexión / 30s lectura mediante `SimpleClientHttpRequestFactory`. Truncado de `userPrompt` a 1200 caracteres para reducir tokens de entrada. Import `JsonProperty` eliminado.
- `application.properties` — `openai.max-tokens` reducido de 400 a 150 (la respuesta JSON corta no necesita más). `openai.temperature` reducido de 0.3 a 0.1 (respuestas más deterministas y rápidas).

**Autor(es):** Daniel

---

## [2.2.0] – 2026-03-09

### Alineación contador recomendaciones (frontend)

#### Archivos modificados
- `RecomendacionController.java` — el mensaje flash de éxito ahora usa `contarPorProyecto()` (fuente de BD) en lugar de `generadas.size()` (fuente del motor), garantizando que el número del mensaje coincide exactamente con el de la vista.
- `templates/usuario/proyectos/recomendaciones.html` — cuando no hay filtros activos se muestra `"N recomendaciones encontradas"` en lugar del confuso `"Mostrando N de N"`. Con filtros activos sigue mostrando `"Mostrando X de Y"`.

**Autor(es):** Daniel

---

## [2.1.0] – 2026-03-09

### Filtrado de convocatorias caducadas

#### Archivos modificados
- `BdnsClientService.java` — añadido parámetro `&vigente=true` a la URL de búsqueda (`buscarPorTexto`) para que la API BDNS devuelva solo convocatorias con plazo abierto. Corregido el mapeo de fechas: antes se usaba `fechaRecepcion` (fecha de registro en BDNS, siempre pasada) como `fechaCierre`; ahora se buscan los campos reales `fechaFinSolicitud`, `fechaCierre` o `plazoSolicitudes`, dejando `null` si no están disponibles. Añadido log `DEBUG` por convocatoria mapeada.
- `MotorMatchingService.java` — filtro de seguridad en memoria en `buscarEnBdns()`: cualquier candidata con `fechaCierre` anterior a hoy se descarta aunque pase el filtro `vigente=true` de BDNS.

**Autor(es):** Daniel

---

## [2.0.0] – 2026-03-09

### Refactor completo del Motor de Matching

#### Cambios arquitectónicos
El motor se rediseñó completamente para eliminar la acumulación masiva de convocatorias en BD y los inserts infinitos que bloqueaban la interfaz.

**Flujo anterior (problemático):**
1. OpenAI genera keywords → `buscarEImportarDesdeBdns()` importa a BD → `findAll()` devuelve TODAS → evaluación de 150+ convocatorias con OpenAI → 150+ inserts

**Flujo nuevo (correcto):**
1. OpenAI genera keywords → búsqueda directa en API BDNS → deduplicación en memoria → evaluación con IA de top 20 → persistencia selectiva solo de las recomendadas (≥ 40 puntos)

#### Archivos modificados
- `MotorMatchingService.java` — reescrito completamente. Eliminada dependencia de `ConvocatoriaService`. Inyectado `BdnsClientService` directamente. Constantes: `UMBRAL_RECOMENDACION=40`, `RESULTADOS_POR_KEYWORD=20`, `MAX_CANDIDATAS_IA=20`. Nuevo método `buscarEnBdns()` con deduplicación por título. Nuevo método `persistirConvocatoria()`: solo guarda en BD las convocatorias que superan el umbral, usando `findByTituloIgnoreCaseAndFuente()` para evitar duplicados.
- `ConvocatoriaService.java` — eliminado `buscarEImportarDesdeBdns()` (ya no se usa). Eliminado import `ArrayList`.
- `ConvocatoriaRepository.java` — añadido `findByTituloIgnoreCaseAndFuente()` que devuelve `Optional<Convocatoria>`. Añadido import `java.util.Optional`. Añadido `buscarPorTitulos()` con `@Query`.

#### Bugs corregidos
- **Insert infinitos:** el motor ya no itera sobre todas las convocatorias de BD (podían ser 150+).
- **`findAll()` en motor de matching:** eliminado completamente, reemplazado por búsqueda directa en API BDNS.
- **Botón "Analizar con IA" bloqueado:** resuelto al limitar a MAX_CANDIDATAS_IA=20 las llamadas a OpenAI.

**Autor(es):** Daniel

---

## [1.9.0] – 2026-03-09

### Cambio de modelo OpenAI a gpt-4.1

- `application.properties` — `openai.model` cambiado de `gpt-4o-mini` a `gpt-4.1`.

**Autor(es):** Daniel

---

## [1.8.0] – 2026-03-06

### Fase 7: Deuda Técnica, Calidad y Producción

Resolución completa de los 8 faltantes detectados en la auditoría (v1.6.0).

#### Nuevos archivos creados
- `application-prod.properties` — perfil Spring para producción con todas las propiedades sensibles via variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY`, `PORT`).
- `templates/usuario/perfil-ver.html` — vista de solo lectura del perfil con DL semántico, badges y botón editar.
- `templates/fragments/navbar-usuario.html` — fragment navbar azul reutilizable para vistas de usuario.
- `templates/fragments/navbar-admin.html` — fragment navbar oscuro reutilizable para vistas de admin.
- `templates/fragments/footer.html` — fragment pie de página con copyright dinámico y enlace aviso legal.
- `templates/aviso-legal.html` — página pública de aviso legal (ruta `GET /aviso-legal`, sin autenticación).
- `service/BdnsClientService.java` — cliente REST para la API pública de BDNS con SSL permisivo para certificados gubernamentales.

#### Archivos modificados

**Backend:**
- `PerfilController` — añadida ruta `GET /usuario/perfil/ver` con redirect automático si no hay perfil.
- `AuthController` — añadida ruta `GET /aviso-legal` pública.
- `AdminController` — métricas del dashboard con `countAll()` directo (sin N+1); `detalleUsuario()` incluye `recsPerProyecto` (`Map<Long,Long>`).
- `ConvocatoriaService` — inyectado `BdnsClientService`; nuevo método `importarDesdeBdns(pagina, tamano)` con detección de duplicados.
- `AdminController` — nuevo endpoint `POST /admin/convocatorias/importar-bdns`.
- `RecomendacionService` — nuevos métodos `filtrar()`, `obtenerTiposDistintos()`, `obtenerSectoresDistintos()`.
- `RecomendacionController` — filtros delegados completamente a BD (eliminado filtrado en memoria).
- `SecurityConfig` — añadida `/aviso-legal` a rutas públicas.

**Repositorios:**
- `ProyectoRepository` — añadido `countAll()` con `@Query`.
- `RecomendacionRepository` — añadidos `countAll()`, `filtrar()` JPQL, `findTiposDistintosByProyectoId()`, `findSectoresDistintosByProyectoId()`.
- `ConvocatoriaRepository` — añadido `existsByTituloAndFuente()` para detección de duplicados BDNS.

**Autor(es):** Daniel (Fase 7 — Deuda técnica y producción)

---

## [1.7.0] – 2026-03-06

### Motor de Matching con OpenAI (Fase 3+)

#### Nuevos archivos creados
- `service/OpenAiClient.java` — cliente HTTP ligero para OpenAI Chat Completions API usando `RestClient` de Spring 6.
- `service/OpenAiMatchingService.java` — construcción del prompt con contexto completo (proyecto + perfil + convocatoria), llamada a OpenAI, parseo de respuesta JSON `{puntuacion, explicacion}`.

#### Archivos modificados
- `MotorMatchingService` — estrategia híbrida: OpenAI como motor primario, fallback automático al motor rule-based si la API falla o no está configurada.
- `Recomendacion` — campo `usadaIa` (boolean) para registrar en BD si fue generada por OpenAI o por reglas.
- `templates/usuario/proyectos/recomendaciones.html` — badge **🤖 Analizado por IA** vs **⚙️ Motor de reglas**.
- `application.properties` — añadidas propiedades `openai.api-key`, `openai.model`, `openai.max-tokens`, `openai.temperature`.

**Autor(es):** Daniel (Integración OpenAI)

---

## [1.6.0] – 2026-03-05

### Fase 6: API REST + JWT + Despliegue

#### Nuevos componentes
- `controller/api/AuthRestController.java` — `POST /api/auth/login` → devuelve JWT.
- `controller/api/PerfilRestController.java` — `GET/PUT /api/usuario/perfil`.
- `controller/api/ProyectoRestController.java` — CRUD `/api/usuario/proyectos`.
- `controller/api/RecomendacionRestController.java` — recomendaciones + generar.
- `model/dto/LoginRequestDTO.java`, `LoginResponseDTO.java`.
- `config/GlobalExceptionHandler.java` — añadidos `AccessDeniedException`, `MethodArgumentNotValidException`.

**Autor(es):** Daniel (Backend/API REST)

---

## [1.4.0] – 2026-03-05

### Fase 5: Panel Administrativo

#### Nuevos componentes
- `controller/AdminController.java` — CRUD usuarios + convocatorias + métricas.
- `service/ConvocatoriaService.java`, `model/dto/ConvocatoriaDTO.java`.
- `templates/admin/dashboard.html`, `usuarios/lista.html`, `usuarios/detalle.html`.
- `templates/admin/convocatorias/lista.html`, `formulario.html`.

**Autor(es):** Daniel

---

## [1.3.0] – 2026-03-05

### Fase 4: Dashboard Interactivo y Roadmap Estratégico

#### Nuevos componentes
- `service/DashboardService.java` — `obtenerTopRecomendacionesPorProyecto`, `obtenerRoadmap`, record `RoadmapItem`.
- `templates/usuario/dashboard.html` — métricas, top recomendaciones, roadmap, aviso legal.
- `static/javascript/dashboard.js` — contador de días restantes.

**Autor(es):** Daniel

---

## [1.2.0] – 2026-03-05

### Fase 3: Convocatorias, Motor de Matching y Recomendaciones

**Autor(es):** Daniel

---

## [1.1.0] – 2026-03-05

### Fase 2: Gestión de Proyectos (CRUD)

**Autor(es):** Daniel

---

## [1.0.0] – 2026-03-05

### Fase 1: Perfil de Usuario

**Autor(es):** Daniel

---

## [0.2.0] – 2026-03-05

### Auditoría Técnica y Actualización de Documentación (Pre-implementación)

Revisión exhaustiva del código base antes de iniciar la implementación. Corrección de imports, roles, dependencias Maven, configuración vacía y modelos de dominio ausentes.

**Autor(es):** Daniel

# Registro de Cambios (Changelog): Syntia

Formato de cada entrada:
- **Fecha**
- **Versión** (MAJOR.MINOR.PATCH)
- **Cambios realizados**
- **Autor**

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
- `service/BdnsClientService.java` — cliente REST para la API pública de BDNS con modo mock configurable (`bdns.mock=true`).

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

**Frontend:**
- 9 vistas (4 usuario + 5 admin) — navbars duplicados reemplazados con `th:replace` de fragments.
- 9 vistas — footer añadido con `th:replace`.
- `perfil.html` — botón "Ver perfil" añadido (visible solo si ya tiene perfil).
- `admin/usuarios/detalle.html` — tabla de proyectos incluye badge de nº recomendaciones por proyecto.
- `admin/convocatorias/lista.html` — sección "Importar desde BDNS" con formulario de paginación.
- `application.properties` — añadidas propiedades `bdns.mock`, `openai.model`, `openai.max-tokens`, `openai.temperature`.

**Documentación:**
- `04-manual-desarrollo.md` — nueva sección §8 con guía completa de despliegue en producción (variables de entorno, JAR, Railway/Render, nginx, HTTPS, checklist).
- `07-fases-implementacion.md` — completamente reescrito para reflejar el estado real: fases 1–7 documentadas, 8 faltantes resueltos en Fase 7.

**Autor(es):** Daniel (Fase 7 — Deuda técnica y producción)

---

## [1.7.0] – 2026-03-06


### Motor de Matching con OpenAI (Fase 3+)

#### Nuevos archivos creados
- `service/OpenAiClient.java` — cliente HTTP ligero para OpenAI Chat Completions API usando `RestClient` de Spring 6. Sin dependencias externas adicionales.
- `service/OpenAiMatchingService.java` — construcción del prompt con contexto completo (proyecto + perfil + convocatoria), llamada a OpenAI, parseo de respuesta JSON `{puntuacion, explicacion}`.

#### Archivos modificados
- `MotorMatchingService` — estrategia híbrida: OpenAI como motor primario, fallback automático al motor rule-based si la API falla o no está configurada.
- `Recomendacion` — campo `usadaIa` (boolean) para registrar en BD si fue generada por OpenAI o por reglas.
- `RecomendacionDTO` — expone `usadaIa` a las vistas.
- `RecomendacionService` — mapea `usadaIa` en `toDTO()`.
- `templates/usuario/proyectos/recomendaciones.html` — badge **🤖 Analizado por IA** vs **⚙️ Motor de reglas**; botón renombrado a "Analizar con IA".
- `application.properties` — añadidas propiedades `openai.api-key` (con fallback vacío), `openai.model`, `openai.max-tokens`, `openai.temperature`.

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
- `config/GlobalExceptionHandler.java` — añadidos `AccessDeniedException`, `MethodArgumentNotValidException`, método `build()` centralizado.
- `resources/data-test.sql` — 2 usuarios, 1 perfil, 8 convocatorias, 1 proyecto de prueba.
- `application.properties` — variables de entorno para producción documentadas.

**Autor(es):** Daniel (Backend/API REST)

---

## [1.4.0] – 2026-03-05

### Fase 5: Panel Administrativo

#### Nuevos componentes
- `controller/AdminController.java` — CRUD usuarios + convocatorias + métricas.
- `service/ConvocatoriaService.java` — `obtenerTodas`, `obtenerPorId`, `crear`, `actualizar`, `eliminar`, `toDTO`.
- `model/dto/ConvocatoriaDTO.java`.
- `service/UsuarioService.java` — añadido `cambiarRol(id, nuevoRol)`.
- `templates/admin/dashboard.html` — métricas y accesos rápidos.
- `templates/admin/usuarios/lista.html` — tabla con cambio de rol inline y modal eliminar.
- `templates/admin/usuarios/detalle.html` — detalle con proyectos del usuario.
- `templates/admin/convocatorias/lista.html` — tabla con editar/eliminar.
- `templates/admin/convocatorias/formulario.html` — formulario crear/editar.

**Autor(es):** Daniel (Backend/Frontend)

---

## [1.3.0] – 2026-03-05

### Fase 4: Dashboard Interactivo y Roadmap Estratégico

#### Nuevos componentes
- `service/DashboardService.java` — `obtenerTopRecomendacionesPorProyecto`, `obtenerRoadmap`, `contarTotalRecomendaciones`, record `RoadmapItem`.
- `controller/AuthController.java` — actualizado `userDashboard` con datos reales.
- `templates/usuario/dashboard.html` — métricas, top recomendaciones por proyecto, roadmap estratégico, aviso legal.
- `static/javascript/dashboard.js` — contador de días restantes en roadmap.

**Autor(es):** Daniel (Backend/Frontend)

---

## [1.2.0] – 2026-03-05

### Fase 3: Convocatorias, Motor de Matching y Recomendaciones

#### Nuevos componentes
- `service/MotorMatchingService.java` — scoring rule-based: sector(+40), ubicación(+30), nacional(+20), keywords(+10).
- `service/RecomendacionService.java` — lectura, conteo y conversión entidad→DTO.
- `controller/RecomendacionController.java` — `GET/{id}/recomendaciones` con filtros + `POST/generar`.
- `model/dto/RecomendacionDTO.java`.
- `repository/ConvocatoriaRepository.java` — añadidos `filtrar()`, `findSectoresDistintos()`, `findTiposDistintos()`.
- `repository/RecomendacionRepository.java` — añadidos `deleteByProyectoId`, `countByProyectoId`.
- `templates/usuario/proyectos/recomendaciones.html` — filtros, puntuación visual, barra de progreso, aviso legal.
- `templates/usuario/proyectos/detalle.html` — actualizado con botón "Ver recomendaciones".

**Autor(es):** Daniel (Backend/Frontend)

---

## [1.1.0] – 2026-03-05

### Fase 2: Gestión de Proyectos (CRUD)

#### Nuevos componentes
- `service/ProyectoService.java` — `obtenerProyectos`, `obtenerPorId`, `crear`, `actualizar`, `eliminar`, `toDTO`, `verificarPropiedad`.
- `controller/ProyectoController.java` — CRUD bajo `/usuario/proyectos`.
- `model/dto/ProyectoDTO.java`.
- `templates/usuario/proyectos/lista.html`, `formulario.html`, `detalle.html`.
- `static/javascript/proyecto.js` — validaciones y contador de caracteres.
- `templates/usuario/dashboard.html` — añadida tarjeta de acceso a proyectos.

**Autor(es):** Daniel (Backend/Frontend)

---

## [1.0.0] – 2026-03-05

### Fase 1: Perfil de Usuario

#### Nuevos componentes
- `service/PerfilService.java` — `tienePerfil`, `obtenerPerfil`, `crearPerfil`, `actualizarPerfil`, `toDTO`.
- `controller/PerfilController.java` — `GET/POST /usuario/perfil`.
- `model/dto/PerfilDTO.java`.
- `templates/usuario/perfil.html` — formulario crear/editar.
- `static/javascript/perfil.js` — validaciones frontend.

**Autor(es):** Daniel (Backend/Frontend)

---

## [0.2.0] – 2026-03-05

### Auditoría Técnica y Actualización de Documentación (Pre-implementación)

Se realiza una revisión exhaustiva del código base y la documentación antes de compartir el proyecto con el equipo e iniciar la implementación.

#### Hallazgos de la Auditoría

1. **Imports de otro proyecto:** `SecurityConfig.java`, `AuthController.java` y `CustomUserDetailsService.java` contenían imports de `es.fempa.acd.demosecurityproductos` en lugar de `com.syntia.mvp`.
2. **CORS incompatible:** `CorsConfig.java` usaba `addAllowedOrigin("*")` junto con `setAllowCredentials(true)`, combinación no permitida por Spring Security.
3. **Dependencia faltante:** `GlobalExceptionHandler.java` usaba `ResourceNotFoundException` de `spring-data-rest`, no incluida en el `pom.xml`.
4. **Código muerto:** `RestExceptionHandler.java` estaba completamente comentado con referencias al proyecto anterior.
5. **Roles incorrectos:** `AuthController.java` usaba `ROLE_CLIENTE` en vez de `ROLE_USUARIO`.
6. **Dependencias Maven faltantes:** Thymeleaf, jjwt (JWT), Bean Validation no estaban en `pom.xml`.
7. **Configuración vacía:** `application.properties` sin conexión a BD, sin JPA, sin Thymeleaf, sin JWT.
8. **Modelos de dominio ausentes:** Solo existía `ErrorResponse.java`, faltaban todas las entidades JPA.
9. **Repositorios vacíos:** Carpeta `repository/` sin archivos.

#### Correcciones y documentación actualizada

- `SecurityConfig.java`: imports corregidos, roles `ADMIN`/`USUARIO`, doble cadena de filtros (JWT para `/api/**` + formulario para vistas).
- `CorsConfig.java`: `addAllowedOrigin("*")` → `setAllowedOriginPatterns(List.of("*"))`.
- `GlobalExceptionHandler.java`: eliminada dependencia `spring-data-rest`, reescrito como `@RestControllerAdvice`.
- `RestExceptionHandler.java`: código muerto eliminado.
- `AuthController.java`: imports corregidos, `ROLE_CLIENTE` → `ROLE_USUARIO`, ruta `/cliente/` → `/usuario/`.
- `model/`: `Rol` (enum), `Usuario`, `Perfil`, `Proyecto`, `Convocatoria`, `Recomendacion`, `ErrorResponse`.
- `repository/`: `UsuarioRepository`, `PerfilRepository`, `ProyectoRepository`, `ConvocatoriaRepository`, `RecomendacionRepository`.
- `security/`: `JwtService`, `JwtAuthenticationFilter`.
- `service/UsuarioService.java`.
- `application.properties`: PostgreSQL (`syntia_db`), JPA, Thymeleaf, JWT.
- `pom.xml`: añadidas Thymeleaf, Thymeleaf Extras Security, Bean Validation, jjwt (0.12.6).
- `01-requisitos.md` → `06-diagramas.md`: documentación actualizada al estado real.

**Autor(es):** Daniel (Auditoría, infraestructura y documentación)

---

## [0.1.0] – 2026-03-04

### Estado inicial del repositorio (heredado)

Versiones iniciales de documentación (hitos 1–5) y esqueleto del proyecto entregado.  
Código con deuda técnica significativa (ver [0.2.0] para detalle de problemas detectados y resueltos).

**Autor(es):** Carlos (Backend/Frontend)



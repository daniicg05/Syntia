# Registro de Cambios (Changelog): Syntia

Formato de cada entrada:
- **Fecha**
- **Versión** (MAJOR.MINOR.PATCH)
- **Cambios realizados**
- **Autor**

---

## [1.6.0] – 2026-03-05

### Auditoría Técnica y Correcciones Urgentes

Auditoría completa del proyecto (fases 1–6). Correcciones de los problemas de prioridad alta detectados.

#### Correcciones aplicadas
- `ProyectoRestController`: cambiado para devolver `ProyectoDTO` en lugar de entidad JPA (evitar exposición de `password_hash`).
- `dashboard.js`: corregido selector `[th:text*]` inválido en runtime (Thymeleaf elimina atributos antes de servir el HTML).
- `AuthRestController`: `expiresIn` ahora lee `${jwt.expiration}` en lugar de valor hardcodeado.
- `templates/usuarios/lista.html`: eliminado artefacto residual del proyecto anterior con referencias a campos inexistentes.

#### Deuda técnica registrada (pendiente)
- Métricas admin con N+1 queries.
- Filtros de recomendaciones en memoria.
- Ausencia de `perfil-ver.html` (solo lectura).
- Falta perfil Spring `prod` y `application-prod.properties`.

**Autor(es):** Daniel (Auditoría y correcciones)

---

## [1.5.0] – 2026-03-05

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



# Plan de Implementación por Fases: Syntia

> Documento generado el 2026-03-05. Refleja el estado real del código y la documentación existente.  
> Repositorio: https://github.com/daniicg05/Syntia.git

---

## Resumen General del Proyecto

Syntia es una plataforma web que permite a usuarios (emprendedores, autónomos, PYMEs) recibir recomendaciones personalizadas sobre subvenciones, ayudas y licitaciones públicas mediante un motor de matching. La arquitectura es **monolítica modular** con Spring Boot, Thymeleaf, PostgreSQL y seguridad híbrida (sesión + JWT).

**Stack fijo:**
- Backend: Java 17, Spring Boot 3.5.x, Spring Security 6.x
- Seguridad: JWT (jjwt 0.12.6) + BCrypt + CORS
- Persistencia: Spring Data JPA + PostgreSQL 17.2 (`syntia_db`)
- Frontend: Thymeleaf + Bootstrap 5 + JavaScript vanilla
- Puerto: `8080` | BD usuario: `syntia` / pass: `syntia`

---

## Estado Actual (inicio de fases)

| Componente | Estado |
|------------|--------|
| Infraestructura base (pom.xml, application.properties) | ✅ Completo |
| Seguridad (SecurityConfig, CorsConfig, JWT) | ✅ Completo |
| Modelo de dominio (entidades JPA) | ✅ Completo |
| Repositorios JPA | ✅ Completo |
| Registro de usuario (`/registro`) | ✅ Completo |
| Login / Logout (`/login`, `/logout`) | ✅ Completo |
| Redirección por rol (`/default`) | ✅ Completo |
| Dashboard admin (esqueleto) | ✅ Esqueleto |
| Dashboard usuario (esqueleto) | ✅ Esqueleto |
| Perfil de usuario | ⏳ Pendiente |
| Gestión de proyectos | ⏳ Pendiente |
| Integración BDNS / Convocatorias | ⏳ Pendiente |
| Motor de matching | ⏳ Pendiente |
| Panel administrativo completo | ⏳ Pendiente |

---

## Inconsistencias Detectadas entre Documentos

1. **`03-especificaciones-tecnicas.md` § 6** lista `PerfilService`, `ProyectoService` y `MotorMatchingService` como pendientes, pero el código real ya tiene `UsuarioService` completo. Los demás servicios siguen pendientes. ✔ Consistente con el estado real.
2. **`06-diagramas.md`** muestra `UsuarioService.login()` devolviendo un `String` (token JWT). En el código actual el login lo gestiona Spring Security por formulario, no el servicio. Para la API REST se añadirá en Fase 2.
3. **`03-especificaciones-tecnicas.md`** menciona `UsuarioController` como pendiente en `controller/`, pero la gestión de usuarios del admin puede integrarse directamente en el controlador del panel admin. Se unificará en Fase 4.

---

## Fase 1 – Autenticación y Perfil de Usuario
> **Objetivo:** Usuario puede registrarse, iniciar sesión y completar/editar su perfil.  
> **Estado:** Registro y login ✅ ya implementados. Falta el perfil.

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 1.1 | Registro de usuario con validación (email + contraseña) | ✅ Hecho | — |
| 1.2 | Login por formulario con redirección por rol | ✅ Hecho | 1.1 |
| 1.3 | Logout con invalidación de sesión | ✅ Hecho | 1.2 |
| 1.4 | Formulario de creación de perfil (sector, ubicación, tipo entidad, objetivos, necesidades, descripción libre) | ⏳ | 1.2 |
| 1.5 | Edición de perfil existente | ⏳ | 1.4 |
| 1.6 | Vista de perfil (solo lectura) | ⏳ | 1.4 |

### Subtareas técnicas

**Backend:**
- [ ] `PerfilService.java` — métodos: `guardarPerfil(usuarioId, dto)`, `obtenerPerfil(usuarioId)`, `actualizarPerfil(usuarioId, dto)`
- [ ] `PerfilDTO.java` en `model/dto/` — campos validados con `@NotBlank`, `@Size`
- [ ] `PerfilController.java` en `controller/` — rutas: `GET /usuario/perfil`, `POST /usuario/perfil`, `GET /usuario/perfil/editar`, `POST /usuario/perfil/editar`

**Frontend (Thymeleaf):**
- [ ] `templates/usuario/perfil.html` — formulario de creación/edición
- [ ] `templates/usuario/perfil-ver.html` — vista de solo lectura
- [ ] Enlace al perfil desde `usuario/dashboard.html`
- [ ] `static/javascript/perfil.js` — validaciones frontend del formulario

**Seguridad:**
- [ ] Verificar que `/usuario/**` requiere `ROLE_USUARIO` (ya configurado en `SecurityConfig`)

### Testing mínimo
- Registro → login → acceso a `/usuario/perfil` devuelve 200
- POST perfil con datos válidos → redirect a perfil
- POST perfil con campos vacíos → errores de validación mostrados
- Editar perfil existente → datos actualizados en BD

---

## Fase 2 – Gestión de Proyectos
> **Objetivo:** El usuario puede crear y gestionar los proyectos sobre los que quiere buscar subvenciones.  
> **Dependencia:** Fase 1 completada (el proyecto se vincula al usuario y su perfil).

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 2.1 | Crear proyecto (nombre, sector, ubicación, descripción) | Alta | 1.2 |
| 2.2 | Listar proyectos del usuario | Alta | 2.1 |
| 2.3 | Ver detalle de un proyecto | Media | 2.1 |
| 2.4 | Editar proyecto | Media | 2.1 |
| 2.5 | Eliminar proyecto | Baja | 2.1 |

### Subtareas técnicas

**Backend:**
- [ ] `ProyectoService.java` — métodos: `crearProyecto(usuarioId, dto)`, `obtenerProyectos(usuarioId)`, `obtenerPorId(id)`, `actualizar(id, dto)`, `eliminar(id)`
- [ ] `ProyectoDTO.java` en `model/dto/` — campos validados
- [ ] `ProyectoController.java` — rutas: `GET /usuario/proyectos`, `GET /usuario/proyectos/nuevo`, `POST /usuario/proyectos`, `GET /usuario/proyectos/{id}`, `POST /usuario/proyectos/{id}/editar`, `POST /usuario/proyectos/{id}/eliminar`
- [ ] Verificar que el usuario solo accede a sus propios proyectos (comparar `proyecto.getUsuario().getId()` con el autenticado)

**Frontend (Thymeleaf):**
- [ ] `templates/usuario/proyectos/lista.html`
- [ ] `templates/usuario/proyectos/nuevo.html`
- [ ] `templates/usuario/proyectos/detalle.html`
- [ ] `templates/usuario/proyectos/editar.html`
- [ ] Enlace a proyectos desde `usuario/dashboard.html`

### Testing mínimo
- Crear proyecto con datos válidos → aparece en lista
- Intentar acceder a proyecto de otro usuario → 403
- Editar y eliminar proyecto propio → cambios reflejados

---

## Fase 3 – Convocatorias y Motor de Matching
> **Objetivo:** Cargar convocatorias (manual o vía BDNS) y generar recomendaciones priorizadas para cada proyecto.  
> **Dependencia:** Fase 2 completada.

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 3.1 | Carga manual de convocatorias (admin) | Alta | 4.1 |
| 3.2 | Integración básica con BDNS (recuperación por sector/tipo) | Alta | — |
| 3.3 | Motor de matching: cruce proyecto ↔ convocatorias por sector y ubicación | Alta | 2.1, 3.1 |
| 3.4 | Puntuación de compatibilidad (scoring por campos coincidentes) | Alta | 3.3 |
| 3.5 | Generación de explicación textual de cada recomendación | Media | 3.4 |
| 3.6 | Almacenamiento de recomendaciones en BD | Alta | 3.4 |
| 3.7 | Vista de recomendaciones del proyecto | Alta | 3.6 |
| 3.8 | Filtrado de recomendaciones (tipo, sector, ubicación) | Media | 3.7 |

### Subtareas técnicas

**Backend:**
- [ ] `ConvocatoriaService.java` — métodos: `recuperarConvocatorias()`, `filtrarPorProyecto(proyecto)`, `guardar(dto)`, `obtenerTodas()`
- [ ] `MotorMatchingService.java` — lógica de scoring:
  - Comparar `proyecto.sector` con `convocatoria.sector` (+puntos si coincide)
  - Comparar `proyecto.ubicacion` con `convocatoria.ubicacion` (+puntos)
  - Generar texto de explicación dinámico
- [ ] `ConvocatoriaDTO.java` y `RecomendacionDTO.java` en `model/dto/`
- [ ] `RecomendacionService.java` (o incluir en `MotorMatchingService`) — guardar y recuperar recomendaciones por proyecto
- [ ] Endpoint que dispara el matching: `POST /usuario/proyectos/{id}/recomendar`

**Frontend (Thymeleaf):**
- [ ] `templates/usuario/proyectos/recomendaciones.html` — lista priorizada con puntuación y explicación
- [ ] Filtros por tipo, sector y ubicación (formulario GET con parámetros)
- [ ] Enlace a URL oficial de cada convocatoria (`target="_blank"`)
- [ ] Aviso legal en la vista de recomendaciones

**Integración BDNS:**
- [ ] Llamada HTTP con `RestTemplate` o `WebClient` a la API pública de BDNS
- [ ] Mapeo de respuesta a entidad `Convocatoria`
- [ ] Tarea programada o botón manual para actualizar convocatorias

### Testing mínimo
- Proyecto con sector "tecnología" → recomendaciones del mismo sector aparecen primero
- Recomendaciones ordenadas por puntuación descendente
- Filtro por tipo → lista filtrada correctamente
- Enlace a URL oficial funciona

---

## Fase 4 – Dashboard Interactivo y Roadmap
> **Objetivo:** Dashboard del usuario con resumen visual de recomendaciones y roadmap de acciones.  
> **Dependencia:** Fase 3 completada.

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 4.1 | Dashboard usuario: resumen de proyectos y top recomendaciones | Alta | 3.6 |
| 4.2 | Roadmap estratégico: lista de convocatorias ordenadas por fecha de cierre | Alta | 3.6 |
| 4.3 | Indicador visual de puntuación (barra de progreso o badge) | Media | 3.4 |
| 4.4 | Filtrado rápido en dashboard (sector, tipo) | Media | 4.1 |
| 4.5 | Aviso legal visible en el dashboard | Alta | — |

### Subtareas técnicas

**Backend:**
- [ ] `DashboardController.java` (o ampliar `AuthController`) — carga datos para la vista: proyectos del usuario, top 5 recomendaciones por proyecto
- [ ] Consulta en `RecomendacionRepository` → `findByProyectoIdOrderByPuntuacionDesc`

**Frontend (Thymeleaf):**
- [ ] Mejorar `templates/usuario/dashboard.html`:
  - Tarjetas de proyectos con número de recomendaciones
  - Sección de roadmap con fechas de cierre próximas
  - Badges de puntuación (Bootstrap)
  - Aviso legal (componente `<div>` fijo o modal)
- [ ] `static/javascript/dashboard.js` — filtros dinámicos sin recarga si aplica
- [ ] Fragment reutilizable `templates/fragments/navbar.html` y `footer.html`

### Testing mínimo
- Dashboard carga correctamente con datos reales del usuario
- Roadmap muestra convocatorias ordenadas por fecha cierre
- Aviso legal visible

---

## Fase 5 – Panel Administrativo
> **Objetivo:** El administrador puede gestionar usuarios y supervisar el sistema.  
> **Dependencia:** Fase 1 completada (modelo Usuario ya existe).

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 5.1 | Listar todos los usuarios registrados | Alta | 1.1 |
| 5.2 | Ver detalle de un usuario | Media | 5.1 |
| 5.3 | Cambiar rol de un usuario (USUARIO ↔ ADMIN) | Media | 5.1 |
| 5.4 | Eliminar usuario | Baja | 5.1 |
| 5.5 | Listar todas las convocatorias cargadas | Alta | 3.1 |
| 5.6 | Añadir / editar / eliminar convocatoria manualmente | Alta | 5.5 |
| 5.7 | Ver métricas básicas: nº usuarios, nº proyectos, nº recomendaciones generadas | Media | 3.6 |

### Subtareas técnicas

**Backend:**
- [ ] `AdminController.java` en `controller/` — rutas bajo `/admin/`
- [ ] Reutilizar `UsuarioService` (métodos `obtenerTodos`, `buscarPorId`, `eliminar` ya existen)
- [ ] Añadir `UsuarioService.cambiarRol(id, nuevoRol)`
- [ ] Reutilizar `ConvocatoriaService` para CRUD de convocatorias
- [ ] Endpoint de métricas: contar registros de cada repositorio

**Frontend (Thymeleaf):**
- [ ] `templates/admin/dashboard.html` — métricas y accesos rápidos
- [ ] `templates/admin/usuarios/lista.html`
- [ ] `templates/admin/usuarios/detalle.html`
- [ ] `templates/admin/convocatorias/lista.html`
- [ ] `templates/admin/convocatorias/nueva.html` / `editar.html`

**Seguridad:**
- [ ] Todas las rutas `/admin/**` protegidas con `ROLE_ADMIN` (ya configurado)
- [ ] Verificar que ningún endpoint admin es accesible con `ROLE_USUARIO`

### Testing mínimo
- Usuario con rol ADMIN accede a `/admin/dashboard` → 200
- Usuario con rol USUARIO intenta acceder a `/admin/dashboard` → 403
- CRUD de convocatorias funciona correctamente

---

## Fase 6 – API REST y Despliegue
> **Objetivo:** Exponer la API REST con JWT, preparar el entorno de producción y documentar el despliegue.  
> **Dependencia:** Fases 1-5 completadas.

### Funcionalidades

| # | Funcionalidad | Prioridad | Dependencia |
|---|--------------|-----------|-------------|
| 6.1 | Endpoint `POST /api/auth/login` → devuelve JWT | Alta | 1.2 |
| 6.2 | Endpoint `GET /api/usuario/perfil` protegido con JWT | Media | 1.4 |
| 6.3 | Endpoint `GET /api/usuario/proyectos` protegido con JWT | Media | 2.1 |
| 6.4 | Endpoint `GET /api/usuario/proyectos/{id}/recomendaciones` | Media | 3.6 |
| 6.5 | Guía de despliegue (variables de entorno, HTTPS, puerto) | Alta | — |
| 6.6 | Script SQL de inicialización de datos de prueba | Alta | — |

### Subtareas técnicas

**Backend:**
- [ ] `AuthRestController.java` en `controller/api/` — `POST /api/auth/login` usando `AuthenticationManager` + `JwtService.generarToken()`
- [ ] `PerfilRestController.java`, `ProyectoRestController.java`, `RecomendacionRestController.java` en `controller/api/`
- [ ] Verificar que `GlobalExceptionHandler` (`@RestControllerAdvice`) cubre todos los endpoints REST
- [ ] `LoginRequestDTO.java` y `LoginResponseDTO.java` (email + token)

**Infraestructura:**
- [ ] `src/main/resources/data.sql` o script separado con usuarios de prueba (1 ADMIN, 1 USUARIO) con contraseñas BCrypt
- [ ] Variables de entorno para `jwt.secret`, datasource en producción
- [ ] Configurar HTTPS (certificado) en entorno de producción
- [ ] Actualizar `CorsConfig` con el dominio de producción real

**Documentación:**
- [ ] Actualizar `04-manual-desarrollo.md` con instrucciones de despliegue en producción
- [ ] Actualizar `05-changelog.md` con las versiones de cada fase completada

### Testing mínimo
- `POST /api/auth/login` con credenciales válidas → 200 + token JWT
- `POST /api/auth/login` con credenciales inválidas → 401
- `GET /api/usuario/perfil` sin token → 403
- `GET /api/usuario/perfil` con token válido → 200 + datos del perfil

---

## Orden de Ejecución Recomendado

```
Fase 1 (Perfil)
    └── Fase 2 (Proyectos)
            └── Fase 3 (Convocatorias + Matching)
                    └── Fase 4 (Dashboard + Roadmap)
Fase 1 (Perfil)
    └── Fase 5 (Panel Admin)        ← puede desarrollarse en paralelo con Fase 2-4
Fases 1-5
    └── Fase 6 (API REST + Despliegue)
```

## Registro de Progreso

| Fase | Descripción | Estado | Versión |
|------|-------------|--------|---------|
| Infraestructura | Setup base, BD, seguridad, modelos | ✅ Completo | 0.2.0 |
| Fase 1 | Perfil de usuario | ✅ Completo | 0.3.0 |
| Fase 2 | Gestión de proyectos | ⏳ Pendiente | — |
| Fase 3 | Convocatorias y matching | ⏳ Pendiente | — |
| Fase 4 | Dashboard y roadmap | ⏳ Pendiente | — |
| Fase 5 | Panel administrativo | ⏳ Pendiente | — |
| Fase 6 | API REST y despliegue | ⏳ Pendiente | — |


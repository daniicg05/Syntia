# Plan de Implementación por Fases: Syntia

> Documento actualizado el 2026-03-06. Refleja el estado real del código tras la auditoría técnica completa.  
> Repositorio: https://github.com/daniicg05/Syntia.git

---

## Resumen General del Proyecto

Syntia es una plataforma web que permite a usuarios (emprendedores, autónomos, PYMEs) recibir recomendaciones personalizadas sobre subvenciones, ayudas y licitaciones públicas mediante un motor de matching. La arquitectura es **monolítica modular** con Spring Boot, Thymeleaf, PostgreSQL y seguridad híbrida (sesión + JWT).

**Stack fijo:**
- Backend: Java 17, Spring Boot 3.5.x, Spring Security 6.x
- Seguridad: JWT (jjwt 0.12.6) + BCrypt + CORS
- Persistencia: Spring Data JPA + PostgreSQL 17.2 (`syntia_db`)
- Frontend: Thymeleaf + Bootstrap 5 + JavaScript vanilla
- **IA:** OpenAI Chat Completions API (gpt-4o-mini por defecto) con fallback rule-based automático
- Puerto: `8080` | BD usuario: `syntia` / pass: `syntia`

---

## Estado Actual (2026-03-06)

| Componente | Estado |
|------------|--------|
| Infraestructura base (pom.xml, application.properties) | ✅ Completo |
| Seguridad (SecurityConfig, CorsConfig, JWT) | ✅ Completo |
| Modelo de dominio (entidades JPA) | ✅ Completo |
| Repositorios JPA | ✅ Completo |
| Registro de usuario (`/registro`) | ✅ Completo |
| Login / Logout (`/login`, `/logout`) | ✅ Completo |
| Redirección por rol (`/default`) | ✅ Completo |
| Perfil de usuario (crear/editar) | ✅ Completo |
| Gestión de proyectos (CRUD) | ✅ Completo |
| Motor de matching (rule-based + **OpenAI**) | ✅ Completo |
| Convocatorias (carga manual desde admin) | ✅ Completo |
| Recomendaciones (generar, ver, filtrar) | ✅ Completo |
| Dashboard usuario (métricas, top recs, roadmap) | ✅ Completo |
| Panel administrativo completo | ✅ Completo |
| API REST con JWT | ✅ Completo |
| Datos de prueba (`data-test.sql`) | ✅ Completo |
| Integración OpenAI (análisis semántico) | ✅ Completo |
| Vista perfil solo lectura (`perfil-ver.html`) | ❌ Pendiente |
| Integración API BDNS | ❌ Pendiente |
| Perfil Spring producción (`application-prod.properties`) | ❌ Pendiente |
| Fragments Thymeleaf reutilizables | ❌ Pendiente |
| Optimización N+1 métricas admin | ❌ Pendiente |
| Filtros recomendaciones en BD (no en memoria) | ❌ Pendiente |
| Guía despliegue producción completa | ❌ Pendiente |
| Detalle usuario admin con proyectos y recomendaciones | ❌ Pendiente |

---

## Inconsistencias Detectadas entre Documentos

1. **`03-especificaciones-tecnicas.md` § 6** lista `PerfilService`, `ProyectoService` y `MotorMatchingService` como pendientes — **resuelto**, todos implementados.
2. **`06-diagramas.md`** muestra `UsuarioService.login()` devolviendo JWT — **resuelto**, implementado en `AuthRestController` vía `AuthenticationManager`.
3. **`03-especificaciones-tecnicas.md`** menciona `UsuarioController` como pendiente — **resuelto**, funcionalidad integrada en `AdminController`.
4. **Deuda técnica activa (registrada en changelog v1.6.0):** N+1 en admin, filtros en memoria, ausencia de `perfil-ver.html`, falta perfil Spring `prod`. Pendiente de resolución en Fase 7.

---

## Fase 1 – Autenticación y Perfil de Usuario
> **Estado: ✅ COMPLETADA (v1.0.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 1.1 | Registro de usuario con validación (email + contraseña) | ✅ |
| 1.2 | Login por formulario con redirección por rol | ✅ |
| 1.3 | Logout con invalidación de sesión | ✅ |
| 1.4 | Formulario de creación de perfil (sector, ubicación, tipo entidad, objetivos, necesidades, descripción libre) | ✅ |
| 1.5 | Edición de perfil existente | ✅ |
| 1.6 | Vista de perfil (solo lectura) | ❌ Pendiente → Fase 7.1 |

### Componentes implementados
- `PerfilService.java`, `PerfilController.java`, `PerfilDTO.java`
- `templates/usuario/perfil.html` (formulario crear/editar)
- `static/javascript/perfil.js`

---

## Fase 2 – Gestión de Proyectos
> **Estado: ✅ COMPLETADA (v1.1.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 2.1 | Crear proyecto (nombre, sector, ubicación, descripción) | ✅ |
| 2.2 | Listar proyectos del usuario | ✅ |
| 2.3 | Ver detalle de un proyecto | ✅ |
| 2.4 | Editar proyecto | ✅ |
| 2.5 | Eliminar proyecto | ✅ |

### Componentes implementados
- `ProyectoService.java`, `ProyectoController.java`, `ProyectoDTO.java`
- `templates/usuario/proyectos/lista.html`, `formulario.html`, `detalle.html`
- `static/javascript/proyecto.js`

---

## Fase 3 – Convocatorias y Motor de Matching
> **Estado: ✅ COMPLETADA (v1.2.0) + OpenAI añadido (v1.7.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 3.1 | Carga manual de convocatorias (admin) | ✅ |
| 3.2 | Integración con API BDNS | ❌ Pendiente → Fase 7.2 |
| 3.3 | Motor de matching: cruce proyecto ↔ convocatorias | ✅ |
| 3.4 | Puntuación de compatibilidad (scoring) | ✅ rule-based + **✅ OpenAI semántico** |
| 3.5 | Generación de explicación textual de cada recomendación | ✅ rule-based + **✅ OpenAI en lenguaje natural** |
| 3.6 | Almacenamiento de recomendaciones en BD | ✅ |
| 3.7 | Vista de recomendaciones del proyecto | ✅ |
| 3.8 | Filtrado de recomendaciones (tipo, sector, ubicación) | ✅ en memoria → optimizar en Fase 7.6 |

### Componentes implementados
- `MotorMatchingService.java` — estrategia híbrida: OpenAI primario + rule-based fallback
- `OpenAiClient.java` — cliente HTTP para OpenAI Chat Completions (sin dependencias externas)
- `OpenAiMatchingService.java` — construcción de prompt, parseo JSON, manejo de errores
- `RecomendacionService.java`, `RecomendacionController.java`, `RecomendacionDTO.java`
- `templates/usuario/proyectos/recomendaciones.html` — badges 🤖/⚙️, filtros, barras de puntuación
- Campo `usadaIa` en entidad `Recomendacion` y DTO

### Configuración OpenAI (`application.properties`)
```properties
openai.api-key=${OPENAI_API_KEY:}   # vacío = fallback automático a motor rule-based
openai.model=gpt-4o-mini
openai.max-tokens=400
openai.temperature=0.3
```

---

## Fase 4 – Dashboard Interactivo y Roadmap
> **Estado: ✅ COMPLETADA (v1.3.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 4.1 | Dashboard usuario: resumen de proyectos y top recomendaciones | ✅ |
| 4.2 | Roadmap estratégico: convocatorias ordenadas por fecha de cierre | ✅ |
| 4.3 | Indicador visual de puntuación (barra de progreso + badge) | ✅ |
| 4.4 | Filtrado rápido en dashboard | ✅ |
| 4.5 | Aviso legal visible en el dashboard | ✅ |

### Componentes implementados
- `DashboardService.java` — `obtenerTopRecomendacionesPorProyecto`, `obtenerRoadmap`, record `RoadmapItem`
- `templates/usuario/dashboard.html` — métricas, top recs, roadmap, aviso legal
- `static/javascript/dashboard.js` — contador días restantes

---

## Fase 5 – Panel Administrativo
> **Estado: ✅ COMPLETADA (v1.4.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 5.1 | Listar todos los usuarios registrados | ✅ |
| 5.2 | Ver detalle de un usuario | ✅ (verificar contenido completo → Fase 7.8) |
| 5.3 | Cambiar rol de un usuario (USUARIO ↔ ADMIN) | ✅ |
| 5.4 | Eliminar usuario | ✅ |
| 5.5 | Listar todas las convocatorias cargadas | ✅ |
| 5.6 | Añadir / editar / eliminar convocatoria manualmente | ✅ |
| 5.7 | Ver métricas básicas | ✅ (N+1 pendiente → Fase 7.5) |

### Componentes implementados
- `AdminController.java` — CRUD usuarios + convocatorias + métricas
- `templates/admin/dashboard.html`, `usuarios/lista.html`, `usuarios/detalle.html`
- `templates/admin/convocatorias/lista.html`, `formulario.html`

---

## Fase 6 – API REST y Despliegue
> **Estado: ✅ COMPLETADA (v1.5.0)**

### Funcionalidades

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 6.1 | `POST /api/auth/login` → devuelve JWT | ✅ |
| 6.2 | `GET /api/usuario/perfil` protegido con JWT | ✅ |
| 6.3 | `GET /api/usuario/proyectos` protegido con JWT | ✅ |
| 6.4 | `GET /api/usuario/proyectos/{id}/recomendaciones` | ✅ |
| 6.5 | Guía de despliegue (variables de entorno, HTTPS) | ⚠️ Parcial → completar en Fase 7.7 |
| 6.6 | Script SQL de datos de prueba | ✅ (`data-test.sql`) |

### Componentes implementados
- `controller/api/`: `AuthRestController`, `PerfilRestController`, `ProyectoRestController`, `RecomendacionRestController`
- `LoginRequestDTO.java`, `LoginResponseDTO.java`
- `GlobalExceptionHandler.java`, `RestExceptionHandler.java`
- `data-test.sql` — 2 usuarios, 1 perfil, 8 convocatorias, 1 proyecto

---

## Fase 7 – Deuda Técnica, Calidad y Producción
> **Objetivo:** Resolver la deuda técnica registrada, mejorar calidad del código y preparar el entorno de producción.  
> **Estado: ❌ PENDIENTE**  
> **Dependencia:** Fases 1–6 completadas ✅

### Funcionalidades

| # | Funcionalidad | Tipo | Prioridad |
|---|--------------|------|-----------|
| 7.1 | Vista de perfil en solo lectura (`perfil-ver.html`) | Frontend | Media |
| 7.2 | Integración con API pública de BDNS | Integración | **Alta** |
| 7.3 | Perfil Spring de producción (`application-prod.properties`) | Infraestructura | **Alta** |
| 7.4 | Fragments Thymeleaf reutilizables (navbar, footer, aviso legal) | Frontend | Media |
| 7.5 | Optimización N+1 en métricas del panel admin | Backend | Media |
| 7.6 | Filtros de recomendaciones delegados a BD (no en memoria) | Backend | Media |
| 7.7 | Guía de despliegue en producción completa (HTTPS, vars entorno) | Documentación | Media |
| 7.8 | Detalle de usuario admin con proyectos y recomendaciones | Frontend+Backend | Media |

---

### 7.1 – Vista de perfil en solo lectura

**Backend:**
- [ ] Añadir ruta `GET /usuario/perfil/ver` en `PerfilController` que carga el perfil existente en modo lectura (redirige a `/usuario/perfil` si no tiene perfil aún)

**Frontend:**
- [ ] `templates/usuario/perfil-ver.html` — muestra todos los campos del perfil sin inputs editables, con botón "Editar perfil" que lleva a `GET /usuario/perfil`
- [ ] Actualizar navbar de `usuario/dashboard.html` para incluir enlace "Ver perfil"

**Testing:**
- `GET /usuario/perfil/ver` con perfil existente → 200, datos visibles
- `GET /usuario/perfil/ver` sin perfil → redirect a `/usuario/perfil`

---

### 7.2 – Integración con API pública de BDNS

**Backend:**
- [ ] `BdnsClientService.java` — llamada HTTP con `RestClient` a `https://www.infosubvenciones.es/bdnstrans/GE/es/api` (endpoint de convocatorias)
- [ ] Mapeo de respuesta JSON de BDNS → entidad `Convocatoria` (campos: título, tipo, sector, fecha cierre, URL oficial)
- [ ] Método `importarDesodeBdns(String sector, int limite)` en `ConvocatoriaService`
- [ ] Endpoint admin: `POST /admin/convocatorias/importar-bdns` con parámetros `sector` y `limite`

**Frontend:**
- [ ] Añadir sección "Importar desde BDNS" en `templates/admin/convocatorias/lista.html`
- [ ] Formulario con selector de sector y botón "Importar" que hace POST al endpoint
- [ ] Mostrar resumen de convocatorias importadas (flash message)

**Testing:**
- `POST /admin/convocatorias/importar-bdns?sector=Tecnología&limite=10` → convocatorias importadas aparecen en lista
- Error de conexión a BDNS → mensaje de error claro, sin crashear la aplicación

---

### 7.3 – Perfil Spring de producción

**Infraestructura:**
- [ ] `src/main/resources/application-prod.properties` con todas las propiedades sensibles por variable de entorno:
  ```properties
  spring.datasource.url=${SPRING_DATASOURCE_URL}
  spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
  spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
  jwt.secret=${JWT_SECRET}
  jwt.expiration=${JWT_EXPIRATION:86400000}
  openai.api-key=${OPENAI_API_KEY}
  spring.jpa.show-sql=false
  logging.level.org.springframework.security=WARN
  logging.level.com.syntia.mvp=INFO
  ```
- [ ] Activar con `--spring.profiles.active=prod` o variable de entorno `SPRING_PROFILES_ACTIVE=prod`
- [ ] Añadir `application-prod.properties` al `.gitignore` (o asegurar que no contiene valores reales)

**Testing:**
- Arrancar con perfil `prod` → aplicación usa variables de entorno, no valores hardcodeados
- Sin las variables de entorno → error claro en arranque (fail-fast)

---

### 7.4 – Fragments Thymeleaf reutilizables

**Frontend:**
- [ ] `templates/fragments/navbar-usuario.html` — navbar de usuario (actualmente duplicado en cada vista)
- [ ] `templates/fragments/navbar-admin.html` — navbar de administrador
- [ ] `templates/fragments/footer.html` — pie de página con versión, aviso legal, enlaces
- [ ] `templates/fragments/aviso-legal.html` — bloque de aviso legal reutilizable
- [ ] Sustituir navbars y avisos duplicados en todas las vistas con `th:replace` / `th:insert`

**Impacto:** Reduce duplicación en ≈12 vistas. Cambio de navbar en un solo lugar.

---

### 7.5 – Optimización N+1 en métricas del admin

**Backend:**
- [ ] Añadir en `ProyectoRepository`:
  ```java
  @Query("SELECT COUNT(p) FROM Proyecto p")
  long countAll();
  ```
- [ ] Añadir en `RecomendacionRepository`:
  ```java
  @Query("SELECT COUNT(r) FROM Recomendacion r")
  long countAll();
  ```
- [ ] Refactorizar `AdminController.dashboard()`: sustituir los bucles con streams de usuarios por las queries directas `proyectoRepository.countAll()` y `recomendacionRepository.countAll()`

**Testing:**
- Dashboard admin carga en < 200ms con 1000 usuarios en BD
- Métricas muestran los valores correctos

---

### 7.6 – Filtros de recomendaciones delegados a BD

**Backend:**
- [ ] Añadir en `RecomendacionRepository` una query con filtros opcionales:
  ```java
  @Query("SELECT r FROM Recomendacion r JOIN r.convocatoria c " +
         "WHERE r.proyecto.id = :proyectoId " +
         "AND (:tipo IS NULL OR c.tipo = :tipo) " +
         "AND (:sector IS NULL OR c.sector = :sector) " +
         "AND (:ubicacion IS NULL OR c.ubicacion = :ubicacion) " +
         "ORDER BY r.puntuacion DESC")
  List<Recomendacion> filtrar(@Param("proyectoId") Long proyectoId,
                               @Param("tipo") String tipo,
                               @Param("sector") String sector,
                               @Param("ubicacion") String ubicacion);
  ```
- [ ] Refactorizar `RecomendacionService` para exponer `filtrar(proyectoId, tipo, sector, ubicacion)`
- [ ] Actualizar `RecomendacionController.verRecomendaciones()` para llamar al método del repositorio en lugar de filtrar en memoria

**Testing:**
- Filtro por tipo con 500 recomendaciones → tiempo de respuesta igual al sin filtro (query BD)
- Resultado idéntico al filtrado en memoria actual

---

### 7.7 – Guía de despliegue en producción completa

**Documentación:**
- [ ] Actualizar `04-manual-desarrollo.md` con sección "Despliegue en Producción":
  - Lista completa de variables de entorno requeridas
  - Instrucciones de arranque con perfil `prod`
  - Configuración HTTPS con certificado (Let's Encrypt + nginx reverse proxy)
  - Comandos de construcción del JAR (`mvn clean package -P prod`)
  - Instrucciones para ejecutar el JAR en servidor Linux (`systemd service`)
  - Checklist pre-despliegue (BD migrada, vars de entorno, SSL activo)

---

### 7.8 – Detalle de usuario admin con proyectos y recomendaciones

**Backend:**
- [ ] Verificar / completar `GET /admin/usuarios/{id}` en `AdminController`: debe cargar usuario + lista de proyectos + total recomendaciones por proyecto
- [ ] Si falta, añadir método en `AdminController`:
  ```java
  @GetMapping("/usuarios/{id}")
  public String detalleUsuario(@PathVariable Long id, Model model) { ... }
  ```

**Frontend:**
- [ ] Verificar / completar `templates/admin/usuarios/detalle.html`:
  - Datos del usuario (email, rol, fecha registro)
  - Tabla de proyectos del usuario (nombre, sector, nº recomendaciones)
  - Acceso rápido a cambio de rol y eliminar usuario

**Testing:**
- `GET /admin/usuarios/1` → 200 con datos del usuario, lista de proyectos y recomendaciones totales
- Usuario sin proyectos → mensaje "Sin proyectos"

---

## Orden de Ejecución Recomendado (Fase 7)

```
7.3 (application-prod.properties)   ← seguridad crítica, 30 min
    │
7.1 (perfil-ver.html)               ← UX sencilla, 1 hora
    │
7.8 (detalle admin verificar)       ← completitud panel, 1 hora
    │
7.4 (fragments Thymeleaf)           ← refactor transversal, 2 horas
    │
7.5 (N+1 admin) + 7.6 (filtros BD) ← calidad backend, 2 horas en paralelo
    │
7.7 (guía despliegue)               ← documentación final, 2 horas
    │
7.2 (integración BDNS)              ← feature principal, 4-8 horas
```

---

## Registro de Progreso

| Fase | Descripción | Estado | Versión |
|------|-------------|--------|---------|
| Infraestructura | Setup base, BD, seguridad, modelos | ✅ Completo | 0.2.0 |
| Fase 1 | Perfil de usuario (crear/editar) | ✅ Completo | 1.0.0 |
| Fase 2 | Gestión de proyectos (CRUD) | ✅ Completo | 1.1.0 |
| Fase 3 | Convocatorias + Motor de matching rule-based | ✅ Completo | 1.2.0 |
| Fase 4 | Dashboard interactivo + Roadmap estratégico | ✅ Completo | 1.3.0 |
| Fase 5 | Panel administrativo completo | ✅ Completo | 1.4.0 |
| Fase 6 | API REST con JWT + datos de prueba | ✅ Completo | 1.5.0 |
| Auditoría | Correcciones urgentes + deuda técnica registrada | ✅ Completo | 1.6.0 |
| Fase 3+ | Integración OpenAI (motor semántico con fallback) | ✅ Completo | 1.7.0 |
| Fase 7 | Deuda técnica, calidad y producción | ❌ Pendiente | — |

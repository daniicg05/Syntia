# Plan de Implementación por Fases: Syntia

> Documento actualizado el **2026-03-09**. Refleja el estado real del código.
> Repositorio: https://github.com/daniicg05/Syntia.git

---

## Resumen General del Proyecto

Syntia es una plataforma web que permite a usuarios (emprendedores, autónomos, PYMEs) recibir recomendaciones personalizadas sobre subvenciones, ayudas y licitaciones públicas mediante un motor de matching con IA. La arquitectura es **monolítica modular** con Spring Boot, Thymeleaf, PostgreSQL y seguridad híbrida (sesión + JWT).

**Stack fijo:**
- Backend: Java 17, Spring Boot 3.5.x, Spring Security 6.x
- Seguridad: JWT (jjwt 0.12.6) + BCrypt + CORS
- Persistencia: Spring Data JPA + PostgreSQL 17.2 (`syntia_db`)
- Frontend: Thymeleaf + Bootstrap 5 + JavaScript vanilla
- **IA:** OpenAI Chat Completions API (`gpt-4.1`) con fallback rule-based automático
- Puerto: `8080` | BD usuario: `syntia` / pass: `syntia`

---

## Estado Actual (2026-03-09) — v2.3.0

| Componente | Estado |
|------------|--------|
| Infraestructura base (pom.xml, application.properties) | ✅ Completo |
| Seguridad (SecurityConfig, CorsConfig, JWT) | ✅ Completo |
| Modelo de dominio (entidades JPA) | ✅ Completo |
| Repositorios JPA | ✅ Completo |
| Registro de usuario (`/registro`) | ✅ Completo |
| Login / Logout (`/login`, `/logout`) | ✅ Completo |
| Redirección por rol (`/default`) | ✅ Completo |
| Perfil de usuario (crear/editar/ver) | ✅ Completo |
| Gestión de proyectos (CRUD) | ✅ Completo |
| Motor de matching (búsqueda directa BDNS + OpenAI gpt-4.1) | ✅ Completo |
| Filtrado convocatorias caducadas (`vigente=true` + filtro en memoria) | ✅ Completo |
| Recomendaciones (generar, ver, filtrar) | ✅ Completo |
| Dashboard usuario (métricas, top recs, roadmap) | ✅ Completo |
| Panel administrativo completo | ✅ Completo |
| API REST con JWT | ✅ Completo |
| Integración OpenAI gpt-4.1 (análisis semántico) | ✅ Completo |
| Integración API BDNS real (búsqueda directa) | ✅ Completo |
| Vista perfil solo lectura (`perfil-ver.html`) | ✅ Completo |
| Fragments Thymeleaf reutilizables (navbar, footer) | ✅ Completo |
| Aviso legal público (`/aviso-legal`) | ✅ Completo |
| Optimización N+1 métricas admin | ✅ Completo |
| Filtros recomendaciones delegados a BD | ✅ Completo |
| Persistencia selectiva (solo convocatorias recomendadas ≥ 40pts) | ✅ Completo |
| Datos de prueba (`data-test.sql`) | ❌ Eliminado (se usan datos reales de BDNS) |
| Perfil Spring producción (`application-prod.properties`) | ✅ Completo |

---

## Fase 1 – Autenticación y Perfil de Usuario
> **Estado: ✅ COMPLETADA (v1.0.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 1.1 | Registro de usuario con validación (email + contraseña) | ✅ |
| 1.2 | Login por formulario con redirección por rol | ✅ |
| 1.3 | Logout con invalidación de sesión | ✅ |
| 1.4 | Formulario de creación/edición de perfil | ✅ |
| 1.5 | Vista de perfil solo lectura (`/usuario/perfil/ver`) | ✅ |

**Componentes:** `PerfilService`, `PerfilController`, `PerfilDTO`, `perfil.html`, `perfil-ver.html`, `perfil.js`

---

## Fase 2 – Gestión de Proyectos
> **Estado: ✅ COMPLETADA (v1.1.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 2.1–2.5 | CRUD completo de proyectos | ✅ |

**Componentes:** `ProyectoService`, `ProyectoController`, `ProyectoDTO`, vistas lista/formulario/detalle, `proyecto.js`

---

## Fase 3 – Motor de Matching con IA
> **Estado: ✅ COMPLETADA (v1.2.0 + v1.7.0 + v2.0.0 + v2.1.0 + v2.3.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 3.1 | Carga manual de convocatorias (admin) | ✅ |
| 3.2 | Búsqueda directa en API BDNS real (~615.000 convocatorias) | ✅ |
| 3.3 | Filtrado `vigente=true` — solo convocatorias con plazo abierto | ✅ |
| 3.4 | Generación de keywords con OpenAI basadas en perfil + proyecto | ✅ |
| 3.5 | Evaluación semántica de compatibilidad con gpt-4.1 | ✅ |
| 3.6 | Puntuación 0-100 con criterios explícitos por rango | ✅ |
| 3.7 | Explicación en lenguaje natural (punto fuerte + condición a verificar) | ✅ |
| 3.8 | Persistencia selectiva: solo convocatorias ≥ 40 puntos se guardan en BD | ✅ |
| 3.9 | Fallback automático a motor rule-based si OpenAI no disponible | ✅ |
| 3.10 | Filtrado recomendaciones por tipo, sector, ubicación (delegado a BD) | ✅ |

**Flujo del motor:**
```
Perfil + Proyecto
      ↓
OpenAI gpt-4.1 → genera 4-6 keywords de búsqueda
      ↓
API BDNS real (?vigente=true) → 20 resultados por keyword
      ↓
Deduplicación en memoria por título → top 20 candidatas
      ↓
OpenAI evalúa cada candidata → puntuación 0-100 + explicación
      ↓
≥ 40 puntos → se persiste convocatoria + recomendación en BD
```

**Configuración (`application.properties`):**
```properties
openai.api-key=${OPENAI_API_KEY:}   # vacío = fallback a motor rule-based
openai.model=gpt-4.1
openai.max-tokens=150
openai.temperature=0.1
```

**Componentes:** `MotorMatchingService`, `OpenAiClient`, `OpenAiMatchingService`, `BdnsClientService`, `RecomendacionService`, `RecomendacionController`, `RecomendacionDTO`, `recomendaciones.html`

---

## Fase 4 – Dashboard Interactivo y Roadmap
> **Estado: ✅ COMPLETADA (v1.3.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 4.1 | Dashboard usuario con métricas y top recomendaciones | ✅ |
| 4.2 | Roadmap estratégico ordenado por fecha de cierre | ✅ |
| 4.3 | Indicadores visuales de puntuación (barra de progreso + badge) | ✅ |
| 4.4 | Aviso legal visible | ✅ |

**Componentes:** `DashboardService`, `dashboard.html`, `dashboard.js`

---

## Fase 5 – Panel Administrativo
> **Estado: ✅ COMPLETADA (v1.4.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 5.1–5.4 | CRUD usuarios (listar, ver detalle, cambiar rol, eliminar) | ✅ |
| 5.5–5.6 | CRUD convocatorias + importación desde BDNS | ✅ |
| 5.7 | Métricas generales del sistema (sin N+1) | ✅ |

**Componentes:** `AdminController`, vistas admin/dashboard, admin/usuarios/*, admin/convocatorias/*

---

## Fase 6 – API REST y Despliegue
> **Estado: ✅ COMPLETADA (v1.5.0 + v1.8.0)**

| # | Funcionalidad | Estado |
|---|--------------|--------|
| 6.1 | `POST /api/auth/login` → devuelve JWT | ✅ |
| 6.2 | `GET/PUT /api/usuario/perfil` protegido con JWT | ✅ |
| 6.3 | CRUD `/api/usuario/proyectos` protegido con JWT | ✅ |
| 6.4 | `GET/POST /api/usuario/proyectos/{id}/recomendaciones` | ✅ |
| 6.5 | `application-prod.properties` con variables de entorno | ✅ |

**Componentes:** `AuthRestController`, `PerfilRestController`, `ProyectoRestController`, `RecomendacionRestController`, `LoginRequestDTO`, `LoginResponseDTO`

---

## Próximas Mejoras (Backlog)

| # | Mejora | Prioridad |
|---|--------|-----------|
| B.1 | Alertas automáticas por email cuando aparezcan nuevas convocatorias compatibles | Media |
| B.2 | Exportación de recomendaciones en PDF | Media |
| B.3 | Estimación de probabilidad de éxito según perfil histórico | Baja |
| B.4 | Integración con fuentes europeas (Horizon Europe, FEDER) | Baja |
| B.5 | Tests de integración automatizados (JUnit 5 + MockMvc) | Alta |


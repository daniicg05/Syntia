# Syntia MVP

> Plataforma inteligente de recomendaciones de subvenciones, ayudas y licitaciones públicas para emprendedores, autónomos y PYMEs.

---

## ¿Para qué sirve Syntia?

Encontrar subvenciones y ayudas públicas es un proceso lento y complejo: hay miles de convocatorias activas en distintos portales (BOE, BDNS, portales autonómicos…) y es difícil saber cuáles aplican a tu proyecto concreto.

**Syntia automatiza ese proceso.** El usuario describe su proyecto (sector, ubicación, objetivos) y el motor de matching analiza todas las convocatorias disponibles para devolver una lista priorizada de las más compatibles, con una puntuación de afinidad y una explicación en lenguaje natural generada por IA.

### Casos de uso principales

| Usuario | Qué puede hacer |
|---------|----------------|
| **Emprendedor / Autónomo / PYME** | Registrarse, crear proyectos, recibir recomendaciones de subvenciones ordenadas por compatibilidad, ver fechas de cierre en un roadmap |
| **Administrador** | Gestionar usuarios y convocatorias, importar convocatorias desde la BDNS, ver métricas del sistema |
| **Integración externa** | Consumir la API REST con JWT para obtener perfil, proyectos y recomendaciones |

---

## Demo rápida

```
# 1. Clona el repositorio
git clone https://github.com/daniicg05/Syntia.git
cd Syntia

# 2. Arranca la aplicación (requiere PostgreSQL local)
./mvnw spring-boot:run

# 3. Abre en el navegador
http://localhost:8080
```

**Credenciales de prueba** (tras ejecutar `data-test.sql`):

| Rol | Email | Contraseña |
|-----|-------|-----------|
| Admin | `admin@syntia.com` | `admin123` |
| Usuario | `usuario@syntia.com` | `user123` |

---

## Flujo de uso para el usuario final

```
1. Registro / Login
        │
        ▼
2. Completar perfil  ←── Aquí describes QUIÉN ERES
   (sector, tipo entidad, objetivos, necesidades, descripción libre)
        │
        ▼
3. Crear proyecto    ←── Aquí describes QUÉ QUIERES FINANCIAR
   (nombre, sector, ubicación, descripción del proyecto)
        │
        ▼
4. "Analizar con IA" ←── El motor cruza perfil + proyecto vs. todas las convocatorias
        │
        ▼
5. Ver lista priorizada con puntuación 0-100 y explicación en lenguaje natural
   (badge 🤖 si usó OpenAI, ⚙️ si usó motor de reglas)
        │
        ▼
6. Filtrar por tipo / sector / ubicación
        │
        ▼
7. Consultar roadmap de fechas de cierre (Dashboard)
        │
        ▼
8. Clic en "Ver convocatoria oficial" → enlace directo al portal público
```

---

## Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN               │
│  Thymeleaf + Bootstrap 5  │  API REST (JSON + JWT)  │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                  CAPA DE NEGOCIO                      │
│  PerfilService │ ProyectoService │ ConvocatoriaService│
│  MotorMatchingService ──► OpenAiMatchingService       │
│                        └──► Rule-based (fallback)     │
│  DashboardService │ BdnsClientService                 │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                 CAPA DE PERSISTENCIA                  │
│  Spring Data JPA │ PostgreSQL 17                      │
│  Repositorios: Usuario, Perfil, Proyecto,             │
│                Convocatoria, Recomendacion             │
└─────────────────────────────────────────────────────┘
```

### Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5.x |
| Seguridad | Spring Security 6 + JWT (jjwt 0.12.6) + BCrypt |
| Persistencia | Spring Data JPA + Hibernate + PostgreSQL 17 |
| Vistas | Thymeleaf 3 + Bootstrap 5 |
| IA | OpenAI Chat Completions API (`gpt-4o-mini`) |
| Datos BDNS | API pública de infosubvenciones.es |
| Build | Maven Wrapper |
| Puerto | `8080` |

---

## Requisitos previos

- **Java 17+** (OpenJDK o Temurin)
- **PostgreSQL 17** corriendo en local
- Maven (o usar `./mvnw` incluido)
- *(Opcional)* API key de OpenAI para el motor de IA semántico

### Configurar la base de datos

```sql
-- En psql o pgAdmin:
CREATE USER syntia WITH PASSWORD 'syntia';
CREATE DATABASE syntia_db OWNER syntia;
```

### Variables de entorno (opcionales en desarrollo)

```bash
# Motor de IA (si no se configura, se usa el motor rule-based automáticamente)
OPENAI_API_KEY=sk-proj-...

# Para producción (ver application-prod.properties)
DB_URL=jdbc:postgresql://host:5432/syntia_db
DB_USER=syntia
DB_PASSWORD=...
JWT_SECRET=...
PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

---

## Instalación y arranque

```bash
# Clonar
git clone https://github.com/daniicg05/Syntia.git
cd Syntia

# (Opcional) Cargar datos de prueba
# Ejecutar src/main/resources/data-test.sql en tu BD

# Arrancar en desarrollo
./mvnw spring-boot:run

# Arrancar en producción
./mvnw clean package -DskipTests
java -jar target/syntia-mvp-*.jar --spring.profiles.active=prod
```

---

## Estructura del proyecto

```
src/main/
├── java/com/syntia/mvp/
│   ├── config/          SecurityConfig, CorsConfig, JwtAuthFilter…
│   ├── controller/      AuthController, PerfilController, ProyectoController,
│   │   │                RecomendacionController, AdminController
│   │   └── api/         AuthRestController, PerfilRestController,
│   │                    ProyectoRestController, RecomendacionRestController
│   ├── model/           Usuario, Perfil, Proyecto, Convocatoria, Recomendacion
│   │   └── dto/         PerfilDTO, ProyectoDTO, ConvocatoriaDTO, RecomendacionDTO…
│   ├── repository/      JPA repositories
│   ├── service/         Lógica de negocio + MotorMatchingService + OpenAiClient
│   │                    + BdnsClientService + DashboardService
│   └── security/        JwtService, JwtAuthenticationFilter
└── resources/
    ├── application.properties          Configuración base (desarrollo)
    ├── application-prod.properties     Configuración producción (vars de entorno)
    ├── data-test.sql                   Datos de prueba
    ├── static/javascript/              dashboard.js, perfil.js, proyecto.js, registro.js
    └── templates/
        ├── fragments/                  navbar-usuario, navbar-admin, footer
        ├── usuario/                    dashboard, perfil, perfil-ver, proyectos/*
        └── admin/                      dashboard, usuarios/*, convocatorias/*
```

---

## API REST

Todos los endpoints REST requieren el header `Authorization: Bearer <token>`.

```bash
# 1. Obtener token
POST /api/auth/login
{"email": "usuario@syntia.com", "password": "user123"}

# 2. Usar el token
GET  /api/usuario/perfil
GET  /api/usuario/proyectos
GET  /api/usuario/proyectos/{id}/recomendaciones
POST /api/usuario/proyectos/{id}/recomendaciones/generar
```

---

## Motor de matching

El corazón de Syntia. Cuando el usuario pulsa **"Analizar con IA"** en un proyecto, el motor evalúa todas las convocatorias activas y genera una puntuación de compatibilidad para cada una.

### ¿Por qué necesitas perfil Y proyecto?

Ambos aportan contexto distinto al motor:

| Dato | Viene de | Para qué sirve en el matching |
|------|----------|-------------------------------|
| Nombre del proyecto | Proyecto | Identificar el tema principal |
| Sector del proyecto | Proyecto | Cruzar con el sector de la convocatoria (+40 pts rule-based) |
| Ubicación del proyecto | Proyecto | Cruzar con el ámbito geográfico (+30 pts rule-based) |
| Descripción del proyecto | Proyecto | Contexto semántico para OpenAI y keywords rule-based |
| Tipo de entidad | Perfil | ¿Es autónomo, startup, PYME? Algunas convocatorias excluyen tipos |
| Objetivos | Perfil | Añade intención: "quiero internacionalizarme", "busco I+D"… |
| Necesidades de financiación | Perfil | Alinea con el tipo de ayuda (subvención, préstamo, aval…) |
| Descripción libre | Perfil | El texto más rico: OpenAI lo usa para entender el contexto real |

> Si no tienes perfil, el motor funciona igualmente pero con menos contexto — las recomendaciones serán menos precisas.

---

### Flujo completo paso a paso

```
Usuario pulsa "Analizar con IA" en un proyecto
                    │
                    ▼
    RecomendacionController.generarRecomendaciones()
                    │
                    ▼
    MotorMatchingService.generarRecomendaciones(proyecto)
          │
          ├─ 1. Borra recomendaciones anteriores del proyecto (para regenerar desde cero)
          │
          ├─ 2. Carga el perfil del usuario (opcional, mejora el contexto)
          │
          └─ 3. Itera sobre TODAS las convocatorias activas en BD
                          │
                          ▼
             Para cada convocatoria → evaluarConFallback()
                          │
                          ├─── ¿Hay OPENAI_API_KEY configurada?
                          │         │
                          │        SÍ ──► OpenAiMatchingService.analizar()
                          │                    │
                          │                    ├─ Construye el PROMPT con:
                          │                    │    · Proyecto: nombre, sector, ubicación, descripción
                          │                    │    · Perfil:   tipo entidad, objetivos,
                          │                    │                necesidades, descripción libre
                          │                    │    · Convocatoria: título, tipo, sector, ámbito, fuente
                          │                    │
                          │                    └─ Envía a gpt-4o-mini → recibe JSON:
                          │                         {"puntuacion": 78, "explicacion": "..."}
                          │
                          └─── NO (o falla OpenAI) ──► Motor Rule-Based (fallback)
                                                            │
                                                            ├─ +40 pts si sector coincide
                                                            ├─ +30 pts si ubicación coincide
                                                            ├─ +20 pts si convocatoria es nacional
                                                            └─ +10 pts si keywords del título
                                                                        aparecen en la descripción
                          │
                          ▼
          Si puntuación > 0 → guarda Recomendacion en BD
               (con campo usadaIa = true/false)
                          │
                          ▼
     Ordena todas por puntuación DESC y devuelve la lista
                          │
                          ▼
     Vista recomendaciones.html muestra:
          · Barra de progreso con la puntuación (0-100)
          · Explicación en lenguaje natural
          · Badge  🤖 Analizado por IA  o  ⚙️ Motor de reglas
          · Enlace directo a la convocatoria oficial
          · Filtros por tipo, sector, ubicación (delegados a BD)
```

### Ejemplo de prompt real enviado a OpenAI

```
PROYECTO: App de gestión agrícola
Sector: Agricultura
Ubicacion: Andalucía
Descripcion: Plataforma móvil para gestión de cultivos con sensores IoT...

Tipo entidad: Startup
Objetivos: Digitalizar el sector agrícola andaluz y exportar a Latinoamérica
Necesidades: Financiación para I+D y contratación de desarrolladores
Perfil libre: Somos 3 fundadores con experiencia en agro y tecnología...

CONVOCATORIA: Ayudas FEADER – Modernización agraria
Tipo: Ayuda
Sector conv: Agricultura
Ambito: Nacional
Fuente: BDNS
```

OpenAI responde:
```json
{"puntuacion": 85, "explicacion": "Alta compatibilidad: el proyecto de digitalización agrícola encaja directamente con el objetivo de modernización del FEADER. La ubicación en Andalucía es elegible y las necesidades de I+D están alineadas con las bases de la convocatoria."}
```

### Resultado almacenado en BD

Cada recomendación guarda en la tabla `recomendaciones`:

| Campo | Valor ejemplo |
|-------|--------------|
| `proyecto_id` | 3 |
| `convocatoria_id` | 7 |
| `puntuacion` | 85 |
| `explicacion` | "Alta compatibilidad: el proyecto de digitalización…" |
| `usada_ia` | `true` |

---

## Documentación técnica

Toda la documentación del proyecto está en la carpeta `docs/`:

| Archivo | Contenido |
|---------|-----------|
| `01-requisitos.md` | Requisitos funcionales y no funcionales |
| `02-plan-proyecto.md` | Plan de proyecto y hitos |
| `03-especificaciones-tecnicas.md` | Stack, modelos de datos, endpoints |
| `04-manual-desarrollo.md` | Guía de desarrollo + §8 despliegue en producción |
| `05-changelog.md` | Historial de versiones (v0.1.0 → v1.8.0) |
| `06-diagramas.md` | Diagramas de arquitectura y secuencia |
| `07-fases-implementacion.md` | Estado real de las 7 fases implementadas |

---

## Versión actual

**v1.8.0** — Todas las fases implementadas:
- ✅ Autenticación + perfil de usuario
- ✅ Gestión de proyectos (CRUD)
- ✅ Motor de matching rule-based + OpenAI
- ✅ Dashboard interactivo + roadmap de fechas
- ✅ Panel administrativo completo
- ✅ API REST con JWT
- ✅ Integración BDNS + perfil de producción + fragments Thymeleaf

---

## Licencia

Proyecto académico / MVP — uso interno. © 2026 Daniel.

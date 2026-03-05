# Manual de Desarrollo: Syntia

## 1. Repositorio y Control de Versiones

- **Repositorio remoto:** https://github.com/daniicg05/Syntia.git
- **Flujo de desarrollo basado en ramas:**
  - `main` → rama estable (producción).
  - `develop` → rama de integración.
  - `feature/*` → funcionalidad específica (ej: `feature/jwt-auth`, `feature/perfil-usuario`).
  - `bugfix/*` → corrección de errores.
- Pull requests con revisión por al menos un miembro del equipo.
- Protección de ramas `main` y `develop` contra push directo.

### Clonar el repositorio

```bash
git clone https://github.com/daniicg05/Syntia.git
cd Syntia
```

## 2. Estándares de Codificación

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Clases | PascalCase | `UsuarioService`, `JwtAuthenticationFilter` |
| Métodos / Variables | camelCase | `buscarPorEmail()`, `tokenExpiration` |
| Constantes | MAYÚSCULAS_CON_GUIONES | `JWT_SECRET`, `ROL_ADMIN` |
| Paquetes | minúsculas | `com.syntia.mvp.config` |
| Entidades JPA | Singular, PascalCase | `Usuario`, `Perfil`, `Proyecto` |
| Tablas BD | snake_case, plural | `usuarios`, `perfiles`, `proyectos` |
| Endpoints REST | kebab-case, plural | `/api/usuarios`, `/api/convocatorias` |
| Comentarios | JavaDoc en clases y métodos públicos | `/** Descripción */` |

## 3. Uso de Git
- Commits frecuentes y descriptivos.
- Formato de commit recomendado: `tipo(alcance): descripción breve`
  - Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
  - Ejemplo: `feat(auth): implementar generación de JWT en login`
- Sincronización regular de ramas para evitar conflictos.
- No subir credenciales ni secrets al repositorio (usar `.gitignore` y variables de entorno).

## 4. Resolución de Conflictos
- Actualizar la rama local antes de fusionar: `git pull origin develop`
- Resolver conflictos localmente y documentar los cambios.
- Ejecutar las pruebas antes de hacer push tras resolver conflictos.

## 5. Configuración del Entorno Local

### 5.1. Prerrequisitos

| Herramienta | Versión mínima |
|-------------|----------------|
| Java JDK | 17+ |
| Maven | 3.8+ (o usar el wrapper `mvnw` incluido) |
| PostgreSQL | 17.2 |
| Git | 2.x |
| IDE recomendado | IntelliJ IDEA / VS Code con extensiones Java |

### 5.2. Configuración de la Base de Datos

```sql
-- Conectar a PostgreSQL como superusuario y ejecutar:
CREATE USER syntia WITH PASSWORD 'syntia';
CREATE DATABASE syntia_db OWNER syntia;
GRANT ALL PRIVILEGES ON DATABASE syntia_db TO syntia;
```

Verificar conexión:
```bash
psql -h localhost -p 5432 -U syntia -d syntia_db
```

### 5.3. Configuración de `application.properties`

El archivo `src/main/resources/application.properties` debe contener:

```properties
spring.application.name=SyntiaMVP
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/syntia_db
spring.datasource.username=syntia
spring.datasource.password=syntia
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false

# JWT
jwt.secret=TU_CLAVE_SECRETA_BASE64_AQUI
jwt.expiration=86400000

# Logging
logging.level.org.springframework.security=DEBUG
logging.level.com.syntia.mvp=DEBUG
```

### 5.4. Ejecución del Proyecto

```bash
# Con Maven wrapper (recomendado)
./mvnw spring-boot:run

# O con Maven instalado
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

### 5.5. Dependencias del `pom.xml`

Todas las dependencias necesarias están incluidas. Ver tabla completa en `03-especificaciones-tecnicas.md § 2.1`.

## 6. Estructura de Paquetes del Proyecto

Estado actual implementado (fases 1–6 completas):

```
com.syntia.mvp
├── SyntiaMvpApplication.java
├── config/
│   ├── SecurityConfig.java           ✅ Dual filter chain: JWT (/api/**) + formulario (web)
│   ├── CorsConfig.java               ✅ allowedOriginPatterns para dev; ajustar en prod
│   ├── GlobalExceptionHandler.java   ✅ @RestControllerAdvice para /api/**
│   ├── RestExceptionHandler.java     ✅ Limpiado (sin código muerto)
│   └── WebExceptionHandler.java      ✅ Manejo de errores para vistas MVC
├── security/
│   ├── JwtService.java               ✅ Generación, validación y extracción de claims
│   └── JwtAuthenticationFilter.java  ✅ Filtro OncePerRequestFilter
├── controller/
│   ├── AuthController.java           ✅ Login, registro, dashboard usuario/admin
│   ├── PerfilController.java         ✅ GET/POST /usuario/perfil
│   ├── ProyectoController.java       ✅ CRUD /usuario/proyectos
│   ├── RecomendacionController.java  ✅ GET/POST /usuario/proyectos/{id}/recomendaciones
│   ├── AdminController.java          ✅ CRUD /admin/usuarios y /admin/convocatorias
│   └── CustomErrorController.java    ✅ Páginas de error personalizadas
├── controller/api/
│   ├── AuthRestController.java       ✅ POST /api/auth/login → JWT
│   ├── PerfilRestController.java     ✅ GET/PUT /api/usuario/perfil
│   ├── ProyectoRestController.java   ✅ CRUD /api/usuario/proyectos
│   └── RecomendacionRestController.java ✅ GET + POST /generar
├── model/
│   ├── Usuario.java                  ✅ @Entity, Lombok, BCrypt password
│   ├── Rol.java                      ✅ enum: ADMIN, USUARIO
│   ├── Perfil.java                   ✅ @OneToOne con Usuario
│   ├── Proyecto.java                 ✅ @ManyToOne con Usuario
│   ├── Convocatoria.java             ✅ Catálogo global de convocatorias
│   ├── Recomendacion.java            ✅ Proyecto + Convocatoria + puntuacion
│   └── ErrorResponse.java            ✅ DTO para errores REST
├── model/dto/
│   ├── RegistroDTO.java              ✅ Registro con confirmación de contraseña
│   ├── PerfilDTO.java                ✅ @NotBlank, @Size
│   ├── ProyectoDTO.java              ✅ @NotBlank, @Size
│   ├── RecomendacionDTO.java         ✅ Aplana relación LAZY para vistas
│   ├── ConvocatoriaDTO.java          ✅ @NotBlank, @Size (getters/setters explícitos)
│   ├── LoginRequestDTO.java          ✅ @Email, @NotBlank
│   └── LoginResponseDTO.java         ✅ token + email + rol + expiresIn
├── repository/
│   ├── UsuarioRepository.java        ✅ findByEmail, existsByEmail
│   ├── PerfilRepository.java         ✅ findByUsuarioId
│   ├── ProyectoRepository.java       ✅ findByUsuarioId
│   ├── ConvocatoriaRepository.java   ✅ filtrar() JPQL, sectores/tipos distintos
│   └── RecomendacionRepository.java  ✅ findByProyectoId, deleteByProyectoId, countByProyectoId
└── service/
    ├── CustomUserDetailsService.java  ✅ Carga por email para Spring Security
    ├── UsuarioService.java            ✅ registrar, buscar, obtenerTodos, eliminar, cambiarRol
    ├── PerfilService.java             ✅ tienePerfil, obtenerPerfil, crear, actualizar, toDTO
    ├── ProyectoService.java           ✅ CRUD + verificarPropiedad + toDTO
    ├── MotorMatchingService.java      ✅ Scoring rule-based (sector, ubicación, nacional, keywords)
    ├── RecomendacionService.java      ✅ obtenerPorProyecto, contarPorProyecto
    ├── ConvocatoriaService.java       ✅ CRUD completo + toDTO
    └── DashboardService.java          ✅ topRecomendaciones, roadmap, contarTotal, RoadmapItem record
```

### Recursos estáticos y plantillas

```
src/main/resources/
├── application.properties            ✅ PostgreSQL, JPA, Thymeleaf, JWT, variables de entorno
├── data-test.sql                     ✅ 2 usuarios, 1 perfil, 8 convocatorias, 1 proyecto
├── static/
│   ├── bootstrap/                    ✅ Bootstrap 5 CSS
│   ├── bootsprap/                    ✅ Bootstrap 5 JS (nombre con typo, no renombrar)
│   └── javascript/
│       ├── registro.js               ✅ Validación contraseñas + email frontend
│       ├── perfil.js                 ✅ Validaciones formulario perfil
│       ├── proyecto.js               ✅ Validaciones + contador caracteres
│       └── dashboard.js              ✅ Contador días restantes en roadmap
└── templates/
    ├── login.html
    ├── registro.html
    ├── error.html
    ├── error/403.html, 404.html, 409.html, 500.html
    ├── usuario/
    │   ├── dashboard.html            ✅ Métricas, top recomendaciones, roadmap
    │   ├── perfil.html               ✅ Formulario crear/editar perfil
    │   └── proyectos/
    │       ├── lista.html
    │       ├── formulario.html       ✅ Crear y editar (vista unificada)
    │       ├── detalle.html
    │       └── recomendaciones.html  ✅ Filtros, puntuación, aviso legal
    └── admin/
        ├── dashboard.html            ✅ Métricas del sistema
        ├── usuarios/
        │   ├── lista.html            ✅ Cambio de rol inline + modal eliminar
        │   └── detalle.html          ✅ Datos + proyectos del usuario
        └── convocatorias/
            ├── lista.html            ✅ Tabla con fechas urgentes resaltadas
            └── formulario.html       ✅ Crear y editar convocatoria
```

## 7. Referencia de la API REST

Todos los endpoints REST están bajo `/api/**` y usan autenticación JWT.  
Cabecera requerida: `Authorization: Bearer <token>`

### Autenticación

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | ❌ | Credenciales → JWT |

**Body request:** `{ "email": "...", "password": "..." }`  
**Body response:** `{ "token": "...", "email": "...", "rol": "...", "expiresIn": 86400000 }`

### Perfil

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/usuario/perfil` | JWT | Ver perfil del usuario autenticado |
| `PUT` | `/api/usuario/perfil` | JWT | Crear o actualizar perfil |

### Proyectos

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/usuario/proyectos` | JWT | Listar proyectos |
| `GET` | `/api/usuario/proyectos/{id}` | JWT | Ver proyecto por ID |
| `POST` | `/api/usuario/proyectos` | JWT | Crear proyecto |
| `PUT` | `/api/usuario/proyectos/{id}` | JWT | Editar proyecto |
| `DELETE` | `/api/usuario/proyectos/{id}` | JWT | Eliminar proyecto |

### Recomendaciones

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/usuario/proyectos/{id}/recomendaciones` | JWT | Ver recomendaciones del proyecto |
| `POST` | `/api/usuario/proyectos/{id}/recomendaciones/generar` | JWT | Disparar motor de matching |

### Códigos de respuesta estándar

| Código | Situación |
|--------|-----------|
| `200` | Éxito |
| `400` | Validación fallida (`MethodArgumentNotValidException`) |
| `401` | Credenciales incorrectas |
| `403` | Sin permisos (`AccessDeniedException`) |
| `404` | Recurso no encontrado (`EntityNotFoundException`) |
| `409` | Conflicto de estado (`IllegalStateException`) |
| `500` | Error interno del servidor |

---

## 8. Instrucciones de Despliegue

### 8.1. Entorno de Desarrollo

```bash
# Clonar y arrancar
git clone https://github.com/daniicg05/Syntia.git
cd Syntia
./mvnw spring-boot:run
```

Acceder en: `http://localhost:8080`  
Datos de prueba: ejecutar `src/main/resources/data-test.sql` una vez creadas las tablas.

### 8.2. Entorno de Producción

Variables de entorno requeridas:

| Variable | Descripción |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | URL completa JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario de BD |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de BD |
| `JWT_SECRET` | Clave secreta JWT (mínimo 32 caracteres, base64) |
| `JWT_EXPIRATION` | Expiración del token en milisegundos (ej: `86400000`) |

Configuración recomendada para producción (sobreescribir en `application-prod.properties`):

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.thymeleaf.cache=true
logging.level.org.springframework.security=INFO
logging.level.com.syntia.mvp=INFO
```

> **Importante:** Cambiar `allowedOriginPatterns("*")` en `CorsConfig.java` por el dominio real antes del despliegue.

---

## 9. Deuda Técnica Registrada

| Prioridad | Componente | Descripción |
|-----------|-----------|-------------|
| 🟡 Media | `AdminController.dashboard()` | N+1 queries para métricas → migrar a query `COUNT` agregada |
| 🟡 Media | `RecomendacionController` | Filtros en memoria, no en BD → escala mal con volumen alto |
| 🟡 Media | `perfil-ver.html` | Vista solo lectura ausente; actualmente perfil.html hace las veces de vista y edición |
| 🟡 Media | `application-prod.properties` | No existe perfil Spring para producción |
| 🟢 Baja | `06-diagramas.md` | Actualizar diagramas de secuencia con flujos de las fases 3–6 |
| 🟢 Baja | Paginación | Sin paginación en listados de usuarios y convocatorias (admin) |
| 🟢 Baja | `bootsprap/` typo | Carpeta de JS de Bootstrap con nombre erróneo; corregir requiere actualizar todas las vistas |


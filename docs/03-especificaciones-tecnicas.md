# Especificaciones Técnicas del Proyecto: Syntia

## 1. Arquitectura

- Modelo cliente-servidor con arquitectura monolítica modular (Spring Boot).
- **Backend:** Spring Boot 3.5.x, gestión de usuarios, integración con BDNS, motor de priorización.
- **Frontend:** Thymeleaf (vistas server-side) + Bootstrap 5 (responsive), dashboard y panel administrativo.
- **API REST:** Endpoints REST protegidos con JWT para integraciones futuras y consumo desde JavaScript.
- **Base de datos:** PostgreSQL 17.2, almacenamiento de usuarios, perfiles, recomendaciones y metadatos de convocatorias.
- **Seguridad:** Enfoque híbrido — formulario de login con sesión para vistas Thymeleaf + JWT para endpoints REST/API.

## 2. Tecnologías Seleccionadas

| Capa | Tecnología | Notas |
|------|------------|-------|
| Lenguaje | Java 17 | LTS, definido en `pom.xml` |
| Framework | Spring Boot 3.5.x | Parent POM |
| Seguridad | Spring Security 6.x + JWT (jjwt 0.12.x) | Autenticación híbrida: sesión + JWT |
| Persistencia | Spring Data JPA + Hibernate | ORM sobre PostgreSQL |
| Motor de plantillas | Thymeleaf | Vistas server-side |
| Frontend | HTML5, CSS3, JavaScript, Bootstrap 5 | Responsive |
| Base de datos | PostgreSQL 17.2 | Puerto `5432`, BD: `syntia_db` |
| Validación | Spring Boot Starter Validation (Bean Validation) | `@Valid`, `@NotBlank`, etc. |
| Utilidades | Lombok | Reducción de boilerplate |
| Dev Tools | Spring Boot DevTools | Hot-reload en desarrollo |
| Infraestructura | Servidor en la nube, comunicación HTTPS | Tomcat embebido, puerto `8080` |

### 2.1. Dependencias Maven Requeridas

| Dependencia | groupId | artifactId | Estado |
|-------------|---------|------------|--------|
| Spring Web | `org.springframework.boot` | `spring-boot-starter-web` | ✅ Incluida |
| Spring Security | `org.springframework.boot` | `spring-boot-starter-security` | ✅ Incluida |
| Spring Data JPA | `org.springframework.boot` | `spring-boot-starter-data-jpa` | ✅ Incluida |
| PostgreSQL Driver | `org.postgresql` | `postgresql` | ✅ Incluida |
| Lombok | `org.projectlombok` | `lombok` | ✅ Incluida |
| DevTools | `org.springframework.boot` | `spring-boot-devtools` | ✅ Incluida |
| Thymeleaf | `org.springframework.boot` | `spring-boot-starter-thymeleaf` | ✅ Incluida |
| Thymeleaf Extras Security | `org.thymeleaf.extras` | `thymeleaf-extras-springsecurity6` | ✅ Incluida |
| Bean Validation | `org.springframework.boot` | `spring-boot-starter-validation` | ✅ Incluida |
| JWT API | `io.jsonwebtoken` | `jjwt-api` (0.12.6) | ✅ Incluida |
| JWT Impl | `io.jsonwebtoken` | `jjwt-impl` (0.12.6) | ✅ Incluida |
| JWT Jackson | `io.jsonwebtoken` | `jjwt-jackson` (0.12.6) | ✅ Incluida |

## 3. Estándares
- **Seguridad:** autenticación JWT para API REST, sesiones para vistas Thymeleaf, cifrado de datos con BCrypt, HTTPS, política CORS.
- **Desarrollo:** convenciones de Java, validación de entradas con Bean Validation, control de acceso por roles (`ADMIN`, `USUARIO`).
- **Interfaz:** diseño intuitivo, profesional y responsivo con Bootstrap 5.

## 4. Interfaz de Usuario
- Registro, autenticación y captura de perfil.
- Dashboard interactivo y roadmap estratégico.
- Panel administrativo para supervisión.
- Todas las vistas renderizadas con Thymeleaf y Bootstrap 5.

## 5. Seguridad

### 5.1. Roles del Sistema

| Rol | Acceso | Descripción |
|-----|--------|-------------|
| `ADMIN` | `/admin/**` | Gestión de usuarios, supervisión del sistema, configuración del motor de IA |
| `USUARIO` | `/usuario/**` | Captura de perfil, creación de proyectos, visualización de recomendaciones y roadmap |

> **Nota:** El código debe usar `ROLE_ADMIN` y `ROLE_USUARIO` internamente (convención de Spring Security con prefijo `ROLE_`). No se usa `CLIENTE` ni `USER`.

### 5.2. Autenticación JWT (API REST)

El flujo JWT se utiliza para proteger los endpoints REST de la API:

1. **Login:** El usuario envía `POST /api/auth/login` con credenciales (`email`, `password`).
2. **Generación del token:** El backend valida las credenciales, y si son correctas, genera un JWT firmado con HMAC-SHA256 que contiene:
   - `sub` (subject): email del usuario.
   - `rol`: rol del usuario (`ADMIN` o `USUARIO`).
   - `iat` (issued at): timestamp de emisión.
   - `exp` (expiration): timestamp de expiración (configurable, por defecto 24h).
3. **Uso del token:** El cliente envía el token en cada petición REST en la cabecera `Authorization: Bearer <token>`.
4. **Validación:** Un filtro `JwtAuthenticationFilter` intercepta cada petición, extrae el token, lo valida (firma + expiración) y establece el contexto de seguridad.
5. **Configuración:** El secret y la expiración se definen en `application.properties`.

**Clases involucradas:**
- `JwtService`: generación, validación y extracción de claims del token.
- `JwtAuthenticationFilter`: filtro de Spring Security que intercepta y valida los tokens.

### 5.3. Autenticación por Formulario (Vistas Thymeleaf)

Para las vistas server-side renderizadas con Thymeleaf:
- Login mediante formulario HTML (`/login`).
- Sesión gestionada por Spring Security.
- Redirección tras login según rol (`/admin/dashboard` o `/usuario/dashboard`).
- Logout con invalidación de sesión (`/logout`).

### 5.4. Configuración CORS

La política CORS permite el acceso cross-origin para la API REST:

```
Orígenes permitidos: http://localhost:8080 (desarrollo), dominio de producción
Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS
Headers permitidos: Authorization, Content-Type
Credenciales: true (permite envío de cookies/tokens)
```

> **⚠️ Importante:** Cuando `allowCredentials = true`, NO se puede usar `"*"` como origen. Se deben especificar los orígenes concretos o usar `addAllowedOriginPattern("*")` (solo para desarrollo).

### 5.5. Cifrado y Protección
- Contraseñas cifradas con **BCrypt** (`BCryptPasswordEncoder`).
- Comunicación segura mediante **HTTPS** en producción.
- CSRF deshabilitado para endpoints REST (API stateless), habilitado para formularios Thymeleaf según necesidad.
- Prevención de vulnerabilidades comunes: XSS (escape de Thymeleaf), inyección SQL (JPA parametrizado), CSRF.

## 6. Estructura de Paquetes

```
com.syntia.mvp
├── SyntiaMvpApplication.java          # Clase principal
├── config/
│   ├── SecurityConfig.java            # Configuración de Spring Security (cadena de filtros)
│   ├── CorsConfig.java                # Configuración CORS
│   ├── WebExceptionHandler.java       # Manejo de excepciones para vistas Thymeleaf
│   └── GlobalExceptionHandler.java    # Manejo de excepciones genérico
├── security/
│   ├── JwtService.java                # Generación y validación de tokens JWT
│   └── JwtAuthenticationFilter.java   # Filtro de autenticación JWT
├── controller/
│   ├── AuthController.java            # Login, logout, redirección por rol
│   ├── CustomErrorController.java     # Manejo de errores HTTP
│   ├── UsuarioController.java         # (pendiente) CRUD de usuarios
│   └── api/                           # (pendiente) Controladores REST
├── model/
│   ├── Usuario.java                   # Entidad JPA
│   ├── Rol.java                       # Enum: ADMIN, USUARIO
│   ├── Perfil.java                    # Entidad JPA
│   ├── Proyecto.java                  # Entidad JPA
│   ├── Convocatoria.java              # Entidad JPA
│   ├── Recomendacion.java             # Entidad JPA
│   └── ErrorResponse.java             # DTO para respuestas de error
├── repository/
│   ├── UsuarioRepository.java         # (pendiente) JPA Repository
│   ├── PerfilRepository.java          # (pendiente) JPA Repository
│   ├── ProyectoRepository.java        # (pendiente) JPA Repository
│   ├── ConvocatoriaRepository.java    # (pendiente) JPA Repository
│   └── RecomendacionRepository.java   # (pendiente) JPA Repository
└── service/
    ├── CustomUserDetailsService.java  # Implementación de UserDetailsService
    ├── UsuarioService.java            # (pendiente) Lógica de negocio de usuarios
    ├── PerfilService.java             # (pendiente) Lógica de negocio de perfiles
    ├── ProyectoService.java           # (pendiente) Lógica de negocio de proyectos
    └── MotorMatchingService.java      # (pendiente) Motor de interpretación y matching
```

## 7. Configuración de `application.properties`

```properties
# Nombre de la aplicación
spring.application.name=SyntiaMVP

# Servidor
server.port=8080

# Base de datos PostgreSQL
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

## 8. Base de Datos

| Parámetro | Valor |
|-----------|-------|
| Motor | PostgreSQL 17.2 |
| Puerto | `5432` |
| Nombre de la BD | `syntia_db` |
| Usuario | `syntia` |
| Contraseña | `syntia` |

### Script de inicialización

```sql
-- Crear usuario y base de datos
CREATE USER syntia WITH PASSWORD 'syntia';
CREATE DATABASE syntia_db OWNER syntia;
GRANT ALL PRIVILEGES ON DATABASE syntia_db TO syntia;
```


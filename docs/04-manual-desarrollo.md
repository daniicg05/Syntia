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

Estado actual implementado:

```
com.syntia.mvp
├── SyntiaMvpApplication.java
├── config/
│   ├── SecurityConfig.java          ✅ Implementado
│   ├── CorsConfig.java              ✅ Implementado
│   ├── GlobalExceptionHandler.java  ✅ Implementado
│   ├── RestExceptionHandler.java    ✅ Implementado
│   └── WebExceptionHandler.java     ✅ Implementado
├── security/
│   ├── JwtService.java              ✅ Implementado
│   └── JwtAuthenticationFilter.java ✅ Implementado
├── controller/
│   ├── AuthController.java          ✅ Implementado
│   └── CustomErrorController.java   ✅ Implementado
├── model/
│   ├── Usuario.java                 ✅ Implementado
│   ├── Rol.java                     ✅ Implementado (enum: ADMIN, USUARIO)
│   ├── Perfil.java                  ✅ Implementado
│   ├── Proyecto.java                ✅ Implementado
│   ├── Convocatoria.java            ✅ Implementado
│   ├── Recomendacion.java           ✅ Implementado
│   └── ErrorResponse.java           ✅ Implementado
├── repository/
│   ├── UsuarioRepository.java       ✅ Implementado
│   ├── PerfilRepository.java        ✅ Implementado
│   ├── ProyectoRepository.java      ✅ Implementado
│   ├── ConvocatoriaRepository.java  ✅ Implementado
│   └── RecomendacionRepository.java ✅ Implementado
└── service/
    ├── CustomUserDetailsService.java ✅ Implementado
    └── UsuarioService.java           ✅ Implementado
```

Pendiente de implementar en siguientes hitos:
- `controller/api/` — Controladores REST
- `service/PerfilService.java`
- `service/ProyectoService.java`
- `service/MotorMatchingService.java`

## 7. Estado de la Deuda Técnica (resuelto en 2026-03-05)

Todos los problemas detectados en la auditoría inicial han sido corregidos:

| Problema | Estado |
|----------|--------|
| Imports de `es.fempa.acd` en `SecurityConfig`, `AuthController`, `CustomUserDetailsService` | ✅ Corregido |
| `CorsConfig`: `addAllowedOrigin("*")` incompatible con `allowCredentials` | ✅ Corregido → `setAllowedOriginPatterns` |
| `GlobalExceptionHandler`: dependencia `spring-data-rest` inexistente | ✅ Corregido → `jakarta.persistence.EntityNotFoundException` |
| `RestExceptionHandler`: código muerto con referencias al proyecto anterior | ✅ Limpiado |
| `AuthController`: `ROLE_CLIENTE` en vez de `ROLE_USUARIO` | ✅ Corregido |
| Modelos de dominio ausentes | ✅ Creados (Usuario, Rol, Perfil, Proyecto, Convocatoria, Recomendacion) |
| Repositorios ausentes | ✅ Creados (5 repositorios JPA) |
| `application.properties` vacío | ✅ Configurado (PostgreSQL, JPA, Thymeleaf, JWT) |
| `pom.xml`: Thymeleaf, jjwt, Bean Validation faltantes | ✅ Añadidas |


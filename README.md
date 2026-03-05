# Syntia – Plataforma de Recomendación de Subvenciones

Plataforma web que permite a los usuarios recibir recomendaciones personalizadas sobre subvenciones, ayudas, licitaciones y sistemas de financiación, mediante un motor de interpretación e IA y datos oficiales (BDNS).

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| Backend | Java 17, Spring Boot 3.5.x, Spring Security 6.x |
| Autenticación | JWT (jjwt 0.12.x) + Sesiones (formulario Thymeleaf) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 17.2 |
| Frontend | Thymeleaf, HTML5, CSS3, JavaScript, Bootstrap 5 |
| Validación | Bean Validation |
| Utilidades | Lombok |

## 📋 Prerrequisitos

- Java JDK 17+
- Maven 3.8+ (o usar `mvnw` incluido)
- PostgreSQL 17.2

## 🚀 Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/daniicg05/Syntia.git
cd Syntia
```

### 2. Crear la base de datos
```sql
CREATE USER syntia WITH PASSWORD 'syntia';
CREATE DATABASE syntia_db OWNER syntia;
GRANT ALL PRIVILEGES ON DATABASE syntia_db TO syntia;
```

### 3. Configurar `application.properties`
Verificar que `src/main/resources/application.properties` contenga:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/syntia_db
spring.datasource.username=syntia
spring.datasource.password=syntia
server.port=8080
```

### 4. Ejecutar
```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

## 📁 Documentación

Toda la documentación del proyecto se encuentra en la carpeta `docs/`:

| Documento | Descripción |
|-----------|-------------|
| [01-requisitos.md](docs/01-requisitos.md) | Requisitos funcionales y no funcionales |
| [02-plan-proyecto.md](docs/02-plan-proyecto.md) | Cronograma, hitos y recursos |
| [03-especificaciones-tecnicas.md](docs/03-especificaciones-tecnicas.md) | Arquitectura, seguridad (JWT/CORS), dependencias, configuración |
| [04-manual-desarrollo.md](docs/04-manual-desarrollo.md) | Guía de desarrollo, estándares, entorno local, deuda técnica |
| [05-changelog.md](docs/05-changelog.md) | Registro de cambios por hitos |
| [06-diagramas.md](docs/06-diagramas.md) | ER, clases UML, casos de uso, secuencias (JWT, recomendación) |

## 🔒 Configuración de la BD

| Parámetro | Valor |
|-----------|-------|
| Motor | PostgreSQL 17.2 |
| Puerto | `5432` |
| Base de datos | `syntia_db` |
| Usuario | `syntia` |
| Contraseña | `syntia` |
| Puerto Tomcat | `8080` |

## 👥 Equipo

Aplicación Web de Syntia del equipo 503.

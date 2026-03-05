# Diagramas Syntia – Versión revisada y completa (2026-03-05)

---

## 1. Modelo Entidad-Relación (ER)

```mermaid
erDiagram
    USUARIO {
        int id PK
        string email UK
        string password_hash
        enum rol "ADMIN | USUARIO"
        datetime creado_en
    }

    PERFIL {
        int id PK
        int usuario_id FK
        string sector
        string ubicacion
        string tipo_entidad
        string objetivos
        string necesidades_financiacion
        text descripcion_libre
    }

    PROYECTO {
        int id PK
        int usuario_id FK
        string nombre
        string sector
        string ubicacion
        text descripcion
    }

    CONVOCATORIA {
        int id PK
        string titulo
        string tipo
        string sector
        string ubicacion
        string url_oficial
        string fuente
        date fecha_cierre
    }

    RECOMENDACION {
        int id PK
        int proyecto_id FK
        int convocatoria_id FK
        int puntuacion
        text explicacion
        datetime generada_en
    }

    USUARIO ||--|| PERFIL : "tiene"
    USUARIO ||--o{ PROYECTO : "describe"
    PROYECTO ||--o{ RECOMENDACION : "genera"
    CONVOCATORIA ||--o{ RECOMENDACION : "aparece en"
```

---

## 2. Diagrama de Clases UML

> **Actualizado a 2026-03-05** — Refleja el estado real de la implementación (fases 1–6).

```mermaid
classDiagram
    class Rol {
        <<enumeration>>
        ADMIN
        USUARIO
    }

    class Usuario {
        +Long id
        +String email
        +String passwordHash
        +Rol rol
        +LocalDateTime creadoEn
    }

    class Perfil {
        +Long id
        +String sector
        +String ubicacion
        +String tipoEntidad
        +String objetivos
        +String necesidadesFinanciacion
        +String descripcionLibre
    }

    class Proyecto {
        +Long id
        +String nombre
        +String sector
        +String ubicacion
        +String descripcion
    }

    class Convocatoria {
        +Long id
        +String titulo
        +String tipo
        +String sector
        +String ubicacion
        +String urlOficial
        +String fuente
        +LocalDate fechaCierre
    }

    class Recomendacion {
        +Long id
        +int puntuacion
        +String explicacion
        +LocalDateTime generadaEn
    }

    class JwtService {
        -String secretKey
        -long expiration
        +generarToken(email, rol) String
        +validarToken(token, username) boolean
        +extraerUsername(token) String
        +extraerRol(token) String
    }

    class JwtAuthenticationFilter {
        -JwtService jwtService
        +doFilterInternal(request, response, chain) void
    }

    class CustomUserDetailsService {
        +loadUserByUsername(username) UserDetails
    }

    class UsuarioService {
        +registrar(email, password, rol) Usuario
        +buscarPorEmail(email) Optional~Usuario~
        +buscarPorId(id) Optional~Usuario~
        +obtenerTodos() List~Usuario~
        +eliminar(id) void
        +cambiarRol(id, nuevoRol) Usuario
    }

    class PerfilService {
        +tienePerfil(usuarioId) boolean
        +obtenerPerfil(usuarioId) Optional~Perfil~
        +crearPerfil(usuario, dto) Perfil
        +actualizarPerfil(usuarioId, dto) Perfil
        +toDTO(perfil) PerfilDTO
    }

    class ProyectoService {
        +obtenerProyectos(usuarioId) List~Proyecto~
        +obtenerPorId(id, usuarioId) Proyecto
        +crear(usuario, dto) Proyecto
        +actualizar(id, usuarioId, dto) Proyecto
        +eliminar(id, usuarioId) void
        +toDTO(proyecto) ProyectoDTO
    }

    class MotorMatchingService {
        +generarRecomendaciones(proyecto) List~Recomendacion~
    }

    class RecomendacionService {
        +obtenerPorProyecto(proyectoId) List~RecomendacionDTO~
        +contarPorProyecto(proyectoId) long
    }

    class ConvocatoriaService {
        +obtenerTodas() List~Convocatoria~
        +obtenerPorId(id) Convocatoria
        +crear(dto) Convocatoria
        +actualizar(id, dto) Convocatoria
        +eliminar(id) void
        +toDTO(convocatoria) ConvocatoriaDTO
    }

    class DashboardService {
        +obtenerTopRecomendacionesPorProyecto(usuarioId, topN) Map
        +obtenerRoadmap(usuarioId) List~RoadmapItem~
        +contarTotalRecomendaciones(usuarioId) long
    }

    Usuario --> Rol : tiene
    Usuario "1" --> "1" Perfil : tiene
    Usuario "1" --> "0..*" Proyecto : crea
    Proyecto "1" --> "0..*" Recomendacion : genera
    Recomendacion "0..*" --> "1" Convocatoria : referencia
    JwtAuthenticationFilter --> JwtService : usa
    CustomUserDetailsService --> UsuarioService : delega
    UsuarioService --> Usuario : gestiona
    PerfilService --> Perfil : gestiona
    ProyectoService --> Proyecto : gestiona
    MotorMatchingService --> Recomendacion : crea
    MotorMatchingService --> ConvocatoriaService : consulta
    RecomendacionService --> Recomendacion : lee
    ConvocatoriaService --> Convocatoria : gestiona
    DashboardService --> ProyectoService : usa
    DashboardService --> RecomendacionService : usa
```

---

## 3. Diagrama de Casos de Uso UML

```mermaid
flowchart TD
    UA([Usuario Final])
    AD([Administrador])

    subgraph Syntia
        UC1[Registrarse]
        UC2[Iniciar sesión]
        UC3[Cerrar sesión]
        UC4[Completar perfil]
        UC5[Crear proyecto]
        UC6[Ver recomendaciones]
        UC7[Ver roadmap estratégico]
        UC8[Filtrar oportunidades]
        UC9[Gestionar usuarios]
        UC10[Supervisar sistema]
        UC11[Configurar motor de IA]
    end

    UA --> UC1
    UA --> UC2
    UA --> UC3
    UA --> UC4
    UA --> UC5
    UA --> UC6
    UA --> UC7
    UA --> UC8
    AD --> UC2
    AD --> UC3
    AD --> UC9
    AD --> UC10
    AD --> UC11
```

---

## 4. Diagrama de Secuencia UML – Flujo principal de recomendación

```mermaid
sequenceDiagram
    actor Usuario
    participant Frontend
    participant Backend
    participant MotorMatching
    participant BDNS

    Usuario->>Frontend: Completa perfil y crea proyecto
    Frontend->>Backend: POST /perfil
    Backend->>Backend: Valida y almacena perfil
    Frontend->>Backend: POST /proyectos
    Backend->>Backend: Valida y almacena proyecto
    Backend->>MotorMatching: generarRecomendaciones(proyecto)
    MotorMatching->>BDNS: Recupera convocatorias
    BDNS-->>MotorMatching: Lista de convocatorias
    MotorMatching->>MotorMatching: Filtra y prioriza
    MotorMatching-->>Backend: Recomendaciones con explicaciones
    Backend->>Backend: Almacena recomendaciones
    Backend-->>Frontend: Recomendaciones priorizadas
    Frontend-->>Usuario: Dashboard con roadmap estratégico
```

---

## 5. Diagrama de Secuencia UML – Flujo de Autenticación JWT (API REST)

```mermaid
sequenceDiagram
    actor Cliente
    participant API as API REST
    participant AuthController
    participant UsuarioService
    participant JwtService
    participant BD as Base de Datos

    Note over Cliente, BD: 1. Login y obtención del token

    Cliente->>API: POST /api/auth/login {email, password}
    API->>AuthController: login(credentials)
    AuthController->>UsuarioService: autenticar(email, password)
    UsuarioService->>BD: findByEmail(email)
    BD-->>UsuarioService: Usuario
    UsuarioService->>UsuarioService: Verifica BCrypt(password, hash)
    UsuarioService-->>AuthController: Usuario autenticado
    AuthController->>JwtService: generarToken(usuario)
    JwtService->>JwtService: Firma HMAC-SHA256 (sub=email, rol, exp)
    JwtService-->>AuthController: JWT token
    AuthController-->>API: 200 OK {token, rol}
    API-->>Cliente: JWT token

    Note over Cliente, BD: 2. Petición autenticada con token

    Cliente->>API: GET /api/recomendaciones (Authorization: Bearer token)
    API->>JwtService: validarToken(token)
    JwtService->>JwtService: Verifica firma y expiración
    JwtService-->>API: Token válido (email, rol)
    API->>AuthController: SecurityContext establecido
    AuthController->>BD: Consulta datos
    BD-->>AuthController: Resultados
    AuthController-->>API: 200 OK {datos}
    API-->>Cliente: Respuesta JSON
```

---

## 6. Diagrama de Secuencia UML – Flujo de Login por Formulario (Thymeleaf)

```mermaid
sequenceDiagram
    actor Usuario
    participant Navegador
    participant SecurityFilter as Spring Security Filter
    participant AuthController
    participant UserDetailsService
    participant BD as Base de Datos

    Usuario->>Navegador: Accede a /login
    Navegador->>AuthController: GET /login
    AuthController-->>Navegador: login.html (formulario)
    Usuario->>Navegador: Introduce email y contraseña
    Navegador->>SecurityFilter: POST /login {email, password}
    SecurityFilter->>UserDetailsService: loadUserByUsername(email)
    UserDetailsService->>BD: findByEmail(email)
    BD-->>UserDetailsService: Usuario
    UserDetailsService-->>SecurityFilter: UserDetails
    SecurityFilter->>SecurityFilter: Verifica BCrypt(password, hash)
    SecurityFilter->>SecurityFilter: Crea sesión HTTP + SecurityContext

    alt Rol = ADMIN
        SecurityFilter-->>Navegador: Redirect /admin/dashboard
    else Rol = USUARIO
        SecurityFilter-->>Navegador: Redirect /usuario/dashboard
    end

    Navegador-->>Usuario: Dashboard según rol
```


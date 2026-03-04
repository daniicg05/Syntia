# Diagramas Syntia – Versión revisada y completa (2026-03-04)

---

## 1. Modelo Entidad-Relación (ER)

```mermaid
erDiagram
    USUARIO {
        int id PK
        string email
        string password_hash
        string rol
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

```mermaid
classDiagram
    class Usuario {
        +int id
        +String email
        +String passwordHash
        +String rol
        +DateTime creadoEn
    }

    class Perfil {
        +int id
        +String sector
        +String ubicacion
        +String tipoEntidad
        +String objetivos
        +String necesidadesFinanciacion
        +String descripcionLibre
    }

    class Proyecto {
        +int id
        +String nombre
        +String sector
        +String ubicacion
        +String descripcion
    }

    class Convocatoria {
        +int id
        +String titulo
        +String tipo
        +String sector
        +String ubicacion
        +String urlOficial
        +String fuente
        +Date fechaCierre
    }

    class Recomendacion {
        +int id
        +int puntuacion
        +String explicacion
        +DateTime generadaEn
    }

    class UsuarioService {
        +registrar(email, password) Usuario
        +login(email, password) String
        +gestionarUsuarios() List~Usuario~
    }

    class PerfilService {
        +guardarPerfil(usuarioId, perfil) Perfil
        +obtenerPerfil(usuarioId) Perfil
    }

    class ProyectoService {
        +crearProyecto(usuarioId, proyecto) Proyecto
        +obtenerProyectos(usuarioId) List~Proyecto~
    }

    class MotorMatching {
        +generarRecomendaciones(proyecto) List~Recomendacion~
        +priorizarOportunidades(proyecto, convocatorias) List~Recomendacion~
    }

    class ConvocatoriaService {
        +recuperarConvocatorias() List~Convocatoria~
        +filtrarPorProyecto(proyecto) List~Convocatoria~
    }

    Usuario "1" --> "1" Perfil
    Usuario "1" --> "0..*" Proyecto
    Proyecto "1" --> "0..*" Recomendacion
    Recomendacion "0..*" --> "1" Convocatoria
    UsuarioService --> Usuario
    PerfilService --> Perfil
    ProyectoService --> Proyecto
    MotorMatching --> Recomendacion
    MotorMatching --> ConvocatoriaService
    ConvocatoriaService --> Convocatoria
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


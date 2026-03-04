# Registro de Cambios (Changelog): Syntia

Formato de cada entrada:
- **Fecha**
- **Versión** (MAJOR.MINOR.PATCH)
- **Cambios realizados**
- **Autor**

---

## [1.0.0] – 2026-03-04

### Cambios Realizados
- Desarrollo inicial del MVP de Syntia.
- Implementación de registro, autenticación, captura de perfil y motor de priorización.
- Integración con la API BDNS y dashboard interactivo.

**Autor(es):** Carlos (Backend/Frontend)

---

## [2.0.0] – 2026-03-04

### Hito 2: Entrega del Primer Prototipo (MVP)

En este segundo hito, se procede a la presentación y entrega del primer prototipo funcional de la aplicación, concebido como Producto Mínimo Viable (MVP). El objetivo principal de esta fase consiste en ofrecer una versión inicial que integre las funcionalidades esenciales, lo que permitirá validar la viabilidad técnica y recolectar retroalimentación temprana de los interesados.

#### Alcance y Objetivos de la Entrega

1. **Funcionalidades clave:** Incluirá las características imprescindibles para demostrar el valor principal de la aplicación, de manera que se pueda verificar su correcto comportamiento y usabilidad.
2. **Diseño preliminar:** Se mostrará un boceto de la interfaz de usuario y la estructura básica de navegación; no se contemplará el refinamiento estético completo, pero sí se garantizará la coherencia y la consistencia con los lineamientos de diseño establecidos.
3. **Pruebas y validaciones iniciales:** Se llevarán a cabo pruebas funcionales básicas para detectar errores críticos. Estas validaciones tempranas se centrarán en asegurar la estabilidad mínima que permita avanzar hacia la siguiente etapa de desarrollo.

#### Resultados Esperados

- Una mejor comprensión de los requerimientos reales del proyecto gracias al análisis de las reacciones e impresiones de los usuarios y partes interesadas.
- Un documento de retroalimentación que, en función de los hallazgos, permita ajustar el alcance, priorizar mejoras y definir pasos concretos para el desarrollo subsecuente.
- El incremento de la confianza del equipo en la dirección técnica y funcional del proyecto, validando supuestos iniciales y reduciendo riesgos a mediano plazo.

> Con esta entrega, se sientan las bases para la siguiente fase de desarrollo, donde se afinarán los componentes clave del MVP y se incorporarán las modificaciones resultantes de la retroalimentación recibida.

**Autor(es):** Carlos (Backend/Frontend)

---

## [3.0.0] – 2026-03-04

### Hito 3: Documentación Técnica y Arquitectura del Sistema

#### Alcance y Objetivos de la Entrega

1. **Modelo Entidad-Relación (ER)**
   - Se incluirá una representación detallada de las entidades del dominio y sus relaciones, reflejando la estructura de la base de datos y mostrando las claves primarias, claves foráneas y cardinalidades.
   - Permitirá una visión clara de los datos y sus dependencias, sirviendo como referencia fundamental para el equipo de desarrollo y para el mantenimiento del proyecto.

2. **Diagrama de Clases UML**
   - Se entregará un diagrama de clases que muestre las principales clases y sus interacciones, contemplando atributos, métodos y relaciones (herencia, asociaciones, dependencias, etc.).
   - Este diagrama proporcionará un entendimiento claro de la lógica de negocio y de la forma en que el sistema está estructurado a nivel de objetos.

3. **Generación de JavaDoc**
   - Documentación del código fuente Java, describiendo la funcionalidad de clases, interfaces y métodos, con la finalidad de aclarar la intención de cada componente y su uso.
   - Garantiza la consistencia entre la lógica de negocio y la documentación, y facilita la incorporación de nuevos integrantes al proyecto o la ampliación de funcionalidades.

4. **Documentación de la API**
   - Se incluirá un documento o portal de referencia con la descripción de los endpoints (rutas, parámetros, formatos de entrada/salida) y los códigos de estado correspondientes.
   - Asegurará que otros equipos o sistemas externos puedan consumir la API de forma correcta y segura, permitiendo su integración fluida.

#### Resultados Esperados

- **Visión integral de la arquitectura:** La entrega de estos componentes facilitará la comprensión de la estructura del sistema, tanto a nivel de datos como de la lógica de negocio.
- **Referencia de desarrollo y mantenimiento:** El conjunto de diagramas y la documentación generada constituirán la base para la continuidad del proyecto, haciendo más eficiente la comunicación entre miembros del equipo.
- **Alineación de requisitos técnicos y funcionales:** Al disponer de modelos y documentación detallados, se reducirá la posibilidad de ambigüedades en la implementación posterior o de inconsistencias respecto a las especificaciones establecidas.
- **Preparación para la siguiente fase:** Con la arquitectura y la documentación técnica claramente definidas, será posible abordar con mayor solidez las etapas finales de desarrollo y refinamiento de la aplicación.

**Autor(es):** Carlos (Backend/Frontend)

---

## [4.0.0] – 2026-03-04

### Hito 4: Integración, Validación y Preparación para el Despliegue Final

#### Alcance y Objetivos de la Entrega

1. **Integración de componentes**
   - **Consolidación del código:** Verificar que los distintos módulos (front-end, back-end, servicios internos) se integren correctamente tras los cambios introducidos en el Hito 3.
   - **Pruebas de compatibilidad:** Asegurarse de que no existan conflictos de dependencias (por ejemplo, versiones de librerías, compatibilidad con frameworks) ni problemas en la configuración de entornos.

2. **Pruebas de calidad y rendimiento**
   - **Pruebas funcionales:** Completar un ciclo de testing que valide escenarios de uso, flujos de navegación y casos de error o excepciones.
   - **Pruebas de rendimiento:** Ejecutar, en la medida de lo posible, cargas de prueba o test de estrés para confirmar que la aplicación responde adecuadamente ante un volumen representativo de usuarios o peticiones.
   - **Pruebas de integración:** Comprobar que los endpoints o controladores funcionan adecuadamente cuando interactúan con bases de datos, servicios externos o APIs de terceros (si aplica).

3. **Revisiones de seguridad y buenas prácticas**
   - **Autenticación y autorización:** Verificar las restricciones de acceso a cada módulo o ruta, confirmando que la aplicación respeta los permisos de usuario.
   - **Revisión de código (code review):** Evaluar la calidad y la seguridad del código, asegurando que no se expongan datos sensibles o credenciales.
   - **Buenas prácticas de desarrollo:** Revisar patrones de diseño, estructura de paquetes y consistencia en la nomenclatura de clases y métodos.

4. **Pulido de la interfaz y experiencia de usuario (UX)**
   - **Corrección de detalles visuales:** Ajustar estilos, consistencia de elementos de la interfaz y uso correcto de la paleta de colores.
   - **Usabilidad:** Revisar la facilidad de uso, el orden de los pasos para el usuario y la claridad de la información presentada.
   - **Retroalimentación parcial de usuarios:** Cuando sea factible, involucrar un grupo reducido de usuarios (estudiantes o compañeros) para obtener comentarios tempranos sobre la navegación y la interacción.

5. **Actualización de la documentación**
   - **Refinamiento de manuales:** Actualizar aquellos aspectos del ER, diagrama de clases, o JavaDoc que hayan cambiado durante esta fase de integración.
   - **Guía de instalación / despliegue:** Crear o perfeccionar un apartado que explique cómo desplegar la aplicación en un entorno de prueba o producción.
   - **Registro de incidencias y soluciones:** Llevar un listado de problemas detectados y resueltos durante esta etapa, de modo que se puedan revisar en el futuro para mantenimiento o mejoras.
   - **Ampliación y finalización del manual de usuario o guías de operación**, redactadas en un lenguaje claro y accesible, que faciliten la adopción de la aplicación.

6. **Plan de despliegue y próximos pasos**
   - **Plan para el Hito 5 (final):** Con la integración y validación realizadas, trazar la ruta que garantice la entrega final a tiempo:
     - Listar las tareas pendientes.
     - Establecer un orden de prioridades.
     - Definir responsables y fechas para cada tarea.

**Autor(es):** Carlos (Backend/Frontend)

---

## [5.0.0] – 2026-03-04

### Hito 5: Entrega Final del Proyecto

#### Alcance y Objetivos de la Entrega

1. **Funcionalidades completas**
   - La aplicación deberá incluir todas las características previstas en el alcance del proyecto, abarcando tanto las esenciales como aquellas adicionales que agreguen valor y satisfacción al usuario.
   - Integración de módulos y servicios requeridos para su operatividad, con la debida vinculación de bases de datos, API o recursos externos.

2. **Refinamiento de la experiencia de usuario (UX/UI)**
   - Se incorporarán los ajustes de diseño e interacción necesarios para asegurar un uso fluido y coherente.
   - El diseño estético, la disposición de elementos y la navegación dentro de la aplicación se presentarán con el nivel de pulido deseado, reflejando la identidad visual del proyecto.

3. **Pruebas finales y aseguramiento de la calidad (QA)**
   - Se ejecutarán pruebas exhaustivas (funcionales, de rendimiento, de usabilidad, etc.) para garantizar un producto estable, minimizando el número de errores detectables en producción.
   - Validaciones cruzadas con el documento de requisitos y con la retroalimentación obtenida en hitos anteriores, a fin de corroborar que se cumplen los objetivos propuestos.

4. **Documentación completa**
   - Entrega de la documentación técnica que incluya, entre otros, arquitectura, modelos de datos, guías de instalación y configuración, así como instrucciones de mantenimiento y/o deployment (los dos últimos solo si aplica).
   - Manual de usuario o guías de operación, redactadas en un lenguaje claro y accesible, que faciliten la adopción de la aplicación.

5. **Configuraciones comunes para la corrección**

   | Parámetro | Valor |
   |-----------|-------|
   | Nombre de la base de datos | `syntia_db` |
   | Usuario de base de datos | `syntia` |
   | Contraseña de base de datos | `syntia` |
   | Puerto Tomcat | `8080` |
   | Motor de base de datos | PostgreSQL |
   | Puerto PostgreSQL | `5432` |

**Autor(es):** Carlos (Backend/Frontend)



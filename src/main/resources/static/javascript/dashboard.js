/**
 * dashboard.js
 * Mejoras de interactividad del dashboard:
 * - Añade etiqueta de días restantes a cada fecha de cierre en el roadmap.
 *
 * Nota técnica: Thymeleaf procesa y elimina los atributos th:* antes de
 * servir el HTML al cliente, por lo que los selectores deben basarse en
 * clases CSS o en el contenido del texto renderizado, nunca en th:*.
 */
document.addEventListener('DOMContentLoaded', function () {

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    // Buscar todos los elementos con clase text-danger, text-warning o text-muted
    // que contengan una fecha en formato dd/MM/yyyy dentro del roadmap
    document.querySelectorAll('.list-group-item').forEach(function (item) {
        // Solo ítems del roadmap (tienen borde izquierdo de color)
        const tieneClaseRoadmap = ['roadmap-urgente', 'roadmap-proximo', 'roadmap-normal']
            .some(cls => item.classList.contains(cls));
        if (!tieneClaseRoadmap) return;

        // Buscar todos los spans con texto de fecha dd/MM/yyyy
        item.querySelectorAll('span').forEach(function (span) {
            const match = span.textContent.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
            if (!match) return;

            const fecha = new Date(
                parseInt(match[3]),
                parseInt(match[2]) - 1,
                parseInt(match[1])
            );
            const diff = Math.ceil((fecha - hoy) / (1000 * 60 * 60 * 24));

            const etiqueta = document.createElement('span');
            etiqueta.className = 'badge ms-2 ' +
                (diff <= 0  ? 'bg-danger' :
                 diff <= 15 ? 'bg-danger' :
                 diff <= 30 ? 'bg-warning text-dark' : 'bg-secondary');
            etiqueta.textContent = diff <= 0  ? '¡Vencida!' :
                                   diff === 1 ? '1 día' :
                                   diff + ' días';
            span.parentNode.insertBefore(etiqueta, span.nextSibling);
        });
    });
});




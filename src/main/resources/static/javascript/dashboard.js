/**
 * dashboard.js
 * Mejoras de interactividad del dashboard:
 * - Resalta automáticamente las convocatorias urgentes (< 15 días)
 * - Muestra un contador de días restantes en el roadmap
 */
document.addEventListener('DOMContentLoaded', function () {

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    // Añadir etiqueta de días restantes a cada ítem del roadmap
    document.querySelectorAll('.list-group-item[class*="roadmap-"]').forEach(function (item) {
        const fechaEl = item.querySelector('[th\\:text*="fechaCierre"], span[class*="text-danger"], span[class*="text-warning"], span[class*="text-muted"]');
        // Buscar texto con formato dd/MM/yyyy dentro del item
        const textos = item.querySelectorAll('span');
        textos.forEach(function (span) {
            const match = span.textContent.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
            if (match) {
                const fecha = new Date(parseInt(match[3]), parseInt(match[2]) - 1, parseInt(match[1]));
                const diff = Math.ceil((fecha - hoy) / (1000 * 60 * 60 * 24));
                const etiqueta = document.createElement('span');
                etiqueta.className = 'badge ms-2 ' +
                    (diff <= 15 ? 'bg-danger' : diff <= 30 ? 'bg-warning text-dark' : 'bg-secondary');
                etiqueta.textContent = diff === 0 ? '¡Hoy!' :
                                       diff === 1 ? '1 día' :
                                       diff + ' días';
                span.parentNode.insertBefore(etiqueta, span.nextSibling);
            }
        });
    });
});


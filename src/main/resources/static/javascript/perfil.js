/**
 * perfil.js
 * Validaciones y utilidades para el formulario de perfil:
 * - Contador de caracteres en tiempo real para textareas
 * - Bloqueo de envío si se supera el límite
 */
document.addEventListener('DOMContentLoaded', function () {

    const limites = [
        { id: 'objetivos',              max: 500 },
        { id: 'necesidadesFinanciacion', max: 500 },
        { id: 'descripcionLibre',        max: 2000 }
    ];

    limites.forEach(function ({ id, max }) {
        const campo = document.getElementById(id);
        if (!campo) return;

        // Crear el contador debajo del textarea
        const contador = document.createElement('div');
        contador.className = 'form-text text-end';
        campo.parentNode.insertBefore(contador, campo.nextSibling);

        function actualizar() {
            const restantes = max - campo.value.length;
            contador.textContent = campo.value.length + ' / ' + max + ' caracteres';
            if (restantes < 0) {
                contador.classList.add('text-danger');
                contador.classList.remove('text-muted');
                campo.classList.add('is-invalid');
            } else if (restantes < max * 0.1) {
                contador.classList.add('text-warning');
                contador.classList.remove('text-danger', 'text-muted');
                campo.classList.remove('is-invalid');
            } else {
                contador.classList.remove('text-danger', 'text-warning');
                contador.classList.add('text-muted');
                campo.classList.remove('is-invalid');
            }
        }

        campo.addEventListener('input', actualizar);
        actualizar(); // inicializar si hay datos precargados
    });

    // Bloquear envío si algún campo supera el límite
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function (e) {
            const hayError = limites.some(function ({ id, max }) {
                const campo = document.getElementById(id);
                return campo && campo.value.length > max;
            });
            if (hayError) {
                e.preventDefault();
            }
        });
    }
});


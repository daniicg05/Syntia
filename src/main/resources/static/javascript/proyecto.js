/**
 * proyecto.js
 * Validaciones del formulario de proyecto:
 * - Nombre obligatorio y máximo 150 caracteres
 * - Descripción máximo 2000 caracteres con contador en tiempo real
 */
document.addEventListener('DOMContentLoaded', function () {

    const form = document.getElementById('formProyecto');
    if (!form) return;

    const nombre = document.getElementById('nombre');
    const descripcion = document.getElementById('descripcion');
    const errorNombre = document.getElementById('jsErrorNombre');

    // --- Contador descripción ---
    if (descripcion) {
        const MAX_DESC = 2000;
        const contador = document.createElement('div');
        contador.className = 'form-text text-end';
        descripcion.parentNode.insertBefore(contador, descripcion.nextSibling);

        function actualizarContador() {
            const len = descripcion.value.length;
            contador.textContent = len + ' / ' + MAX_DESC + ' caracteres';
            if (len > MAX_DESC) {
                contador.classList.add('text-danger');
                descripcion.classList.add('is-invalid');
            } else if (len > MAX_DESC * 0.9) {
                contador.classList.remove('text-danger');
                contador.classList.add('text-warning');
                descripcion.classList.remove('is-invalid');
            } else {
                contador.classList.remove('text-danger', 'text-warning');
                descripcion.classList.remove('is-invalid');
            }
        }
        descripcion.addEventListener('input', actualizarContador);
        actualizarContador();
    }

    // --- Validación nombre ---
    function validarNombre() {
        if (!nombre.value.trim()) {
            nombre.classList.add('is-invalid');
            errorNombre.textContent = 'El nombre del proyecto es obligatorio.';
            return false;
        }
        if (nombre.value.length > 150) {
            nombre.classList.add('is-invalid');
            errorNombre.textContent = 'El nombre no puede superar los 150 caracteres.';
            return false;
        }
        nombre.classList.remove('is-invalid');
        errorNombre.textContent = '';
        return true;
    }

    nombre.addEventListener('input', validarNombre);

    // --- Bloquear envío si hay errores ---
    form.addEventListener('submit', function (e) {
        const okNombre = validarNombre();
        const descLen = descripcion ? descripcion.value.length : 0;
        if (!okNombre || descLen > 2000) {
            e.preventDefault();
        }
    });
});


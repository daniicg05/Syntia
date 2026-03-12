/**
 * registro.js
 * Validación del formulario de registro:
 * - Email con formato válido (contiene @ y dominio)
 * - Contraseña mínimo 4 caracteres
 * - Confirmación de contraseña idéntica a la contraseña
 */
document.addEventListener('DOMContentLoaded', function () {

    const form = document.getElementById('formRegistro');
    const email = document.getElementById('email');
    const password = document.getElementById('password');
    const confirmar = document.getElementById('confirmarPassword');
    const errorEmail = document.getElementById('jsErrorEmail');
    const errorPassword = document.getElementById('jsErrorPassword');
    const errorConfirmar = document.getElementById('jsErrorConfirmar');

    const MIN = 4;
    const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    function validarEmail() {
        if (email.value.length > 0 && !EMAIL_REGEX.test(email.value)) {
            email.classList.add('is-invalid');
            errorEmail.textContent = 'Introduce un email válido (ej: nombre@dominio.com).';
            return false;
        }
        email.classList.remove('is-invalid');
        errorEmail.textContent = '';
        return true;
    }

    function validarPassword() {
        if (password.value.length > 0 && password.value.length < MIN) {
            password.classList.add('is-invalid');
            errorPassword.textContent = 'La contraseña debe tener al menos ' + MIN + ' caracteres.';
            return false;
        }
        password.classList.remove('is-invalid');
        errorPassword.textContent = '';
        return true;
    }

    function validarConfirmar() {
        if (confirmar.value.length > 0 && confirmar.value !== password.value) {
            confirmar.classList.add('is-invalid');
            errorConfirmar.textContent = 'Las contraseñas no coinciden.';
            return false;
        }
        confirmar.classList.remove('is-invalid');
        errorConfirmar.textContent = '';
        return true;
    }

    email.addEventListener('input', validarEmail);

    password.addEventListener('input', function () {
        validarPassword();
        if (confirmar.value.length > 0) validarConfirmar();
    });

    confirmar.addEventListener('input', validarConfirmar);

    form.addEventListener('submit', function (e) {
        const okEmail = validarEmail();
        const okPassword = validarPassword();
        const okConfirmar = validarConfirmar();
        if (!okEmail || !okPassword || !okConfirmar) {
            e.preventDefault();
        }
    });
});


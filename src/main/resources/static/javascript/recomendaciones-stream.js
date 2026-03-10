/**
 * Syntia — recomendaciones-stream.js
 * Maneja la conexión SSE para recibir recomendaciones en tiempo real.
 */
(function () {
    'use strict';

    /**
     * Inicia el análisis con IA usando Server-Sent Events.
     * @param {number} proyectoId - ID del proyecto a analizar
     */
    window.iniciarAnalisisStream = function (proyectoId) {
        const btn = document.getElementById('btnAnalizar');
        const btnSincrono = document.getElementById('formAnalizar');
        const panel = document.getElementById('panelProgreso');
        const estado = document.getElementById('estadoTexto');
        const barra = document.getElementById('barraProgreso');
        const detalle = document.getElementById('progresoDetalle');
        const resultados = document.getElementById('resultadosStream');
        const contadorResultados = document.getElementById('contadorResultados');

        if (!btn || !panel) return;

        // Ocultar el formulario síncrono
        if (btnSincrono) btnSincrono.style.display = 'none';

        // UI: deshabilitar botón, mostrar panel de progreso
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Analizando...';
        panel.style.display = 'block';
        panel.classList.add('animate__fadeIn');
        if (resultados) resultados.innerHTML = '';
        if (contadorResultados) contadorResultados.textContent = '0';

        // Resetear barra
        barra.style.width = '0%';
        barra.textContent = '';
        barra.className = 'progress-bar progress-bar-striped progress-bar-animated bg-primary';

        // Abrir conexión SSE
        var source = new EventSource(
            '/usuario/proyectos/' + proyectoId + '/recomendaciones/generar-stream'
        );

        var totalEncontradas = 0;

        // ── Evento: estado (mensajes de texto) ──
        source.addEventListener('estado', function (e) {
            var texto = e.data;
            try { texto = JSON.parse(texto); } catch (ignore) {}
            if (estado) estado.textContent = texto;
        });

        // ── Evento: keywords generadas ──
        source.addEventListener('keywords', function (e) {
            try {
                var data = JSON.parse(e.data);
                if (detalle) {
                    detalle.textContent = 'Keywords de búsqueda: ' + data.keywords.join(', ');
                }
            } catch (ignore) {}
        });

        // ── Evento: resultados de búsqueda BDNS ──
        source.addEventListener('busqueda', function (e) {
            try {
                var data = JSON.parse(e.data);
                if (detalle) {
                    detalle.textContent = data.candidatas + ' convocatorias candidatas encontradas en BDNS';
                }
            } catch (ignore) {}
        });

        // ── Evento: progreso de evaluación ──
        source.addEventListener('progreso', function (e) {
            try {
                var data = JSON.parse(e.data);
                var pct = Math.round((data.actual / data.total) * 100);
                barra.style.width = pct + '%';
                barra.textContent = pct + '%';
                var titulo = data.titulo || '';
                if (titulo.length > 70) titulo = titulo.substring(0, 70) + '...';
                if (detalle) {
                    detalle.textContent = 'Evaluando ' + data.actual + '/' + data.total + ': ' + titulo;
                }
            } catch (ignore) {}
        });

        // ── Evento: resultado (nueva recomendación encontrada) ──
        source.addEventListener('resultado', function (e) {
            try {
                var rec = JSON.parse(e.data);
                totalEncontradas++;
                if (contadorResultados) {
                    contadorResultados.textContent = totalEncontradas;
                }

                if (resultados) {
                    var card = crearTarjetaRecomendacion(rec);
                    resultados.insertAdjacentHTML('beforeend', card);
                    // Efecto de aparición suave
                    var lastCard = resultados.lastElementChild;
                    if (lastCard) {
                        lastCard.style.opacity = '0';
                        lastCard.style.transform = 'translateY(20px)';
                        requestAnimationFrame(function () {
                            lastCard.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
                            lastCard.style.opacity = '1';
                            lastCard.style.transform = 'translateY(0)';
                        });
                    }
                }
            } catch (err) {
                console.warn('Error parseando resultado SSE:', err);
            }
        });

        // ── Evento: completado ──
        source.addEventListener('completado', function (e) {
            source.close();
            try {
                var data = JSON.parse(e.data);
                barra.style.width = '100%';
                barra.textContent = '100%';
                barra.classList.remove('progress-bar-animated', 'bg-primary');
                barra.classList.add('bg-success');

                var msg = '✅ Análisis completado: ' + data.totalRecomendaciones +
                    ' recomendaciones de ' + data.totalEvaluadas + ' convocatorias evaluadas.';
                if (data.errores > 0) {
                    msg += ' (' + data.errores + ' errores de IA)';
                }
                if (estado) estado.textContent = msg;
                if (detalle) detalle.textContent = '';
            } catch (ignore) {}

            btn.disabled = false;
            btn.innerHTML = '🤖 Analizar con IA';

            // Recargar tras 2.5s para mostrar vista completa con filtros y modales
            setTimeout(function () {
                window.location.reload();
            }, 2500);
        });

        // ── Evento: error del servidor ──
        source.addEventListener('error_custom', function (e) {
            source.close();
            var msg = 'Error del servidor';
            try { msg = JSON.parse(e.data); } catch (ignore) {}
            if (estado) estado.textContent = '❌ ' + msg;
            barra.classList.remove('progress-bar-animated', 'bg-primary');
            barra.classList.add('bg-danger');
            barra.style.width = '100%';
            btn.disabled = false;
            btn.innerHTML = '🤖 Analizar con IA';
        });

        // ── Error de conexión ──
        source.onerror = function () {
            // EventSource reintenta automáticamente, pero si ya terminó o dio error real, cerramos
            if (source.readyState === EventSource.CLOSED) return;
            source.close();
            if (estado) estado.textContent = '⚠️ Se perdió la conexión. Inténtalo de nuevo.';
            barra.classList.remove('progress-bar-animated', 'bg-primary');
            barra.classList.add('bg-warning');
            btn.disabled = false;
            btn.innerHTML = '🤖 Analizar con IA';
        };
    };

    /**
     * Crea el HTML de una tarjeta de recomendación para inserción dinámica.
     * Incluye botón de guía de solicitud con flujo visual.
     */
    function crearTarjetaRecomendacion(rec) {
        var claseP = rec.puntuacion >= 70 ? 'puntuacion-alta' :
            (rec.puntuacion >= 40 ? 'puntuacion-media' : 'puntuacion-baja');
        var claseBarra = rec.puntuacion >= 70 ? 'bg-success' :
            (rec.puntuacion >= 40 ? 'bg-warning' : 'bg-secondary');

        var titulo = escapeHtml(rec.titulo || 'Sin título');
        var explicacion = escapeHtml(rec.explicacion || '');
        var tipo = rec.tipo ? '<span class="badge bg-primary me-1">' + escapeHtml(rec.tipo) + '</span>' : '';
        var sector = rec.sector ? '<span class="badge bg-secondary me-1">' + escapeHtml(rec.sector) + '</span>' : '';
        var ubicacion = rec.ubicacion ? '<span class="badge bg-info text-dark me-1">' + escapeHtml(rec.ubicacion) + '</span>' : '';
        var fuente = rec.fuente ? '<span class="badge bg-light text-dark border">' + escapeHtml(rec.fuente) + '</span>' : '';
        var urlBtn = rec.urlOficial
            ? '<a href="' + escapeHtml(rec.urlOficial) + '" target="_blank" rel="noopener noreferrer" class="btn btn-outline-primary btn-sm">Ver convocatoria oficial ↗</a>'
            : '';

        // Guardar datos de la recomendación en un atributo data para el botón de guía
        var recId = 'streamRec_' + Date.now() + '_' + Math.random().toString(36).substr(2,5);

        // Botón de guía de solicitud
        var guiaBtn = '<button type="button" class="btn btn-success btn-sm" ' +
            'onclick="abrirGuiaStream(window[\'' + recId + '\'])">' +
            '📋 Ver guía de solicitud</button>';

        // Guardar el objeto rec en una variable global temporal
        window[recId] = rec;

        return '<div class="col-12">' +
            '<div class="card shadow-sm border-0 border-start border-3 border-success">' +
            '<div class="card-body d-flex justify-content-between align-items-start flex-wrap gap-2">' +
            '<div class="flex-grow-1">' +
            '<h6 class="card-title mb-1">' + titulo + '</h6>' +
            '<div class="mb-2">' + tipo + sector + ubicacion + fuente +
            '<span class="badge bg-success ms-1">🤖 IA</span></div>' +
            '<p class="text-muted small mb-1">' + explicacion + '</p>' +
            '<div class="d-flex flex-wrap gap-2 mt-2">' + guiaBtn + urlBtn + '</div>' +
            '</div>' +
            '<div class="text-center ms-3" style="min-width:70px;">' +
            '<div class="fs-3 fw-bold ' + claseP + '">' + rec.puntuacion + '</div>' +
            '<div class="small text-muted">/ 100</div>' +
            '<div class="progress mt-1" style="height:5px;">' +
            '<div class="progress-bar ' + claseBarra + '" style="width:' + rec.puntuacion + '%"></div>' +
            '</div></div></div></div></div>';
    }

    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

})();


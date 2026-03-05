-- =============================================================
-- Syntia MVP – Script de datos de prueba
-- Base de datos: syntia_db
-- Puerto PostgreSQL: 5432
-- =============================================================
-- IMPORTANTE: ejecutar DESPUÉS de que Hibernate haya creado
-- las tablas con ddl-auto=update (primer arranque).
-- Las contraseñas son BCrypt de "admin1234" y "user1234".
-- =============================================================

-- ── USUARIOS ─────────────────────────────────────────────────
INSERT INTO usuarios (email, password_hash, rol, creado_en)
VALUES
  ('admin@syntia.com',  '$2a$10$N.qOuiHrRbwEi6hM7ATfCeP5wT6Bnq9B2XY9vOwG0VJqOylBnWTRe', 'ADMIN',   NOW()),
  ('usuario@syntia.com','$2a$10$jHdqH7GvYqZoLFq2kPmVdOKdNuXMgxr7YNEi2tL5Br8JVeLzCz6by', 'USUARIO', NOW())
ON CONFLICT (email) DO NOTHING;

-- ── PERFILES ─────────────────────────────────────────────────
INSERT INTO perfiles (usuario_id, sector, ubicacion, tipo_entidad, objetivos, necesidades_financiacion, descripcion_libre)
SELECT id, 'Tecnología', 'Madrid', 'PYME',
       'Digitalizar procesos internos y expandir el producto al mercado europeo',
       'Financiación para I+D y contratación de personal técnico',
       'Startup tecnológica especializada en software de gestión para el sector retail. Buscamos subvenciones para acelerar el desarrollo de nuestra plataforma SaaS.'
FROM usuarios WHERE email = 'usuario@syntia.com'
ON CONFLICT DO NOTHING;

-- ── CONVOCATORIAS DE PRUEBA ───────────────────────────────────
INSERT INTO convocatorias (titulo, tipo, sector, ubicacion, url_oficial, fuente, fecha_cierre) VALUES
  ('Ayudas para la digitalización de PYMES – Kit Digital 2026',
   'Ayuda', 'Tecnología', 'Nacional',
   'https://www.acelerapyme.gob.es/kit-digital',
   'Red.es', '2026-06-30'),

  ('Subvenciones I+D+i para empresas tecnológicas – CDTI',
   'Subvención', 'Tecnología', 'Nacional',
   'https://www.cdti.es/ayudas/proyectos-de-id',
   'CDTI', '2026-05-15'),

  ('Convocatoria Horizon Europe – Cluster 4: Digital',
   'Europeo', 'Tecnología', 'Nacional',
   'https://ec.europa.eu/info/funding-tenders/opportunities',
   'UE / Horizon Europe', '2026-09-01'),

  ('Programa de apoyo a la internacionalización – ICEX',
   'Ayuda', 'Servicios', 'Nacional',
   'https://www.icex.es/es/navegacion-principal/exporta-con-icex',
   'ICEX', '2026-07-31'),

  ('Subvenciones para proyectos agroalimentarios – FEADER',
   'Subvención', 'Agricultura', 'Nacional',
   'https://www.mapa.gob.es/es/desarrollo-rural/temas/feader',
   'MAPA', '2026-04-30'),

  ('Ayudas para contratación de jóvenes investigadores – Madrid',
   'Ayuda', 'Tecnología', 'Madrid',
   'https://www.comunidad.madrid/servicios/empleo',
   'Comunidad de Madrid', '2026-08-15'),

  ('Línea de financiación para startups – Enisa Jóvenes Emprendedores',
   'Préstamo', 'Tecnología', 'Nacional',
   'https://www.enisa.es/es/financiacion',
   'ENISA', '2026-12-31'),

  ('Subvenciones para proyectos culturales y creativos – Europa Creativa',
   'Europeo', 'Cultura', 'Nacional',
   'https://www.europacreativa.eu',
   'UE / Europa Creativa', '2026-10-15')

ON CONFLICT DO NOTHING;

-- ── PROYECTO DE PRUEBA ────────────────────────────────────────
INSERT INTO proyectos (usuario_id, nombre, sector, ubicacion, descripcion)
SELECT u.id,
       'Plataforma SaaS de gestión retail',
       'Tecnología',
       'Madrid',
       'Desarrollo de una plataforma de software como servicio (SaaS) para digitalizar la gestión de inventario, ventas y clientes en el sector retail. El proyecto requiere financiación para I+D y expansión al mercado europeo.'
FROM usuarios u WHERE u.email = 'usuario@syntia.com'
ON CONFLICT DO NOTHING;


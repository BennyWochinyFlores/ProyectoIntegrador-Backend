-- Datos de prueba - Centro de Estimulacion Integral

INSERT INTO especialidad (nombre, duracion_minutos) VALUES
('Estimulación temprana', 45),
('Terapia de lenguaje', 60),
('Terapia ocupacional', 50);

INSERT INTO usuario (nombre, email, password, rol) VALUES
('María Apoderada', 'apoderado@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'APODERADO'),
('Dr. Carlos Terapeuta', 'terapeuta@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'TERAPEUTA'),
('Admin Principal', 'admin@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'ADMIN');

DECLARE @esp1 bigint = (SELECT TOP 1 id FROM especialidad WHERE nombre = 'Estimulación temprana');
DECLARE @terUsr bigint = (SELECT TOP 1 id FROM usuario WHERE email = 'terapeuta@test.com');
DECLARE @apo bigint = (SELECT TOP 1 id FROM usuario WHERE email = 'apoderado@test.com');

INSERT INTO terapeuta (usuario_id, especialidad_id, colegiatura) VALUES (@terUsr, @esp1, 'CTP-12345');

INSERT INTO nino (nombres, fecha_nacimiento, apoderado_id) VALUES
('Luisito Pérez', '2021-03-15', @apo),
('Sofía Ramírez', '2020-07-22', @apo);
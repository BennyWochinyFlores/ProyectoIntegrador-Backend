-- Datos de prueba - Centro de Estimulacion Integral

INSERT INTO especialidad (nombre, duracion_minutos, precio) VALUES
('Estimulacion temprana', 45, 50.00),
('Terapia de lenguaje', 60, 60.00),
('Terapia ocupacional', 50, 55.00);

INSERT INTO usuario (nombre, email, password, rol) VALUES
('Maria Apoderada', 'apoderado@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'APODERADO'),
('Dr. Carlos Terapeuta', 'terapeuta@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'TERAPEUTA'),
('Admin Principal', 'admin@test.com', '$2a$10$SnKMz2AE8kKNDlg1afzbF.wJosZIScyZsYdjZhiH3p9sWayOqLuC2', 'ADMIN');

INSERT INTO terapeuta (usuario_id, especialidad_id, colegiatura) VALUES
((SELECT id FROM usuario WHERE email = 'terapeuta@test.com'),
 (SELECT id FROM especialidad WHERE nombre = 'Estimulacion temprana'),
 'CTP-12345');

INSERT INTO nino (nombres, fecha_nacimiento, apoderado_id) VALUES
('Luisito Perez', '2021-03-15', (SELECT id FROM usuario WHERE email = 'apoderado@test.com')),
('Sofia Ramirez', '2020-07-22', (SELECT id FROM usuario WHERE email = 'apoderado@test.com'));
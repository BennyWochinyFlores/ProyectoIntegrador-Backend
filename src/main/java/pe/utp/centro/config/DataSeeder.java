package pe.utp.centro.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.utp.centro.model.Contrato;
import pe.utp.centro.model.Especialidad;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Telefono;
import pe.utp.centro.model.Terapeuta;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.ContratoRepository;
import pe.utp.centro.repository.EspecialidadRepository;
import pe.utp.centro.repository.NinoRepository;
import pe.utp.centro.repository.TelefonoRepository;
import pe.utp.centro.repository.TerapeutaRepository;
import pe.utp.centro.repository.UsuarioRepository;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedData(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              EspecialidadRepository especialidadRepository,
                              TerapeutaRepository terapeutaRepository,
                              ContratoRepository contratoRepository,
                              TelefonoRepository telefonoRepository,
                              NinoRepository ninoRepository) {
        return args -> {
            if (especialidadRepository.count() == 0) {
                // Insertar especialidades si no existen
                Especialidad esp1 = new Especialidad();
                esp1.setNombre("Estimulacion temprana");
                esp1.setDuracionMinutos(45);
                esp1.setPrecio(new java.math.BigDecimal("50.00"));
                esp1 = especialidadRepository.save(esp1);

                Especialidad esp2 = new Especialidad();
                esp2.setNombre("Terapia de lenguaje");
                esp2.setDuracionMinutos(60);
                esp2.setPrecio(new java.math.BigDecimal("60.00"));
                esp2 = especialidadRepository.save(esp2);

                Especialidad esp3 = new Especialidad();
                esp3.setNombre("Terapia ocupacional");
                esp3.setDuracionMinutos(50);
                esp3.setPrecio(new java.math.BigDecimal("55.00"));
                esp3 = especialidadRepository.save(esp3);

                System.out.println("=== Especialidades inserted ===");
            }

            if (usuarioRepository.findByEmail("apoderado@test.com").isEmpty()) {
                Usuario apoderado = new Usuario();
                apoderado.setNombre("Maria Apoderada");
                apoderado.setEmail("apoderado@test.com");
                apoderado.setPassword(passwordEncoder.encode("password123"));
                apoderado.setRol(Usuario.Rol.APODERADO);
                usuarioRepository.save(apoderado);

                Usuario terapeutaUser = new Usuario();
                terapeutaUser.setNombre("Dr. Carlos Terapeuta");
                terapeutaUser.setEmail("terapeuta@test.com");
                terapeutaUser.setPassword(passwordEncoder.encode("password123"));
                terapeutaUser.setRol(Usuario.Rol.TERAPEUTA);
                terapeutaUser.setDni("12345678");
                terapeutaUser = usuarioRepository.save(terapeutaUser);

                Usuario admin = new Usuario();
                admin.setNombre("Admin Principal");
                admin.setEmail("admin@test.com");
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setRol(Usuario.Rol.ADMIN);
                admin.setDni("87654321");
                usuarioRepository.save(admin);

                System.out.println("=== Users inserted ===");
            }

            // Completar datos del apoderado (DNI, telefono e hijo) si faltan
            Usuario apoderadoUser = usuarioRepository.findByEmail("apoderado@test.com").orElse(null);
            if (apoderadoUser != null) {
                if (apoderadoUser.getDni() == null || apoderadoUser.getDni().isEmpty()) {
                    apoderadoUser.setDni("45678912");
                    usuarioRepository.save(apoderadoUser);
                    System.out.println("=== DNI apoderado inserted ===");
                }
                if (telefonoRepository.findByUsuario(apoderadoUser).isEmpty()) {
                    Telefono tel1 = new Telefono();
                    tel1.setNumero("987654321");
                    tel1.setEsPrincipal(true);
                    tel1.setUsuario(apoderadoUser);
                    telefonoRepository.save(tel1);
                    System.out.println("=== Telefono apoderado inserted ===");
                }
                if (ninoRepository.findByApoderado(apoderadoUser).isEmpty()) {
                    Nino nino = new Nino();
                    nino.setDni("10101010");
                    nino.setApellidoPaterno("Apoderada");
                    nino.setApellidoMaterno("Llanos");
                    nino.setNombres("Samuel Adrian");
                    nino.setFechaNacimiento(java.time.LocalDate.of(2019, 5, 12));
                    nino.setGenero("M");
                    nino.setParentesco("MADRE");
                    nino.setEsPrincipal(true);
                    nino.setApoderado(apoderadoUser);
                    ninoRepository.save(nino);
                    System.out.println("=== Nino apoderado inserted ===");
                }
            }

            if (terapeutaRepository.count() == 0) {
                Usuario terapeutaUser = usuarioRepository.findByEmail("terapeuta@test.com").orElse(null);
                Especialidad esp1 = especialidadRepository.findAll().stream().findFirst().orElse(null);
                if (terapeutaUser != null && esp1 != null) {
                    Terapeuta terapeuta = new Terapeuta();
                    terapeuta.setUsuario(terapeutaUser);
                    terapeuta.setEspecialidad(esp1);
                    terapeuta.setColegiatura("CTP-12345");
                    terapeutaRepository.save(terapeuta);
                    System.out.println("=== Terapeuta inserted ===");

                    // Create contract
                    Contrato contrato = new Contrato();
                    contrato.setUsuario(terapeutaUser);
                    contrato.setNumero("CT-001");
                    contrato.setTipoContrato("INDEFINIDO");
                    contrato.setPuesto("Terapeuta de estimulacion");
                    contrato.setJornada("Lun-Sab 08:00-20:00");
                    contrato.setSalario(3500.0);
                    contrato.setFechaInicio("2025-01-01");
                    contrato.setFechaFin(null);
                    contrato.setEstado("ACTIVO");
                    contratoRepository.save(contrato);
                    System.out.println("=== Contrato inserted ===");
                }
            }

            System.out.println("=== Seed data check completed ===");
        };
    }
}
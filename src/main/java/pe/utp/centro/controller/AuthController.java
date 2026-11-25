package pe.utp.centro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.LoginRequest;
import pe.utp.centro.dto.LoginResponse;
import pe.utp.centro.dto.RegistroRequest;
import pe.utp.centro.model.Telefono;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.TelefonoRepository;
import pe.utp.centro.repository.UsuarioRepository;
import pe.utp.centro.security.JwtUtil;

/**
 * HU-01 (login) y HU-02 (registro). El registro crea un usuario rol APODERADO.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final TelefonoRepository telefonoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          TelefonoRepository telefonoRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.telefonoRepository = telefonoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!passwordEncoder.matches(req.password(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return LoginResponse.of(token, usuario);
    }

    @PostMapping("/registro")
    public LoginResponse registrar(@Valid @RequestBody RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        // El DNI no puede estar registrado por otro usuario
        String dni = req.dni() != null ? req.dni().trim() : null;
        if (dni != null && !dni.isEmpty()
                && usuarioRepository.findAll().stream()
                        .anyMatch(u -> u.getDni() != null && u.getDni().equals(dni))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }

        // Nombre completo: nombres + apellidos
        String nombre = String.join(" ", req.nombres(), req.apellidoPaterno(), req.apellidoMaterno())
                .trim().replaceAll("\\s+", " ");

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setEmail(email);
        nuevo.setPassword(passwordEncoder.encode(req.password()));
        nuevo.setRol(Usuario.Rol.APODERADO);
        nuevo.setDni(dni);

        if (req.fechaNacimiento() != null && !req.fechaNacimiento().isEmpty()) {
            try {
                nuevo.setFechaNacimiento(java.time.LocalDate.parse(req.fechaNacimiento()));
            } catch (Exception ignored) {
                // fecha invalida: se guarda nula
            }
        }
        if (req.genero() != null && !req.genero().isEmpty()) {
            nuevo.setGenero(req.genero());
        }
        nuevo = usuarioRepository.save(nuevo);

        // Guardar el celular como telefono principal del apoderado
        if (req.telefono() != null && !req.telefono().isEmpty()) {
            Telefono tel = new Telefono();
            tel.setNumero(req.telefono().trim());
            tel.setEsPrincipal(true);
            tel.setUsuario(nuevo);
            telefonoRepository.save(tel);
        }

        String token = jwtUtil.generarToken(nuevo.getEmail(), nuevo.getRol().name());
        return LoginResponse.of(token, nuevo);
    }
}
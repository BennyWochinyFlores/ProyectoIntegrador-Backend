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
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.UsuarioRepository;
import pe.utp.centro.security.JwtUtil;

/**
 * HU-01 (login) y HU-02 (registro). El registro crea un usuario rol APODERADO.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
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

        Usuario nuevo = new Usuario();
        nuevo.setNombre(req.nombre().trim());
        nuevo.setEmail(email);
        nuevo.setPassword(passwordEncoder.encode(req.password()));
        nuevo.setRol(Usuario.Rol.APODERADO);
        nuevo = usuarioRepository.save(nuevo);

        String token = jwtUtil.generarToken(nuevo.getEmail(), nuevo.getRol().name());
        return LoginResponse.of(token, nuevo);
    }
}
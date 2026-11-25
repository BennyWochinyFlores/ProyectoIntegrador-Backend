package pe.utp.centro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.model.Telefono;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.TelefonoRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telefonos")
public class TelefonoController {

    private final TelefonoRepository telefonoRepository;

    public TelefonoController(TelefonoRepository telefonoRepository) {
        this.telefonoRepository = telefonoRepository;
    }

    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    public List<Telefono> listarMios(@AuthenticationPrincipal Usuario usuario) {
        return telefonoRepository.findByUsuario(usuario);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public Telefono agregar(@Valid @RequestBody Map<String, String> body,
                            @AuthenticationPrincipal Usuario usuario) {
        String numero = body.get("numero");
        if (numero == null || !numero.matches("\\d{9}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número debe tener 9 dígitos");
        }

        List<Telefono> existentes = telefonoRepository.findByUsuario(usuario);
        boolean esPrincipal = existentes.isEmpty();

        Telefono telefono = new Telefono();
        telefono.setNumero(numero);
        telefono.setEsPrincipal(esPrincipal);
        telefono.setUsuario(usuario);
        return telefonoRepository.save(telefono);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Telefono actualizar(@PathVariable Long id,
                              @Valid @RequestBody Map<String, String> body,
                              @AuthenticationPrincipal Usuario usuario) {
        Telefono telefono = telefonoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teléfono no encontrado"));

        if (!telefono.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar este teléfono");
        }

        String numero = body.get("numero");
        if (numero == null || !numero.matches("\\d{9}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número debe tener 9 dígitos");
        }

        telefono.setNumero(numero);
        return telefonoRepository.save(telefono);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Telefono telefono = telefonoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teléfono no encontrado"));

        if (!telefono.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar este teléfono");
        }

        boolean eraPrincipal = telefono.getEsPrincipal();
        telefonoRepository.delete(telefono);

        if (eraPrincipal) {
            List<Telefono> restantes = telefonoRepository.findByUsuario(usuario);
            if (!restantes.isEmpty()) {
                Telefono nuevoPrincipal = restantes.get(0);
                nuevoPrincipal.setEsPrincipal(true);
                telefonoRepository.save(nuevoPrincipal);
            }
        }
    }
}
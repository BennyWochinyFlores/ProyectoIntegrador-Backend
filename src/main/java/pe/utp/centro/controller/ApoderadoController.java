package pe.utp.centro.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Telefono;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.NinoRepository;
import pe.utp.centro.repository.TelefonoRepository;
import pe.utp.centro.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apoderados")
public class ApoderadoController {

    private final UsuarioRepository usuarioRepository;
    private final NinoRepository ninoRepository;
    private final TelefonoRepository telefonoRepository;

    public ApoderadoController(UsuarioRepository usuarioRepository,
                               NinoRepository ninoRepository,
                               TelefonoRepository telefonoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.ninoRepository = ninoRepository;
        this.telefonoRepository = telefonoRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public List<Map<String, Object>> listar(@RequestParam(required = false) String nombre) {
        List<Usuario> apoderados = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Usuario.Rol.APODERADO)
                .filter(u -> nombre == null || nombre.isEmpty() || u.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();

        return apoderados.stream().map(a -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", a.getId());
            map.put("nombreCompleto", a.getNombre());
            map.put("email", a.getEmail());
            map.put("dni", a.getDni() != null ? a.getDni() : "");

            // Telefonos
            List<String> telefonos = telefonoRepository.findByUsuario(a).stream()
                    .map(Telefono::getNumero)
                    .collect(Collectors.toList());
            map.put("telefonos", telefonos);

            // Hijos
            List<Map<String, Object>> hijos = ninoRepository.findByApoderado(a).stream()
                    .map(n -> {
                        Map<String, Object> h = new java.util.HashMap<>();
                        h.put("id", n.getId());
                        h.put("nombreCompleto", n.getNombreCompleto());
                        h.put("parentesco", n.getParentesco());
                        return h;
                    })
                    .collect(Collectors.toList());
            map.put("hijos", hijos);

            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public Map<String, Object> obtener(@PathVariable Long id) {
        Usuario a = usuarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Apoderado no encontrado"));

        if (a.getRol() != Usuario.Rol.APODERADO) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Apoderado no encontrado");
        }

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", a.getId());
        map.put("nombreCompleto", a.getNombre());
        map.put("email", a.getEmail());
        map.put("dni", a.getDni() != null ? a.getDni() : "");

        List<String> telefonos = telefonoRepository.findByUsuario(a).stream()
                .map(Telefono::getNumero)
                .collect(Collectors.toList());
        map.put("telefonos", telefonos);

        List<Map<String, Object>> hijos = ninoRepository.findByApoderado(a).stream()
                .map(n -> {
                    Map<String, Object> h = new java.util.HashMap<>();
                    h.put("id", n.getId());
                    h.put("nombreCompleto", n.getNombreCompleto());
                    h.put("parentesco", n.getParentesco());
                    return h;
                })
                .collect(Collectors.toList());
        map.put("hijos", hijos);

        return map;
    }
}
package pe.utp.centro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.model.Contrato;
import pe.utp.centro.model.Especialidad;
import pe.utp.centro.model.Terapeuta;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.ContratoRepository;
import pe.utp.centro.repository.EspecialidadRepository;
import pe.utp.centro.repository.TerapeutaRepository;
import pe.utp.centro.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trabajadores")
public class TrabajadorController {

    private final UsuarioRepository usuarioRepository;
    private final TerapeutaRepository terapeutaRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ContratoRepository contratoRepository;
    private final PasswordEncoder passwordEncoder;

    public TrabajadorController(UsuarioRepository usuarioRepository,
                                TerapeutaRepository terapeutaRepository,
                                EspecialidadRepository especialidadRepository,
                                ContratoRepository contratoRepository,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.terapeutaRepository = terapeutaRepository;
        this.especialidadRepository = especialidadRepository;
        this.contratoRepository = contratoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private Map<String, Object> toMap(Usuario u) {
        Map<String, Object> t = new java.util.HashMap<>();
        t.put("id", u.getId());
        t.put("tipo", u.getRol().name());
        t.put("nombre", u.getNombre());
        t.put("nombres", u.getNombre());
        t.put("email", u.getEmail());
        t.put("activo", true);
        t.put("apellidoPaterno", "");
        t.put("apellidoMaterno", "");
        t.put("dni", u.getDni() != null ? u.getDni() : "");
        t.put("especialidades", List.of());
        t.put("colegiatura", null);
        t.put("contrato", null);

        terapeutaRepository.findAll().stream()
                .filter(ter -> ter.getUsuario().getId().equals(u.getId()))
                .findFirst()
                .ifPresent(ter -> {
                    t.put("colegiatura", ter.getColegiatura());
                    t.put("especialidades", List.of(ter.getEspecialidad().getNombre()));
                });

        contratoRepository.findByUsuario(u).ifPresent(c -> {
            Map<String, Object> contrato = new java.util.HashMap<>();
            contrato.put("numero", c.getNumero());
            contrato.put("tipoContrato", c.getTipoContrato());
            contrato.put("puesto", c.getPuesto());
            contrato.put("jornada", c.getJornada());
            contrato.put("salario", c.getSalario());
            contrato.put("fechaInicio", c.getFechaInicio());
            contrato.put("fechaFin", c.getFechaFin());
            contrato.put("estado", c.getEstado());
            t.put("contrato", contrato);
        });

        return t;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> listar(@RequestParam(required = false) String tipo,
                                            @RequestParam(required = false) String nombre,
                                            @RequestParam(required = false) Long id) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != Usuario.Rol.APODERADO)
                .filter(u -> tipo == null || tipo.isEmpty() || u.getRol().name().equals(tipo))
                .filter(u -> id == null || u.getId().equals(id))
                .filter(u -> nombre == null || nombre.isEmpty() || u.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> obtener(@PathVariable Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajador no encontrado"));
        return toMap(u);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> registrar(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        String nombre = body.get("nombres") + " " + body.get("apellidoPaterno") + " " + body.get("apellidoMaterno");
        String tipo = (String) body.get("tipo");
        String dni = (String) body.get("dni");

        if (usuarioRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(email.trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(Usuario.Rol.valueOf(tipo));
        usuario.setDni(dni);
        final Usuario savedUsuario = usuarioRepository.save(usuario);

        if ("TERAPEUTA".equals(tipo)) {
            String colegiatura = (String) body.get("colegiatura");
            @SuppressWarnings("unchecked")
            List<Integer> especialidadIds = (List<Integer>) body.get("especialidadIds");
            if (especialidadIds != null && !especialidadIds.isEmpty()) {
                Especialidad esp = especialidadRepository.findById(especialidadIds.get(0).longValue()).orElse(null);
                if (esp != null) {
                    Terapeuta terapeuta = new Terapeuta();
                    terapeuta.setUsuario(savedUsuario);
                    terapeuta.setEspecialidad(esp);
                    terapeuta.setColegiatura(colegiatura);
                    terapeutaRepository.save(terapeuta);
                }
            }
        }

        // Save contract
        @SuppressWarnings("unchecked")
        Map<String, Object> contratoData = (Map<String, Object>) body.get("contrato");
        if (contratoData == null) {
            contratoData = new java.util.HashMap<>();
            contratoData.put("tipoContrato", body.get("tipoContrato"));
            contratoData.put("puesto", body.get("puesto"));
            contratoData.put("fechaInicio", body.get("fechaInicio"));
            contratoData.put("fechaFin", body.get("fechaFin"));
            contratoData.put("salario", body.get("salario"));
        }
        if (contratoData.get("tipoContrato") != null || contratoData.get("fechaInicio") != null) {
            Contrato contrato = new Contrato();
            contrato.setUsuario(savedUsuario);
            String numero = (String) contratoData.get("numero");
            contrato.setNumero(numero != null && !numero.isEmpty() ? numero : "CT-" + String.format("%03d", savedUsuario.getId()));
            String jornada = (String) body.get("jornada");
            contrato.setJornada(jornada != null && !jornada.isEmpty() ? jornada : (String) contratoData.get("jornada"));
            Object salario = contratoData.get("salario");
            if (salario != null && !salario.toString().isEmpty()) {
                contrato.setSalario(Double.parseDouble(salario.toString()));
            }
            String fechaInicio = (String) contratoData.get("fechaInicio");
            String fechaFin = (String) contratoData.get("fechaFin");
            contrato.setFechaInicio(fechaInicio != null && !fechaInicio.isEmpty() ? fechaInicio : null);
            contrato.setFechaFin(fechaFin != null && !fechaFin.isEmpty() ? fechaFin : null);
            String puesto = (String) contratoData.get("puesto");
            contrato.setPuesto(puesto != null && !puesto.isEmpty() ? puesto : null);
            String tipoContratoVal = (String) contratoData.get("tipoContrato");
            contrato.setTipoContrato(tipoContratoVal != null && !tipoContratoVal.isEmpty() ? tipoContratoVal : null);
            contrato.setEstado("ACTIVO");
            contratoRepository.save(contrato);
        }

        return toMap(savedUsuario);
    }
}
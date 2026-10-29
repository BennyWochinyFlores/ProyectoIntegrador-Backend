package pe.utp.centro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.CitaRequest;
import pe.utp.centro.dto.CitaResponse;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Especialidad;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Terapeuta;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.repository.EspecialidadRepository;
import pe.utp.centro.repository.NinoRepository;
import pe.utp.centro.repository.TerapeutaRepository;
import pe.utp.centro.service.CitaService;

import java.util.List;

/**
 * Modulo de agendamiento (RF-03/RF-04). El listado y las acciones se filtran
 * segun el rol del usuario autenticado.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;
    private final CitaRepository citaRepository;
    private final NinoRepository ninoRepository;
    private final TerapeutaRepository terapeutaRepository;
    private final EspecialidadRepository especialidadRepository;

    public CitaController(CitaService citaService, CitaRepository citaRepository,
                           NinoRepository ninoRepository, TerapeutaRepository terapeutaRepository,
                           EspecialidadRepository especialidadRepository) {
        this.citaService = citaService;
        this.citaRepository = citaRepository;
        this.ninoRepository = ninoRepository;
        this.terapeutaRepository = terapeutaRepository;
        this.especialidadRepository = especialidadRepository;
    }

    /** Lista segun el rol: admin/recepcionista ven todas; apoderado las de sus hijos; terapeuta las suyas. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CitaResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        List<Cita> citas = switch (usuario.getRol()) {
            case ADMIN, RECEPCIONISTA -> citaRepository.findAll();
            case APODERADO -> citaRepository.findAll().stream()
                    .filter(c -> c.getNino().getApoderado().getId().equals(usuario.getId()))
                    .toList();
            case TERAPEUTA -> citaRepository.findAll().stream()
                    .filter(c -> c.getTerapeuta().getUsuario().getId().equals(usuario.getId()))
                    .toList();
        };
        return citas.stream().map(CitaResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CitaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        validarAcceso(cita, usuario);
        return CitaResponse.from(cita);
    }

    @PostMapping
    @PreAuthorize("hasRole('APODERADO') or hasRole('ADMIN')")
    public ResponseEntity<?> agendar(@Valid @RequestBody CitaRequest req,
                                     @AuthenticationPrincipal Usuario usuario) {
        Nino nino = ninoRepository.findById(req.ninoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Niño no encontrado"));
        Terapeuta terapeuta = terapeutaRepository.findById(req.terapeutaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Terapeuta no encontrado"));
        Especialidad especialidad = especialidadRepository.findById(req.especialidadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada"));

        // Un apoderado solo puede agendar para sus propios hijos
        if (usuario.getRol() == Usuario.Rol.APODERADO && !nino.getApoderado().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes agendar para tus hijos");
        }

        try {
            Cita cita = citaService.agendar(nino, terapeuta, especialidad, req.fechaHora());
            return ResponseEntity.status(HttpStatus.CREATED).body(CitaResponse.from(cita));
        } catch (CitaService.HorarioNoDisponibleException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public CitaResponse confirmar(@PathVariable Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return CitaResponse.from(citaService.confirmar(cita));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public CitaResponse cancelar(@PathVariable Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return CitaResponse.from(citaService.cancelar(cita));
    }

    private void validarAcceso(Cita cita, Usuario usuario) {
        boolean permite = switch (usuario.getRol()) {
            case ADMIN, RECEPCIONISTA -> true;
            case APODERADO -> cita.getNino().getApoderado().getId().equals(usuario.getId());
            case TERAPEUTA -> cita.getTerapeuta().getUsuario().getId().equals(usuario.getId());
        };
        if (!permite) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta cita");
        }
    }
}
package pe.utp.centro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.SesionRequest;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Sesion;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.repository.NinoRepository;
import pe.utp.centro.repository.SesionRepository;
import pe.utp.centro.service.SesionService;

import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

    private final SesionService sesionService;
    private final CitaRepository citaRepository;
    private final NinoRepository ninoRepository;
    private final SesionRepository sesionRepository;

    public SesionController(SesionService sesionService,
                            CitaRepository citaRepository,
                            NinoRepository ninoRepository,
                            SesionRepository sesionRepository) {
        this.sesionService = sesionService;
        this.citaRepository = citaRepository;
        this.ninoRepository = ninoRepository;
        this.sesionRepository = sesionRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('TERAPEUTA') or hasRole('ADMIN')")
    public Sesion registrar(@Valid @RequestBody SesionRequest req,
                            @AuthenticationPrincipal Usuario usuario) {
        Cita cita = citaRepository.findById(req.citaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        if (usuario.getRol() == Usuario.Rol.TERAPEUTA
                && (cita.getTerapeuta() == null || cita.getTerapeuta().getUsuario() == null
                || !cita.getTerapeuta().getUsuario().getId().equals(usuario.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes registrar sesiones de tus propias citas.");
        }
        try {
            return sesionService.registrarSesion(cita, req.observaciones(), req.nivelAvance());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @GetMapping("/nino/{ninoId}")
    @PreAuthorize("isAuthenticated()")
    public List<Sesion> progresoDeNino(@PathVariable Long ninoId, @AuthenticationPrincipal Usuario usuario) {
        Nino nino = ninoRepository.findById(ninoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Niño no encontrado"));
        if (usuario.getRol() == Usuario.Rol.APODERADO && !nino.getApoderado().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es hijo de este apoderado");
        }
        List<Sesion> sesiones = sesionRepository.findByCita_Nino(nino);
        if (usuario.getRol() == Usuario.Rol.TERAPEUTA) {
            return sesiones.stream()
                    .filter(s -> s.getCita() != null && s.getCita().getTerapeuta() != null
                            && s.getCita().getTerapeuta().getUsuario() != null
                            && s.getCita().getTerapeuta().getUsuario().getId().equals(usuario.getId()))
                    .toList();
        }
        return sesiones;
    }
}
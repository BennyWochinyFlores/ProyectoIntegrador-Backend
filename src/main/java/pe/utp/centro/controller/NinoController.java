package pe.utp.centro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.NinoRequest;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.NinoRepository;

import java.util.List;

/**
 * HU-03: listado de ninos. HU-10: el apoderado solo ve a sus hijos.
 * Registo de ninos por el propio apoderado (HU-02).
 */
@RestController
@RequestMapping("/api/ninos")
public class NinoController {

    private final NinoRepository ninoRepository;

    public NinoController(NinoRepository ninoRepository) {
        this.ninoRepository = ninoRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('APODERADO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Nino registrar(@Valid @RequestBody NinoRequest req, @AuthenticationPrincipal Usuario apoderado) {
        Nino nino = new Nino();
        nino.setNombres(req.nombres().trim());
        nino.setFechaNacimiento(req.fechaNacimiento());
        nino.setApoderado(apoderado);
        return ninoRepository.save(nino);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA') or hasRole('TERAPEUTA')")
    public List<Nino> listarTodos() {
        return ninoRepository.findAll();
    }

    @GetMapping("/mis-hijos")
    @PreAuthorize("hasRole('APODERADO')")
    public List<Nino> misHijos(@AuthenticationPrincipal Usuario usuario) {
        return ninoRepository.findByApoderado(usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Nino obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Nino nino = ninoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Niño no encontrado"));
        if (usuario.getRol() == Usuario.Rol.APODERADO && !nino.getApoderado().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es hijo de este apoderado");
        }
        return nino;
    }
}
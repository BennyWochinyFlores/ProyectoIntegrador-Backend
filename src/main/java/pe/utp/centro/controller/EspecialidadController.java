package pe.utp.centro.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.centro.model.Especialidad;
import pe.utp.centro.repository.EspecialidadRepository;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadController(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Especialidad> listar() {
        return especialidadRepository.findAll();
    }
}
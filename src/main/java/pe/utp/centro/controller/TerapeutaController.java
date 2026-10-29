package pe.utp.centro.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.centro.model.Terapeuta;
import pe.utp.centro.repository.TerapeutaRepository;

import java.util.List;

@RestController
@RequestMapping("/api/terapeutas")
public class TerapeutaController {

    private final TerapeutaRepository terapeutaRepository;

    public TerapeutaController(TerapeutaRepository terapeutaRepository) {
        this.terapeutaRepository = terapeutaRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Terapeuta> listar() {
        return terapeutaRepository.findAll();
    }
}
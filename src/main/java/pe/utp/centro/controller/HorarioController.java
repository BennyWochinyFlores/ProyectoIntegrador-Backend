package pe.utp.centro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    @GetMapping
    public Map<String, List<String>> obtenerHorarios() {
        return Map.of(
            "feriados", List.of(),
            "diasCerrados", List.of("SUNDAY")
        );
    }
}
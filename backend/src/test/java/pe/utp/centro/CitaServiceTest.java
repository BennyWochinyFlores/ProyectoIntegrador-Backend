package pe.utp.centro;

import org.junit.jupiter.api.Test;
import pe.utp.centro.model.*;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.service.CitaService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CitaServiceTest {

    @Test
    void debeRechazarCitaConHorarioSolapado() {
        CitaRepository repo = mock(CitaRepository.class);
        CitaService service = new CitaService(repo);

        Especialidad especialidad = new Especialidad();
        especialidad.setId(1L);
        especialidad.setNombre("Estimulación temprana");
        especialidad.setDuracionMinutos(45);

        Terapeuta terapeuta = new Terapeuta();
        terapeuta.setId(1L);

        Nino nino = new Nino();
        nino.setId(1L);

        LocalDateTime horaExistente = LocalDateTime.of(2026, 9, 1, 10, 0);

        Cita citaExistente = new Cita();
        citaExistente.setTerapeuta(terapeuta);
        citaExistente.setEspecialidad(especialidad);
        citaExistente.setFechaHora(horaExistente);
        citaExistente.setEstado(Cita.Estado.CONFIRMADA);

        when(repo.findByTerapeutaAndFechaHoraBetweenAndEstadoNot(any(), any(), any(), any()))
                .thenReturn(List.of(citaExistente));

        // Intento de agendar 15 minutos despues de una cita de 45 min -> se solapa
        LocalDateTime horaConflictiva = horaExistente.plusMinutes(15);

        assertThrows(CitaService.HorarioNoDisponibleException.class, () ->
                service.agendar(nino, terapeuta, especialidad, horaConflictiva));
    }

    @Test
    void debePermitirCitaEnHorarioLibre() {
        CitaRepository repo = mock(CitaRepository.class);
        CitaService service = new CitaService(repo);

        Especialidad especialidad = new Especialidad();
        especialidad.setId(1L);
        especialidad.setDuracionMinutos(45);

        Terapeuta terapeuta = new Terapeuta();
        Nino nino = new Nino();

        when(repo.findByTerapeutaAndFechaHoraBetweenAndEstadoNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(repo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita resultado = service.agendar(nino, terapeuta, especialidad, LocalDateTime.of(2026, 9, 1, 10, 0));

        assertEquals(Cita.Estado.PENDIENTE, resultado.getEstado());
    }
}

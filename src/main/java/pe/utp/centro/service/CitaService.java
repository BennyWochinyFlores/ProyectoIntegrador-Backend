package pe.utp.centro.service;

import org.springframework.stereotype.Service;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Especialidad;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Terapeuta;
import pe.utp.centro.repository.CitaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementa RF-03 (agendar validando disponibilidad) y RF-04 (confirmar/reprogramar/cancelar).
 */
@Service
public class CitaService {

    private final CitaRepository citaRepository;

    public CitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    public static class HorarioNoDisponibleException extends RuntimeException {
        public HorarioNoDisponibleException(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Agenda una cita validando que el terapeuta no tenga otra cita activa
     * (no cancelada) que se solape con la duración de la especialidad.
     */
    public Cita agendar(Nino nino, Terapeuta terapeuta, Especialidad especialidad, LocalDateTime fechaHora) {
        validarDisponibilidad(nino, terapeuta, especialidad, fechaHora, null);

        Cita cita = new Cita();
        cita.setNino(nino);
        cita.setTerapeuta(terapeuta);
        cita.setEspecialidad(especialidad);
        cita.setFechaHora(fechaHora);
        cita.setEstado(Cita.Estado.PENDIENTE);
        return citaRepository.save(cita);
    }

    /** Valida que el terapeuta no tenga otra cita activa que se solape con el horario propuesto. */
    private void validarDisponibilidad(Nino nino, Terapeuta terapeuta, Especialidad especialidad,
                                       LocalDateTime fechaHora, Cita citaAExcluir) {
        LocalDateTime finPropuesto = fechaHora.plusMinutes(especialidad.getDuracionMinutos());

        List<Cita> citasEnRango = citaRepository.findByTerapeutaAndFechaHoraBetweenAndEstadoNot(
                terapeuta,
                fechaHora.minusHours(4), // ventana amplia para detectar solapamientos cercanos
                finPropuesto.plusHours(4),
                Cita.Estado.CANCELADA
        );

        boolean haySolape = citasEnRango.stream()
                .filter(c -> citaAExcluir == null || !c.getId().equals(citaAExcluir.getId()))
                .anyMatch(c -> seSuperponen(
                        c.getFechaHora(), c.getFechaHora().plusMinutes(c.getEspecialidad().getDuracionMinutos()),
                        fechaHora, finPropuesto
                ));

        if (haySolape) {
            throw new HorarioNoDisponibleException(
                    "El terapeuta ya tiene una cita programada que se solapa con este horario.");
        }
    }

    public Cita confirmar(Cita cita) {
        cita.setEstado(Cita.Estado.CONFIRMADA);
        return citaRepository.save(cita);
    }

    public Cita cancelar(Cita cita) {
        cita.setEstado(Cita.Estado.CANCELADA);
        return citaRepository.save(cita);
    }

    /** Reprograma la misma cita (mismo id) revalidando disponibilidad en el nuevo horario. */
    public Cita reprogramar(Cita cita, LocalDateTime nuevaFechaHora) {
        validarDisponibilidad(cita.getNino(), cita.getTerapeuta(), cita.getEspecialidad(),
                nuevaFechaHora, cita);
        cita.setFechaHora(nuevaFechaHora);
        cita.setEstado(Cita.Estado.PENDIENTE);
        return citaRepository.save(cita);
    }

    private boolean seSuperponen(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }
}

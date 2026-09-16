package pe.utp.centro.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Sesion;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.repository.SesionRepository;

/**
 * Implementa RF-05 (registrar sesion) y sirve de base para RF-06 (reporte de progreso).
 */
@Service
public class SesionService {

    private final SesionRepository sesionRepository;
    private final CitaRepository citaRepository;

    public SesionService(SesionRepository sesionRepository, CitaRepository citaRepository) {
        this.sesionRepository = sesionRepository;
        this.citaRepository = citaRepository;
    }

    @Transactional
    public Sesion registrarSesion(Cita cita, String observaciones, Sesion.NivelAvance nivelAvance) {
        if (cita.getEstado() != Cita.Estado.CONFIRMADA && cita.getEstado() != Cita.Estado.ATENDIDA) {
            throw new IllegalStateException("Solo se puede registrar una sesion sobre una cita confirmada o atendida.");
        }
        cita.setEstado(Cita.Estado.ATENDIDA);
        citaRepository.save(cita);

        Sesion sesion = new Sesion();
        sesion.setCita(cita);
        sesion.setObservaciones(observaciones);
        sesion.setNivelAvance(nivelAvance);
        return sesionRepository.save(sesion);
    }
}

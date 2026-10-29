package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Terapeuta;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByNino(Nino nino);

    // Usado por CitaService para validar disponibilidad (HU-04)
    List<Cita> findByTerapeutaAndFechaHoraBetweenAndEstadoNot(
            Terapeuta terapeuta, LocalDateTime desde, LocalDateTime hasta, Cita.Estado estadoExcluido);
}

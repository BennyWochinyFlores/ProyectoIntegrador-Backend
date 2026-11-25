package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Sesion;

import java.util.List;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    // RF-06: reporte de progreso por nino
    List<Sesion> findByCita_Nino(Nino nino);

    // Find session by cita
    java.util.Optional<Sesion> findByCita(pe.utp.centro.model.Cita cita);
}

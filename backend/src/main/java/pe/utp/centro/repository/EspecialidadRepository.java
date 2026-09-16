package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
}

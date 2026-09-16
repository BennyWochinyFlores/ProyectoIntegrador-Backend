package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Terapeuta;

public interface TerapeutaRepository extends JpaRepository<Terapeuta, Long> {
}

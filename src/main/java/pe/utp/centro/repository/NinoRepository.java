package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface NinoRepository extends JpaRepository<Nino, Long> {
    List<Nino> findByApoderado(Usuario apoderado);
    Optional<Nino> findByDni(String dni);
}

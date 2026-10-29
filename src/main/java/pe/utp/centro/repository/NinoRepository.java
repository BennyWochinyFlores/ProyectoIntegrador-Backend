package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Usuario;

import java.util.List;

public interface NinoRepository extends JpaRepository<Nino, Long> {
    List<Nino> findByApoderado(Usuario apoderado);
}

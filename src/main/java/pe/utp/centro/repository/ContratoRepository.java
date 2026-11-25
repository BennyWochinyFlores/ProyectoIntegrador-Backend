package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Contrato;
import pe.utp.centro.model.Usuario;

import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Optional<Contrato> findByUsuario(Usuario usuario);
}
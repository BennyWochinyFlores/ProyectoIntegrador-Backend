package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Telefono;
import pe.utp.centro.model.Usuario;

import java.util.List;

public interface TelefonoRepository extends JpaRepository<Telefono, Long> {
    List<Telefono> findByUsuario(Usuario usuario);
}
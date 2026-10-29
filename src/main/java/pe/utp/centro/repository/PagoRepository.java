package pe.utp.centro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.centro.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}

package pe.utp.centro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Metodo metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(length = 50)
    private String numeroBoleta;

    @Column(length = 50)
    private String numeroOperacion;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public enum Metodo {
        EFECTIVO, TARJETA, TRANSFERENCIA
    }

    public enum Estado {
        PENDIENTE, PAGADO
    }
}

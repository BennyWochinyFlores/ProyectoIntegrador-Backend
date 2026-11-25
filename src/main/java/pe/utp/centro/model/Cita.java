package pe.utp.centro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nino_id", nullable = false)
    private Nino nino;

    @ManyToOne
    @JoinColumn(name = "terapeuta_id", nullable = false)
    private Terapeuta terapeuta;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @OneToOne(mappedBy = "cita")
    private Sesion sesion;

    public enum Estado {
        PENDIENTE, CONFIRMADA, CANCELADA, ATENDIDA
    }
}

package pe.utp.centro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "nino")
@Getter
@Setter
@NoArgsConstructor
public class Nino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @ManyToOne
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Usuario apoderado;
}

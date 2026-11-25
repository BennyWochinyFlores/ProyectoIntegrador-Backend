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

    @Column(nullable = false, length = 8)
    private String dni;

    @Column(nullable = false, length = 60)
    private String apellidoPaterno;

    @Column(nullable = false, length = 60)
    private String apellidoMaterno;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 1)
    private String genero;

    @Column(nullable = false, length = 20)
    private String parentesco;

    @Column(nullable = false)
    private Boolean esPrincipal = true;

    @ManyToOne
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Usuario apoderado;

    public String getNombreCompleto() {
        return (apellidoPaterno + " " + apellidoMaterno + " " + nombres).trim();
    }
}

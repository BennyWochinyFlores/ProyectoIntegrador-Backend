package pe.utp.centro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contrato")
@Getter
@Setter
@NoArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(length = 30)
    private String numero;

    @Column(length = 20)
    private String tipoContrato;

    @Column(length = 100)
    private String puesto;

    @Column(length = 50)
    private String jornada;

    private Double salario;

    private String fechaInicio;

    private String fechaFin;

    @Column(length = 20)
    private String estado = "ACTIVO";
}
package pe.utp.centro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "terapeuta")
@Getter
@Setter
@NoArgsConstructor
public class Terapeuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(length = 30)
    private String colegiatura;

    public String getNombre() {
        return usuario != null ? usuario.getNombre() : null;
    }
}

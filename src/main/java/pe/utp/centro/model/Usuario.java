package pe.utp.centro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String dni;

    private LocalDate fechaNacimiento;

    @Column(length = 1)
    private String genero;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    public enum Rol {
        ADMIN, RECEPCIONISTA, TERAPEUTA, APODERADO
    }

    public String getNombreCompleto() {
        return nombre;
    }
}

package pe.utp.centro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sesion")
@Getter
@Setter
@NoArgsConstructor
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    @JsonIgnore
    private Cita cita;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NivelAvance nivelAvance;

    private Boolean asistio;

    private Integer duracionMinutos;

    // Diagnostico (archivo subido por el terapeuta)
    @Column(length = 255)
    private String nombreArchivo;

    private Long tamanoArchivo;

    private String fechaDiagnostico;

    @Lob
    @Column(name = "datos_archivo")
    private byte[] datosArchivo;

    public enum NivelAvance {
        INICIAL, EN_PROGRESO, LOGRADO, AVANZADO, COMPLETADO
    }
}

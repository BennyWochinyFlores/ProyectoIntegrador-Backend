package pe.utp.centro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NinoRequest(
        @NotBlank @Size(min = 8, max = 8) String dni,
        @NotBlank @Size(max = 60) String apellidoPaterno,
        @NotBlank @Size(max = 60) String apellidoMaterno,
        @NotBlank @Size(max = 120) String nombres,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Size(max = 1) String genero,
        @NotBlank @Size(max = 20) String parentesco
) {}
package pe.utp.centro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NinoRequest(
        @NotBlank @Size(max = 120) String nombres,
        @NotNull @Past LocalDate fechaNacimiento
) {}
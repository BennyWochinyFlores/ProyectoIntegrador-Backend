package pe.utp.centro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank String nombres,
        @NotBlank String apellidoPaterno,
        @NotBlank String apellidoMaterno,
        String dni,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
        String telefono,
        String fechaNacimiento,
        String genero
) {}

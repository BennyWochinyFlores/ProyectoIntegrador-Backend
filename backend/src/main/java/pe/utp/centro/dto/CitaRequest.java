package pe.utp.centro.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull Long ninoId,
        @NotNull Long terapeutaId,
        @NotNull Long especialidadId,
        @NotNull LocalDateTime fechaHora
) {}

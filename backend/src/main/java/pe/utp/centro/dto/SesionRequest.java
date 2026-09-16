package pe.utp.centro.dto;

import jakarta.validation.constraints.NotNull;
import pe.utp.centro.model.Sesion;

public record SesionRequest(
        @NotNull Long citaId,
        String observaciones,
        @NotNull Sesion.NivelAvance nivelAvance
) {}

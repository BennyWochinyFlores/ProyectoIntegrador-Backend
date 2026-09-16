package pe.utp.centro.dto;

import pe.utp.centro.model.Cita;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        String nino,
        String terapeuta,
        String especialidad,
        LocalDateTime fechaHora,
        String estado
) {
    public static CitaResponse from(Cita c) {
        return new CitaResponse(
                c.getId(),
                c.getNino().getNombres(),
                c.getTerapeuta().getUsuario().getNombre(),
                c.getEspecialidad().getNombre(),
                c.getFechaHora(),
                c.getEstado().name()
        );
    }
}

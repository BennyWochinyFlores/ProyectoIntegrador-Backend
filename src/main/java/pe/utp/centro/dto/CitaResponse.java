package pe.utp.centro.dto;

import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Nino;
import pe.utp.centro.model.Sesion;

import java.time.LocalDateTime;
import java.util.List;

public record CitaResponse(
        Long id,
        NinoInfo nino,
        String terapeuta,
        Long terapeutaId,
        String especialidad,
        Integer duracionMinutos,
        Double precio,
        LocalDateTime fechaHora,
        String estado,
        String nivelAvance,
        String observaciones,
        Boolean asistio,
        String siguienteNivel,
        String diagnosticoNombre,
        Long diagnosticoTamano,
        String diagnosticoFecha
) {
    public record NinoInfo(
            Long id,
            String nombreCompleto,
            String dni,
            String genero,
            String fechaNacimiento,
            String parentesco,
            ApoderadoInfo apoderado
    ) {}

    public record ApoderadoInfo(
            String nombre
    ) {}

    private static final List<Sesion.NivelAvance> NIVEL_ORDEN = List.of(
            Sesion.NivelAvance.INICIAL, Sesion.NivelAvance.EN_PROGRESO,
            Sesion.NivelAvance.AVANZADO, Sesion.NivelAvance.COMPLETADO);

    /** Siguiente nivel en la cadena INICIAL -> EN_PROGRESO -> AVANZADO -> COMPLETADO. */
    public static String siguienteNivelDe(Sesion.NivelAvance nivel) {
        if (nivel == null) return Sesion.NivelAvance.INICIAL.name();
        int i = NIVEL_ORDEN.indexOf(nivel);
        return i < NIVEL_ORDEN.size() - 1 ? NIVEL_ORDEN.get(i + 1).name() : nivel.name();
    }

    /** Devuelve una copia con otro nivel sugerido para la proxima sesion. */
    public CitaResponse conSiguienteNivel(String nuevo) {
        return new CitaResponse(
                id, nino, terapeuta, terapeutaId, especialidad, duracionMinutos, precio,
                fechaHora, estado, nivelAvance, observaciones, asistio, nuevo,
                diagnosticoNombre, diagnosticoTamano, diagnosticoFecha
        );
    }

    public static CitaResponse from(Cita c) {
        String nivelAvance = null;
        String observaciones = null;
        Boolean asistio = null;
        Integer duracionSesion = null;
        String siguienteNivel = null;
        String nombreArchivo = null;
        Long tamanoArchivo = null;
        String fechaDiagnostico = null;
        if (c.getSesion() != null) {
            Sesion s = c.getSesion();
            nivelAvance = s.getNivelAvance() != null ? s.getNivelAvance().name() : null;
            observaciones = s.getObservaciones();
            asistio = s.getAsistio();
            duracionSesion = s.getDuracionMinutos();
            siguienteNivel = siguienteNivelDe(s.getNivelAvance());
            nombreArchivo = s.getNombreArchivo();
            tamanoArchivo = s.getTamanoArchivo();
            fechaDiagnostico = s.getFechaDiagnostico();
        }
        Nino nino = c.getNino();
        ApoderadoInfo apoderadoInfo = new ApoderadoInfo(nino.getApoderado().getNombre());
        NinoInfo ninoInfo = new NinoInfo(
                nino.getId(),
                nino.getNombreCompleto(),
                nino.getDni(),
                nino.getGenero(),
                nino.getFechaNacimiento() != null ? nino.getFechaNacimiento().toString() : null,
                nino.getParentesco(),
                apoderadoInfo
        );
        return new CitaResponse(
                c.getId(),
                ninoInfo,
                c.getTerapeuta().getUsuario().getNombre(),
                c.getTerapeuta().getId(),
                c.getEspecialidad().getNombre(),
                duracionSesion != null ? duracionSesion : c.getEspecialidad().getDuracionMinutos(),
                c.getEspecialidad().getPrecio() != null ? c.getEspecialidad().getPrecio().doubleValue() : null,
                c.getFechaHora(),
                c.getEstado().name(),
                nivelAvance,
                observaciones,
                asistio,
                siguienteNivel != null ? siguienteNivel : NIVEL_ORDEN.get(0).name(),
                nombreArchivo,
                tamanoArchivo,
                fechaDiagnostico
        );
    }
}

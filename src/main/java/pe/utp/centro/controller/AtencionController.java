package pe.utp.centro.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.CitaResponse;
import pe.utp.centro.model.Cita;
import pe.utp.centro.model.Sesion;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.repository.NinoRepository;
import pe.utp.centro.repository.PagoRepository;
import pe.utp.centro.repository.SesionRepository;
import pe.utp.centro.model.Pago;
import pe.utp.centro.service.CitaService;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/atenciones")
public class AtencionController {

    private final CitaRepository citaRepository;
    private final NinoRepository ninoRepository;
    private final SesionRepository sesionRepository;
    private final PagoRepository pagoRepository;
    private final CitaService citaService;

    public AtencionController(CitaRepository citaRepository,
                              NinoRepository ninoRepository,
                              SesionRepository sesionRepository,
                              PagoRepository pagoRepository,
                              CitaService citaService) {
        this.citaRepository = citaRepository;
        this.ninoRepository = ninoRepository;
        this.sesionRepository = sesionRepository;
        this.pagoRepository = pagoRepository;
        this.citaService = citaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CitaResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        List<Cita> citas = switch (usuario.getRol()) {
            case ADMIN, RECEPCIONISTA -> citaRepository.findAll();
            case APODERADO -> citaRepository.findAll().stream()
                    .filter(c -> c.getNino().getApoderado().getId().equals(usuario.getId()))
                    .toList();
            case TERAPEUTA -> citaRepository.findAll().stream()
                    .filter(c -> c.getTerapeuta().getUsuario().getId().equals(usuario.getId()))
                    .toList();
        };
        return citas.stream().map(CitaResponse::from).toList();
    }

    @GetMapping("/nino/{ninoId}")
    @PreAuthorize("isAuthenticated()")
    public List<CitaResponse> atencionesDeNino(@PathVariable Long ninoId,
                                               @AuthenticationPrincipal Usuario usuario) {
        ninoRepository.findById(ninoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Niño no encontrado"));

        List<Cita> citas = citaRepository.findAll().stream()
                .filter(c -> c.getNino().getId().equals(ninoId))
                .filter(c -> puedeAcceder(c, usuario))
                .toList();

        return citas.stream().map(CitaResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CitaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        validarAcceso(cita, usuario);

        CitaResponse respuesta = CitaResponse.from(cita);

        // Si la cita aun no tiene sesion, el nivel sugerido es el siguiente de la MISMA terapia
        if (cita.getSesion() == null) {
            List<Sesion> previas = sesionesPreviasDeLaTerapia(cita, cita.getId());
            Sesion.NivelAvance ultimo = previas.isEmpty() ? null : previas.get(previas.size() - 1).getNivelAvance();
            respuesta = respuesta.conSiguienteNivel(CitaResponse.siguienteNivelDe(ultimo));
        }

        return respuesta;
    }

    /** Confirmar asistencia de la cita (pendiente -> confirmada). */
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public CitaResponse confirmar(@PathVariable Long id) {
        Cita cita = obtenerCita(id);
        return CitaResponse.from(citaService.confirmar(cita));
    }

    /** Cancelar la cita (salvo que ya esté atendida). */
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public CitaResponse cancelar(@PathVariable Long id) {
        Cita cita = obtenerCita(id);
        if (cita.getEstado() == Cita.Estado.ATENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar una atención ya atendida.");
        }
        return CitaResponse.from(citaService.cancelar(cita));
    }

    /** Reprogramar la cita en una nueva fecha y hora, validando disponibilidad del terapeuta. */
    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public CitaResponse reprogramar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Cita cita = obtenerCita(id);
        if (cita.getEstado() == Cita.Estado.ATENDIDA || cita.getEstado() == Cita.Estado.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reprogramar una atención cancelada o atendida.");
        }
        LocalDateTime nuevaFechaHora;
        try {
            nuevaFechaHora = LocalDateTime.parse(String.valueOf(body.get("fechaHora")));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de fecha y hora inválido.");
        }
        if (nuevaFechaHora.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La nueva fecha debe ser futura.");
        }
        try {
            return CitaResponse.from(citaService.reprogramar(cita, nuevaFechaHora));
        } catch (CitaService.HorarioNoDisponibleException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    /** Registrar la sesión: asistencia, duración efectiva y observaciones (terapeuta). */
    @PutMapping("/{id}/atender")
    @PreAuthorize("hasRole('TERAPEUTA') or hasRole('ADMIN')")
    public CitaResponse atender(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                @AuthenticationPrincipal Usuario usuario) {
        Cita cita = obtenerCita(id);
        if (usuario.getRol() == Usuario.Rol.TERAPEUTA
                && !cita.getTerapeuta().getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes registrar tus propias atenciones.");
        }

        boolean asistio = Boolean.TRUE.equals(body.get("asistio"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("asistio")));
        int duracionMinutos;
        try {
            duracionMinutos = Integer.parseInt(String.valueOf(body.get("duracionMinutos")));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duración inválida.");
        }
        if (duracionMinutos < 5 || duracionMinutos > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La duración debe estar entre 5 y 240 minutos.");
        }
        String observaciones = body.get("observaciones") != null ? String.valueOf(body.get("observaciones")) : null;

        final Long citaId = cita.getId();
        Sesion sesion = sesionRepository.findByCita(cita).orElseGet(Sesion::new);
        sesion.setCita(cita);
        sesion.setAsistio(asistio);
        sesion.setDuracionMinutos(duracionMinutos);
        sesion.setObservaciones(observaciones);

        // Progresion estricta por terapia: INICIAL -> EN_PROGRESO -> AVANZADO -> COMPLETADO
        sesion.setNivelAvance(nivelProgresivo(sesionesPreviasDeLaTerapia(cita, citaId)));
        sesionRepository.save(sesion);
        cita.setSesion(sesion);

        cita.setEstado(asistio ? Cita.Estado.ATENDIDA : Cita.Estado.CANCELADA);
        cita = citaRepository.save(cita);

        // Si asistio, generar el pago pendiente de la sesion (si aun no existe)
        if (asistio && cita.getEspecialidad().getPrecio() != null) {
            boolean yaExiste = pagoRepository.findAll().stream()
                    .anyMatch(p -> p.getCita() != null && p.getCita().getId().equals(citaId));
            if (!yaExiste) {
                Pago pago = new Pago();
                pago.setCita(cita);
                pago.setMonto(cita.getEspecialidad().getPrecio());
                pago.setEstado(Pago.Estado.PENDIENTE);
                pago.setFecha(java.time.LocalDateTime.now());
                pagoRepository.save(pago);
            }
        }

        return CitaResponse.from(cita);
    }

    /** Subir el diagnóstico de la sesión (terapeuta). */
    @PutMapping("/{id}/diagnostico")
    @PreAuthorize("hasRole('TERAPEUTA') or hasRole('ADMIN')")
    public Map<String, Object> subirDiagnostico(@PathVariable Long id,
                                                @RequestParam("archivo") MultipartFile archivo,
                                                @AuthenticationPrincipal Usuario usuario) {
        final Cita cita = obtenerCita(id);
        if (usuario.getRol() == Usuario.Rol.TERAPEUTA
                && !cita.getTerapeuta().getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes subir el diagnóstico de tus propias atenciones.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ningún archivo.");
        }

        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo.");
        }
        validarContenidoWord(archivo.getOriginalFilename(), datos);

        Sesion sesion = sesionRepository.findByCita(cita).orElseGet(() -> {
            Sesion nueva = new Sesion();
            nueva.setCita(cita);
            nueva.setNivelAvance(Sesion.NivelAvance.INICIAL);
            return nueva;
        });
        sesion.setDatosArchivo(datos);
        sesion.setNombreArchivo(nombreSeguro(archivo.getOriginalFilename()));
        sesion.setTamanoArchivo((long) datos.length);
        sesion.setFechaDiagnostico(LocalDateTime.now().toString());
        sesionRepository.save(sesion);
        cita.setSesion(sesion);

        return Map.of(
                "nombre", sesion.getNombreArchivo(),
                "tamano", sesion.getTamanoArchivo(),
                "fecha", sesion.getFechaDiagnostico()
        );
    }

    /** Descargar el diagnóstico de la sesión. */
    @GetMapping("/{id}/diagnostico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarDiagnostico(@PathVariable Long id,
                                                       @AuthenticationPrincipal Usuario usuario) {
        Cita cita = obtenerCita(id);
        validarAcceso(cita, usuario);
        Sesion sesion = sesionRepository.findByCita(cita).orElse(null);
        if (sesion == null || sesion.getDatosArchivo() == null || sesion.getDatosArchivo().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta atención no tiene diagnóstico subido.");
        }

        String nombre = nombreSeguro(sesion.getNombreArchivo());
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment().filename(nombre).build().toString())
                .header("Content-Type", "application/octet-stream")
                .body(sesion.getDatosArchivo());
    }

    /**
     * Sesiones atendidas previas del mismo niño en la MISMA terapia, ordenadas por fecha.
     * La progresion de niveles es por terapia, no global del niño.
     */
    private List<Sesion> sesionesPreviasDeLaTerapia(Cita cita, Long idCitaExcluir) {
        return sesionRepository.findByCita_Nino(cita.getNino()).stream()
                .filter(s -> s.getCita() != null && s.getCita().getEspecialidad() != null
                        && s.getCita().getEspecialidad().getId().equals(cita.getEspecialidad().getId())
                        && !s.getCita().getId().equals(idCitaExcluir))
                .filter(s -> Boolean.TRUE.equals(s.getAsistio()))
                .sorted(java.util.Comparator.comparing(s -> s.getCita().getFechaHora()))
                .toList();
    }

    /**
     * Nivel de la sesion segun la progresion:
     * 1ra -> INICIAL; 2da -> EN_PROGRESO; 3ra -> AVANZADO; 4ta+ -> COMPLETADO (no retrocede).
     */
    private Sesion.NivelAvance nivelProgresivo(List<Sesion> previas) {
        if (previas.isEmpty()) return Sesion.NivelAvance.INICIAL;

        Sesion.NivelAvance ultimo = previas.get(previas.size() - 1).getNivelAvance();
        if (ultimo == null || ultimo == Sesion.NivelAvance.INICIAL) {
            return Sesion.NivelAvance.EN_PROGRESO;
        }
        if (ultimo == Sesion.NivelAvance.EN_PROGRESO) {
            return Sesion.NivelAvance.AVANZADO;
        }
        // AVANZADO, COMPLETADO o LOGRADO (valor antiguo): la terapia termina en COMPLETADO
        return Sesion.NivelAvance.COMPLETADO;
    }

    /** Control de titularidad: ADMIN/RECEPCIÓN ven todo; el resto solo lo propio. */
    private boolean puedeAcceder(Cita cita, Usuario usuario) {
        if (usuario == null) return false;
        return switch (usuario.getRol()) {
            case ADMIN, RECEPCIONISTA -> true;
            case APODERADO -> cita.getNino() != null && cita.getNino().getApoderado() != null
                    && cita.getNino().getApoderado().getId().equals(usuario.getId());
            case TERAPEUTA -> cita.getTerapeuta() != null && cita.getTerapeuta().getUsuario() != null
                    && cita.getTerapeuta().getUsuario().getId().equals(usuario.getId());
        };
    }

    private void validarAcceso(Cita cita, Usuario usuario) {
        if (!puedeAcceder(cita, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta atención");
        }
    }

    /** Normaliza el nombre de archivo para evitar rutas relativas, comillas y CRLF. */
    private String nombreSeguro(String nombre) {
        if (nombre == null || nombre.isBlank()) return "diagnostico.docx";
        String limpio = Paths.get(nombre).getFileName().toString();
        limpio = limpio.replaceAll("[\r\n\"\\\\/]", "_").trim();
        return limpio.isEmpty() ? "diagnostico.docx" : limpio;
    }

    /** Valida en servidor que el archivo sea realmente un Word (.doc OLE2 o .docx ZIP). */
    private void validarContenidoWord(String nombre, byte[] datos) {
        String seguro = nombreSeguro(nombre).toLowerCase();
        if (!seguro.endsWith(".doc") && !seguro.endsWith(".docx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El diagnóstico debe ser un archivo Word (.doc o .docx).");
        }
        if (datos.length > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El diagnóstico no debe superar los 5 MB.");
        }
        boolean ole2 = datos.length >= 8
                && (datos[0] & 0xFF) == 0xD0 && (datos[1] & 0xFF) == 0xCF
                && (datos[2] & 0xFF) == 0x11 && (datos[3] & 0xFF) == 0xE0
                && (datos[4] & 0xFF) == 0xA1 && (datos[5] & 0xFF) == 0xB1
                && (datos[6] & 0xFF) == 0x1A && (datos[7] & 0xFF) == 0xE1;
        boolean zip = datos.length >= 4
                && (datos[0] & 0xFF) == 0x50 && (datos[1] & 0xFF) == 0x4B
                && (datos[2] & 0xFF) == 0x03 && (datos[3] & 0xFF) == 0x04;
        if (!ole2 && !zip) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El contenido del archivo no corresponde a un documento Word válido.");
        }
    }

    private Cita obtenerCita(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
    }
}

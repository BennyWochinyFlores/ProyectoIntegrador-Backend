package pe.utp.centro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.utp.centro.dto.PagoResponse;
import pe.utp.centro.model.Pago;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.CitaRepository;
import pe.utp.centro.repository.PagoRepository;
import pe.utp.centro.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modulo de pagos (RF-05). Cada sesion atendida genera un pago pendiente que
 * el apoderado efectua (efectivo o transferencia con boleta y nro de operacion).
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    public PagoController(PagoRepository pagoRepository,
                          CitaRepository citaRepository,
                          UsuarioRepository usuarioRepository) {
        this.pagoRepository = pagoRepository;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Historial completo de pagos (admin/recepcionista) con filtros de fecha. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public List<PagoResponse> listar(@RequestParam(required = false) LocalDateTime desde,
                                     @RequestParam(required = false) LocalDateTime hasta) {
        List<Pago> pagos = pagoRepository.findAll().stream()
                .filter(p -> desde == null || !p.getFecha().isBefore(desde))
                .filter(p -> hasta == null || !p.getFecha().isAfter(hasta))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .toList();
        return pagos.stream().map(PagoResponse::from).toList();
    }

    /** Pagos de los hijos del apoderado autenticado. */
    @GetMapping("/mios")
    @PreAuthorize("hasRole('APODERADO')")
    public List<PagoResponse> mios(@AuthenticationPrincipal Usuario usuario) {
        return pagoRepository.findAll().stream()
                .filter(p -> p.getCita() != null && p.getCita().getNino() != null
                        && p.getCita().getNino().getApoderado().getId().equals(usuario.getId()))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(PagoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PagoResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        validarAcceso(pago, usuario);
        return PagoResponse.from(pago);
    }

    /** Efectuar un pago pendiente: metodo + datos de boleta si fue transferencia. */
    @PutMapping("/{id}/pagar")
    @PreAuthorize("hasRole('APODERADO') or hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public PagoResponse pagar(@PathVariable Long id, @RequestBody Map<String, Object> body,
                              @AuthenticationPrincipal Usuario usuario) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        validarAcceso(pago, usuario);

        if (pago.getEstado() == Pago.Estado.PAGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pago ya fue efectuado.");
        }

        Pago.Metodo metodo;
        try {
            metodo = Pago.Metodo.valueOf(String.valueOf(body.get("metodo")).toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metodo de pago invalido.");
        }
        String numeroBoleta = body.get("numeroBoleta") != null ? String.valueOf(body.get("numeroBoleta")) : null;
        String numeroOperacion = body.get("numeroOperacion") != null ? String.valueOf(body.get("numeroOperacion")) : null;

        if (metodo == Pago.Metodo.TRANSFERENCIA
                && ((numeroBoleta == null || numeroBoleta.isEmpty())
                    || (numeroOperacion == null || numeroOperacion.isEmpty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para transferencia se requiere numero de boleta y numero de operacion.");
        }

        pago.setMetodo(metodo);
        pago.setEstado(Pago.Estado.PAGADO);
        pago.setFecha(LocalDateTime.now());
        pago.setNumeroBoleta(numeroBoleta != null && !numeroBoleta.isEmpty() ? numeroBoleta : null);
        pago.setNumeroOperacion(numeroOperacion != null && !numeroOperacion.isEmpty() ? numeroOperacion : null);
        pago = pagoRepository.save(pago);
        return PagoResponse.from(pago);
    }

    private void validarAcceso(Pago pago, Usuario usuario) {
        boolean permite = switch (usuario.getRol()) {
            case ADMIN, RECEPCIONISTA -> true;
            case APODERADO -> pago.getCita() != null && pago.getCita().getNino() != null
                    && pago.getCita().getNino().getApoderado().getId().equals(usuario.getId());
            case TERAPEUTA -> pago.getCita() != null
                    && pago.getCita().getTerapeuta().getUsuario().getId().equals(usuario.getId());
        };
        if (!permite) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este pago");
        }
    }
}

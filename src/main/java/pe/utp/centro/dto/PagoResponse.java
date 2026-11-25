package pe.utp.centro.dto;

import pe.utp.centro.model.Pago;

import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        CitaResponse atencion,
        Double monto,
        String estado,
        String metodo,
        LocalDateTime fechaPago,
        String numeroBoleta,
        String numeroOperacion
) {
    public static PagoResponse from(Pago p) {
        return new PagoResponse(
                p.getId(),
                CitaResponse.from(p.getCita()),
                p.getMonto() != null ? p.getMonto().doubleValue() : null,
                p.getEstado() != null ? p.getEstado().name() : Pago.Estado.PENDIENTE.name(),
                p.getMetodo() != null ? p.getMetodo().name() : null,
                p.getFecha(),
                p.getNumeroBoleta(),
                p.getNumeroOperacion()
        );
    }
}

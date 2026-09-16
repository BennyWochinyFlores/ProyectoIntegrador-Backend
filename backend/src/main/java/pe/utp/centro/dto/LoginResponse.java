package pe.utp.centro.dto;

import pe.utp.centro.model.Usuario;

public record LoginResponse(
        String token,
        Usuario usuario
) {
    public static LoginResponse of(String token, Usuario usuario) {
        return new LoginResponse(token, usuario);
    }
}
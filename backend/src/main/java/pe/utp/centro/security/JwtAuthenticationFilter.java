package pe.utp.centro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.utp.centro.model.Usuario;
import pe.utp.centro.repository.UsuarioRepository;

import java.io.IOException;
import java.util.List;

/**
 * Valida el token JWT de cada peticion (Authorization: Bearer <token>) y carga
 * al usuario autenticado en el SecurityContext (RNF-04, HU-01/HU-02).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && jwtUtil.esValido(header.substring(7))) {
            String email = jwtUtil.extraerEmail(header.substring(7));
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
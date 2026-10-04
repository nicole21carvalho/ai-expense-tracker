package br.com.nicole.cofre.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Roda uma vez por requisicao, le o header Authorization e popula o contexto.
 * Se o token faltar ou for invalido o filtro nao bloqueia aqui: ele apenas
 * deixa o contexto vazio e o SecurityConfig decide o que exige autenticacao.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";
    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIXO)) {
            try {
                Claims claims = jwtService.extrairClaims(header.substring(PREFIXO.length()));
                UsuarioAutenticado usuario = new UsuarioAutenticado(
                        claims.get("uid", Number.class).longValue(),
                        claims.getSubject());

                var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                // Token adulterado, expirado ou malformado: segue sem autenticacao.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}

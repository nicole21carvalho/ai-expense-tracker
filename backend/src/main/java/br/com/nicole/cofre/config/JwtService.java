package br.com.nicole.cofre.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Gera e valida o token.
 *
 * Por que JWT e nao sessao em memoria? Sessao obriga o servidor a guardar
 * estado, e com mais de uma instancia atras de um load balancer cada
 * requisicao pode cair em uma instancia diferente que nao conhece a sessao.
 * O JWT carrega a identidade assinada: qualquer instancia valida sozinha.
 *
 * O preco e que nao da para invalidar um token antes de ele expirar. Por isso
 * a expiracao e curta (2 horas). Um sistema que precise de logout imediato
 * usa lista de revogacao no Redis ou volta para sessao.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    // HS256 exige chave de pelo menos 256 bits.
    private static final int TAMANHO_MINIMO_BYTES = 32;

    public JwtService(@Value("${cofre.jwt.secret:}") String secret,
                      @Value("${cofre.jwt.expiracao-minutos}") long expiracaoMinutos) {
        // Sem valor padrao de proposito. Uma chave que esta no repositorio e
        // publica: com ela qualquer um assina um token com o uid que quiser.
        // Melhor a aplicacao nao subir do que subir aberta.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "COFRE_JWT_SECRET ausente ou com menos de " + TAMANHO_MINIMO_BYTES
                    + " bytes. Gere uma com: openssl rand -base64 48");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(String email, Long usuarioId) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", usuarioId)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

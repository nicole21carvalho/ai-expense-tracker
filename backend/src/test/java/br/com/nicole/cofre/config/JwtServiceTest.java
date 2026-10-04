package br.com.nicole.cofre.config;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CHAVE = "chave-usada-apenas-nos-testes-com-mais-de-32-bytes";

    @Test
    @DisplayName("recusa subir sem chave")
    void recusaChaveVazia() {
        assertThatThrownBy(() -> new JwtService("", 120))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COFRE_JWT_SECRET");
    }

    @Test
    @DisplayName("recusa chave com menos de 32 bytes")
    void recusaChaveCurta() {
        assertThatThrownBy(() -> new JwtService("a".repeat(31), 120))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("token gerado e lido de volta com a mesma chave")
    void geraEValida() {
        JwtService service = new JwtService(CHAVE, 120);
        String token = service.gerarToken("ana@exemplo.com", 42L);

        assertThat(service.extrairClaims(token).getSubject()).isEqualTo("ana@exemplo.com");
        assertThat(service.extrairClaims(token).get("uid", Number.class).longValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("token assinado com outra chave e rejeitado")
    void rejeitaTokenDeOutraChave() {
        String forjado = new JwtService("outra-chave-qualquer-tambem-com-32-bytes-ou-mais", 120)
                .gerarToken("ana@exemplo.com", 42L);

        assertThatThrownBy(() -> new JwtService(CHAVE, 120).extrairClaims(forjado))
                .isInstanceOf(SignatureException.class);
    }
}

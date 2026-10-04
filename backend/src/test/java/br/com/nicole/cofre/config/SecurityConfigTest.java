package br.com.nicole.cofre.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O front encerra a sessao quando recebe 401. Se a API voltar a responder
 * 403 para token ausente ou expirado, o usuario fica preso numa tela que
 * nao carrega nada. Estes testes seguram esse contrato.
 */
@SpringBootTest(properties = "cofre.jwt.secret=" + SecurityConfigTest.CHAVE)
@AutoConfigureMockMvc
class SecurityConfigTest {

    static final String CHAVE = "chave-usada-apenas-nos-testes-com-mais-de-32-bytes";

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    @Test
    @DisplayName("sem token responde 401 com WWW-Authenticate e o formato de erro da API")
    void semToken() throws Exception {
        mvc.perform(get("/api/transacoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
    }

    @Test
    @DisplayName("token assinado com outra chave responde 401")
    void tokenDeOutraChave() throws Exception {
        String forjado = new JwtService("outra-chave-qualquer-tambem-com-32-bytes-ou-mais", 120)
                .gerarToken("ana@exemplo.com", 1L);

        mvc.perform(get("/api/transacoes").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token expirado responde 401")
    void tokenExpirado() throws Exception {
        String expirado = new JwtService(CHAVE, -1).gerarToken("ana@exemplo.com", 1L);

        mvc.perform(get("/api/transacoes").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("console do H2 e actuator nao ficam publicos")
    void semRotasPublicasEsquecidas() throws Exception {
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API proibe ser embutida em iframe")
    void bloqueiaIframe() throws Exception {
        mvc.perform(get("/api/transacoes"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("token valido passa")
    void tokenValido() throws Exception {
        mvc.perform(get("/api/transacoes")
                        .header("Authorization", "Bearer " + jwt.gerarToken("ana@exemplo.com", 1L)))
                .andExpect(status().isOk());
    }
}

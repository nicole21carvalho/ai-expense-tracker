package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrada invalida tem que voltar 400 com o formato de erro da API. Erro 500
 * aqui era pior que feio: o reencaminhamento para /error respondia 401, e o
 * front deslogava o usuario por ter digitado um valor grande.
 */
@SpringBootTest(properties = "cofre.jwt.secret=chave-usada-apenas-nos-testes-com-mais-de-32-bytes")
@AutoConfigureMockMvc
class ValidacaoEntradaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    @ParameterizedTest(name = "valor {0} e recusado com 400")
    @ValueSource(strings = {"1000000000000", "1e20", "10.123"})
    void recusaValorForaDaColuna(String valor) throws Exception {
        lancar(valor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.startsWith("valor:")));
    }

    @Test
    @DisplayName("o maior valor que cabe na coluna e aceito")
    void aceitaLimite() throws Exception {
        // Token do proprio cadastro: a transacao precisa de um usuario que exista.
        String resposta = cadastrar("limite@x.com", "senha-forte-123")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(resposta, "$.token");

        lancar("999999999999.99", token).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("JSON malformado de usuario logado volta 400, nao 401")
    void jsonMalformadoNaoDesloga() throws Exception {
        mvc.perform(post("/api/transacoes")
                        .header("Authorization", "Bearer " + jwt.gerarToken("ana@x.com", 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\": \"isto nao e numero\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"));
    }

    @Test
    @DisplayName("/error chamado de fora continua exigindo autenticacao")
    void errorDeForaExigeLogin() throws Exception {
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("senha com menos de 72 caracteres mas mais de 72 bytes volta 400")
    void senhaAcimaDe72Bytes() throws Exception {
        cadastrar("acentos@x.com", "ç".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Senha longa demais")));
    }

    @Test
    @DisplayName("senha acentuada de ate 72 bytes e aceita")
    void senhaAcentuadaNoLimite() throws Exception {
        cadastrar("acentos-ok@x.com", "ç".repeat(36)).andExpect(status().isCreated());
    }

    // Os casos recusados param na validacao, antes do banco: o uid nao precisa existir.
    private ResultActions lancar(String valor) throws Exception {
        return lancar(valor, jwt.gerarToken("qualquer@x.com", 1L));
    }

    private ResultActions lancar(String valor, String token) throws Exception {
        return mvc.perform(post("/api/transacoes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"teste\",\"valor\":%s,\"tipo\":\"DESPESA\",\"data\":\"2026-10-01\"}"
                        .formatted(valor)));
    }

    private ResultActions cadastrar(String email, String senha) throws Exception {
        // IP proprio: o limite de cadastro por IP e compartilhado no contexto de teste.
        return mvc.perform(post("/api/auth/cadastro")
                .with(r -> { r.setRemoteAddr("10.0.9." + Math.abs(email.hashCode() % 250)); return r; })
                .contentType("application/json;charset=UTF-8")
                .content("{\"nome\":\"Teste\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }
}

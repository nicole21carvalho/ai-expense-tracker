package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.service.Categorizador;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsentimentoIaTest {

    private static final String CHAVE = "cofre.jwt.secret=chave-usada-apenas-nos-testes-com-mais-de-32-bytes";

    /**
     * Modo IA. O categorizador e um mock que se declara externo: assim da para
     * provar que ele nao e chamado sem consentimento, sem rede de verdade.
     */
    @Nested
    @SpringBootTest(properties = CHAVE)
    @AutoConfigureMockMvc
    @DisplayName("com categorizacao por IA")
    class ComIa {

        @Autowired
        MockMvc mvc;

        @MockitoBean
        Categorizador ia;

        @BeforeEach
        void configurarMock() {
            reset(ia);
            when(ia.enviaDadosParaFora()).thenReturn(true);
            when(ia.sugerir(any(), anyList())).thenReturn(Optional.of("Lazer"));
        }

        @Test
        @DisplayName("nada vai para a IA sem consentimento; aceitar liga, revogar desliga")
        void consentimentoControlaEnvio() throws Exception {
            String token = cadastrar(mvc, "10.0.20.1", "consente@x.com");

            // Conta nova: sem consentimento. A sugestao vem das regras locais.
            privacidade(mvc, token)
                    .andExpect(jsonPath("$.categorizacaoPorIa").value(true))
                    .andExpect(jsonPath("$.provedorIa").value("Anthropic"))
                    .andExpect(jsonPath("$.consentimentoIa").value(false))
                    .andExpect(jsonPath("$.consentimentoIaEm").doesNotExist());
            lancar(mvc, token, "IFOOD PIZZARIA").andExpect(jsonPath("$.categoriaNome").value("Alimentação"));
            verify(ia, never()).sugerir(any(), anyList());

            // Aceita: a proxima sugestao passa pela IA.
            consentir(mvc, token, true)
                    .andExpect(jsonPath("$.consentimentoIa").value(true))
                    .andExpect(jsonPath("$.consentimentoIaEm").isNotEmpty());
            lancar(mvc, token, "IFOOD PIZZARIA").andExpect(jsonPath("$.categoriaNome").value("Lazer"));
            verify(ia, times(1)).sugerir(any(), anyList());

            // Revoga: volta para as regras locais, sem nova chamada a IA.
            consentir(mvc, token, false)
                    .andExpect(jsonPath("$.consentimentoIa").value(false))
                    .andExpect(jsonPath("$.consentimentoIaEm").doesNotExist());
            lancar(mvc, token, "IFOOD PIZZARIA").andExpect(jsonPath("$.categoriaNome").value("Alimentação"));
            verify(ia, times(1)).sugerir(any(), anyList());
        }

        @Test
        @DisplayName("aceitar de novo mantem a data do consentimento original")
        void reaceitarNaoMudaData() throws Exception {
            String token = cadastrar(mvc, "10.0.20.2", "reaceita@x.com");
            String primeira = JsonPath.read(consentir(mvc, token, true).andReturn()
                    .getResponse().getContentAsString(), "$.consentimentoIaEm");
            consentir(mvc, token, true).andExpect(jsonPath("$.consentimentoIaEm").value(primeira));
        }

        @Test
        @DisplayName("consentimento exige o campo aceito")
        void exigeCampo() throws Exception {
            String token = cadastrar(mvc, "10.0.20.3", "semcampo@x.com");
            mvc.perform(put("/api/privacidade/consentimento-ia")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @SpringBootTest(properties = {CHAVE, "cofre.ia.api-key="})
    @AutoConfigureMockMvc
    @DisplayName("sem IA configurada")
    class SemIa {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("informa que nada sai da aplicacao, e o front nao precisa pedir consentimento")
        void informaSemIa() throws Exception {
            String token = cadastrar(mvc, "10.0.21.1", "semia@x.com");
            privacidade(mvc, token).andExpect(jsonPath("$.categorizacaoPorIa").value(false));
        }
    }

    private static String cadastrar(MockMvc mvc, String ip, String email) throws Exception {
        String corpo = mvc.perform(post("/api/auth/cadastro")
                        .with(r -> { r.setRemoteAddr(ip); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste\",\"email\":\"%s\",\"senha\":\"senha-forte-123\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private static ResultActions privacidade(MockMvc mvc, String token) throws Exception {
        return mvc.perform(get("/api/privacidade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private static ResultActions consentir(MockMvc mvc, String token, boolean aceito) throws Exception {
        return mvc.perform(put("/api/privacidade/consentimento-ia")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceito\":" + aceito + "}"))
                .andExpect(status().isOk());
    }

    private static ResultActions lancar(MockMvc mvc, String token, String descricao) throws Exception {
        return mvc.perform(post("/api/transacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"%s\",\"valor\":10,\"tipo\":\"DESPESA\",\"data\":\"2026-10-01\"}"
                                .formatted(descricao)))
                .andExpect(status().isCreated());
    }
}

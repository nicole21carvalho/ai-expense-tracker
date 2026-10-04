package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O front (interface Pagina em modelos.ts) depende deste formato. Mudar a
 * forma de serializar a Page quebra a tela sem erro de compilacao em lado
 * nenhum, entao o contrato fica preso aqui.
 */
@SpringBootTest(properties = "cofre.jwt.secret=chave-usada-apenas-nos-testes-com-mais-de-32-bytes")
@AutoConfigureMockMvc
class ContratoPaginaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    @Test
    @DisplayName("listagem devolve content e a paginacao separada em page")
    void formatoDaPagina() throws Exception {
        mvc.perform(get("/api/transacoes")
                        .header("Authorization", "Bearer " + jwt.gerarToken("ana@x.com", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").isNumber())
                .andExpect(jsonPath("$.page.number").isNumber())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber())
                // Campos da PageImpl crua, que o formato estavel nao tem mais.
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist());
    }
}

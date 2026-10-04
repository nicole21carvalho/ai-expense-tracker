package br.com.nicole.cofre.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os contadores vivem no contexto do Spring, que e compartilhado entre os
 * testes. Por isso cada teste usa IP e e-mail proprios.
 */
@SpringBootTest(properties = "cofre.jwt.secret=chave-usada-apenas-nos-testes-com-mais-de-32-bytes")
@AutoConfigureMockMvc
class AuthLimitesTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("5 senhas erradas bloqueiam o e-mail, ate com a senha certa, e o bloqueio vale entre IPs")
    void bloqueiaEmailAposFalhas() throws Exception {
        cadastrar("10.0.1.0", "alvo@x.com");

        for (int i = 1; i <= 5; i++) {
            // Um IP por tentativa: simula ataque distribuido, que o limite por IP nao pega.
            login("10.0.1." + i, "alvo@x.com", "errada-" + i).andExpect(status().isBadRequest());
        }

        login("10.0.1.99", "alvo@x.com", SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Muitas tentativas")));
    }

    @Test
    @DisplayName("o limite por e-mail ignora maiusculas")
    void emailSemDiferencaDeCaixa() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("10.0.2." + i, i % 2 == 0 ? "Caixa@X.com" : "caixa@x.com", "errada")
                    .andExpect(status().isBadRequest());
        }
        login("10.0.2.99", "CAIXA@X.COM", "errada").andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("e-mail sem conta bloqueia igual, para o bloqueio nao revelar quem tem conta")
    void emailInexistenteBloqueiaIgual() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("10.0.3." + i, "ninguem@x.com", "errada").andExpect(status().isBadRequest());
        }
        login("10.0.3.99", "ninguem@x.com", "errada").andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("login certo nao gasta o limite de falhas do e-mail")
    void sucessoNaoConta() throws Exception {
        cadastrar("10.0.4.0", "frequente@x.com");
        for (int i = 1; i <= 8; i++) {
            login("10.0.4." + i, "frequente@x.com", SENHA).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("um IP faz no maximo 10 logins por minuto, mesmo trocando de e-mail")
    void limitaLoginPorIp() throws Exception {
        for (int i = 1; i <= 10; i++) {
            login("10.0.5.1", "conta" + i + "@x.com", "errada").andExpect(status().isBadRequest());
        }
        login("10.0.5.1", "outra@x.com", "errada")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Outro IP segue livre.
        login("10.0.5.2", "outra@x.com", "errada").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("um IP cria no maximo 5 contas por hora")
    void limitaCadastroPorIp() throws Exception {
        for (int i = 1; i <= 5; i++) {
            cadastrar("10.0.6.1", "nova" + i + "@x.com").andExpect(status().isCreated());
        }
        cadastrar("10.0.6.1", "nova6@x.com").andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String ip, String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    private ResultActions cadastrar(String ip, String email) throws Exception {
        return mvc.perform(post("/api/auth/cadastro")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)));
    }
}

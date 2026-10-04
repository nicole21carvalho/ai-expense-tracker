package br.com.nicole.cofre.service;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Categoriza chamando um modelo de linguagem.
 *
 * Tres decisoes que valem explicar numa entrevista:
 *
 * 1. O prompt restringe a resposta a uma das categorias que o usuario ja tem,
 *    mais a palavra NENHUMA. Modelo solto inventa categoria nova e polui a base.
 * 2. Qualquer falha devolve Optional.empty() em vez de propagar excecao.
 *    A categorizacao e um extra: se a IA cair, o lancamento tem que salvar
 *    mesmo assim, sem categoria.
 * 3. A descricao da transacao vai para um servico externo. Isso e um dado
 *    pessoal, entao so e chamado com consentimento explicito do usuario
 *    (LGPD, art. 7, I): enviaDadosParaFora() devolve true e o TransacaoService
 *    usa as regras locais para quem nao consentiu.
 */
public class CategorizadorPorIA implements Categorizador {

    private static final Logger log = LoggerFactory.getLogger(CategorizadorPorIA.class);

    private final RestClient client;
    private final String apiKey;
    private final String modelo;

    public CategorizadorPorIA(String endpoint, String apiKey, String modelo) {
        this.client = RestClient.builder().baseUrl(endpoint).build();
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public boolean enviaDadosParaFora() {
        return true;
    }

    @Override
    public Optional<String> sugerir(String descricao, List<String> categoriasDisponiveis) {
        if (categoriasDisponiveis.isEmpty()) {
            return Optional.empty();
        }

        String prompt = """
                Classifique a transacao financeira abaixo em exatamente uma das categorias da lista.
                Responda apenas com o nome da categoria, sem pontuacao e sem explicacao.
                Se nenhuma servir, responda NENHUMA.

                Categorias: %s
                Transacao: %s
                """.formatted(String.join(", ", categoriasDisponiveis), descricao);

        try {
            JsonNode resposta = client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(Map.of(
                            "model", modelo,
                            "max_tokens", 20,
                            "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve()
                    .body(JsonNode.class);

            String texto = resposta.path("content").path(0).path("text").asString("").trim();

            // Nunca confie na saida do modelo: valide contra a lista antes de gravar.
            return categoriasDisponiveis.stream()
                    .filter(c -> c.equalsIgnoreCase(texto))
                    .findFirst();

        } catch (Exception e) {
            log.warn("Categorizacao por IA falhou, seguindo sem categoria: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

package br.com.nicole.cofre.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fallback por palavra-chave. Usado quando cofre.ia.api-key esta vazio
 * (a escolha fica em CategorizadorConfig).
 *
 * Resolve uns 60% dos casos reais e custa zero. Vale como linha de base:
 * sem ele nao da para dizer se o modelo esta de fato ajudando.
 */
public class CategorizadorPorRegras implements Categorizador {

    private record Regra(String categoria, List<Pattern> palavras) {}

    // Lista, e nao Map.of: a ordem e o criterio de desempate e precisa ser
    // fixa. O Map.of sorteia a ordem de iteracao a cada JVM, e "Pagamento de
    // Condomínio" saia Moradia numa execucao e Salário na seguinte.
    //
    // Salário fica por ultimo de proposito: as palavras dele ("pagamento",
    // "deposito") sao as mais genericas e aparecem em contas a pagar.
    private static final List<Regra> REGRAS = List.of(
            regra("Alimentação", "ifood", "restaurante", "padaria", "mercado", "lanche", "lanchonete", "pizza"),
            regra("Transporte",  "uber", "ubertrip", "99", "99app", "combustivel", "gasolina", "posto", "onibus", "metro", "estacionamento"),
            regra("Moradia",     "aluguel", "condominio", "luz", "agua", "gas", "internet"),
            regra("Saúde",       "farmacia", "drogaria", "consulta", "exame", "plano de saude"),
            regra("Educação",    "faculdade", "curso", "livro", "mensalidade", "udemy", "alura"),
            regra("Lazer",       "cinema", "netflix", "spotify", "jogo", "bar", "show"),
            regra("Salário",     "salario", "pagamento", "pix recebido", "deposito")
    );

    /**
     * Cada palavra-chave casa so como palavra inteira, aceitando plural com
     * "s" ("exames" casa com "exame"). Antes era trecho de texto, e "bar"
     * achava "Barbearia", "gas" achava "gasolina" (que ia para Moradia) e
     * "agua" achava "aguardando".
     */
    private static Regra regra(String categoria, String... palavras) {
        return new Regra(categoria, Arrays.stream(palavras)
                .map(p -> Pattern.compile("\\b" + Pattern.quote(p) + "s?\\b"))
                .toList());
    }

    /**
     * Vence a categoria com mais palavras encontradas na descricao. Em empate,
     * vence a que vem antes em REGRAS (o ">" estrito mantem a primeira).
     */
    @Override
    public Optional<String> sugerir(String descricao, List<String> categoriasDisponiveis) {
        String texto = paraBusca(descricao);

        Optional<String> melhor = Optional.empty();
        long melhorPontuacao = 0;

        for (Regra regra : REGRAS) {
            long pontuacao = regra.palavras().stream().filter(p -> p.matcher(texto).find()).count();
            if (pontuacao > melhorPontuacao) {
                Optional<String> doUsuario = nomeDoUsuario(regra.categoria(), categoriasDisponiveis);
                if (doUsuario.isPresent()) {
                    melhor = doUsuario;
                    melhorPontuacao = pontuacao;
                }
            }
        }
        return melhor;
    }

    /**
     * Compara o nome sem acento e sem caixa, e devolve o nome do jeito que o
     * usuario tem. Assim "Saúde", "Saude" (conta antiga) e "saude" (criada a
     * mao) batem com a mesma regra.
     */
    private Optional<String> nomeDoUsuario(String categoria, List<String> categoriasDisponiveis) {
        String alvo = normalizar(categoria);
        return categoriasDisponiveis.stream()
                .filter(c -> normalizar(c).equals(alvo))
                .findFirst();
    }

    /**
     * Prepara a descricao para a busca por palavra: sem acento e caixa, numeros
     * com separador juntados ("R$ 1,99" vira "199", senao o "99" casaria com a
     * regra de Transporte) e qualquer pontuacao vira um espaco.
     */
    private String paraBusca(String descricao) {
        return normalizar(descricao)
                .replaceAll("(\\d)[.,](?=\\d)", "$1")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** Remove acento e caixa para "Farmácia" bater com "farmacia". */
    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }
}

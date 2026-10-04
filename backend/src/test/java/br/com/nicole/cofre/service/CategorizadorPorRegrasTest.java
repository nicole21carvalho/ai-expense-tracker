package br.com.nicole.cofre.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizadorPorRegrasTest {

    private final CategorizadorPorRegras categorizador = new CategorizadorPorRegras();
    private final List<String> disponiveis =
            List.of("Alimentação", "Transporte", "Moradia", "Lazer");

    @Test
    @DisplayName("reconhece palavra-chave direta na descricao")
    void reconhecePalavraChave() {
        assertThat(categorizador.sugerir("IFOOD *LANCHONETE SP", disponiveis))
                .contains("Alimentação");
    }

    @Test
    @DisplayName("ignora acento e caixa ao comparar")
    void ignoraAcentoECaixa() {
        assertThat(categorizador.sugerir("Pagamento de Condomínio", disponiveis))
                .contains("Moradia");
    }

    @Test
    @DisplayName("nao sugere categoria que o usuario nao possui")
    void naoSugereCategoriaInexistente() {
        // "drogaria" mapeia para Saúde, que nao esta na lista do usuario.
        assertThat(categorizador.sugerir("DROGARIA PACHECO", disponiveis))
                .isEmpty();
    }

    @Test
    @DisplayName("aceita a categoria sem acento de conta antiga e devolve o nome como o usuario tem")
    void aceitaNomeSemAcento() {
        assertThat(categorizador.sugerir("DROGARIA PACHECO", List.of("Saude", "Lazer")))
                .contains("Saude");
        assertThat(categorizador.sugerir("Mensalidade faculdade", List.of("educacao")))
                .contains("educacao");
    }

    // As 7 categorias que o cadastro cria: o caso real, onde as regras disputam.
    private final List<String> todas = List.of(
            "Alimentação", "Transporte", "Moradia", "Saúde", "Educação", "Lazer", "Salário");

    @Test
    @DisplayName("pagamento de conta nao vira salario: em empate, Salário perde por ser o mais generico")
    void pagamentoDeContaNaoEhSalario() {
        // "pagamento" (Salário) e "condominio" (Moradia): 1 a 1, desempata a ordem.
        assertThat(categorizador.sugerir("Pagamento de Condomínio", todas)).contains("Moradia");
    }

    @Test
    @DisplayName("salario de verdade vence por ter mais palavras encontradas")
    void salarioVencePorPontos() {
        assertThat(categorizador.sugerir("Pagamento de salário", todas)).contains("Salário");
    }

    @Test
    @DisplayName("mais palavras encontradas vence mesmo uma categoria que vem antes na ordem")
    void pontuacaoVemAntesDaOrdem() {
        // Transporte vem antes de Saúde, mas Saúde acha 2 palavras e Transporte 1.
        assertThat(categorizador.sugerir("Consulta e exame, ida de Uber", todas)).contains("Saúde");
    }

    @Test
    @DisplayName("categoria que o usuario nao tem nao impede a proxima melhor")
    void pulaCategoriaAusente() {
        List<String> semSaude = List.of("Transporte", "Lazer");
        assertThat(categorizador.sugerir("Consulta e exame, ida de Uber", semSaude)).contains("Transporte");
    }

    @ParameterizedTest(name = "\"{0}\" nao vira {1}")
    @DisplayName("palavra-chave dentro de outra palavra nao conta")
    @CsvSource({
            // Todos estes casavam no codigo antigo, que buscava trecho de texto.
            "Barbearia do Zé,               Lazer",
            "Aguardando compensação,        Moradia",
            "Gastos diversos,               Moradia",
            "'Compra de R$ 1,99 na loja',   Transporte",
            "Metrópole Imóveis,             Transporte",
            "Showroom de móveis,            Lazer",
            "Salão da Luzia,                Moradia",
    })
    void naoCasaPedacoDePalavra(String descricao, String categoriaErrada) {
        assertThat(categorizador.sugerir(descricao, todas)).isNotEqualTo(Optional.of(categoriaErrada));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("palavra inteira, plural, valor com centavos e expressao de varias palavras")
    @CsvSource({
            "Gasolina no posto,              Transporte",
            "LANCHONETE DO ZE,               Alimentação",
            "99 Taxi,                        Transporte",
            "UBERTRIP SAO PAULO,             Transporte",
            "99APP *99APP,                   Transporte",
            "Exames laboratoriais,           Saúde",
            "Cursos online,                  Educação",
            "Plano de saúde Unimed,          Saúde",
            "PIX RECEBIDO - JOAO,            Salário",
            "'Mercado R$ 1.299,99',          Alimentação",
            "IFOOD*RESTAURANTE,              Alimentação",
            "Conta de água e luz,            Moradia",
    })
    void casaPalavraInteira(String descricao, String esperada) {
        assertThat(categorizador.sugerir(descricao, todas)).contains(esperada);
    }

    @Test
    @DisplayName("devolve vazio quando nada bate")
    void devolveVazioSemCorrespondencia() {
        assertThat(categorizador.sugerir("TED RECEBIDA 4471", disponiveis))
                .isEqualTo(Optional.empty());
    }
}

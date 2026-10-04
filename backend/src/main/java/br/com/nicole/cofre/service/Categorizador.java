package br.com.nicole.cofre.service;

import java.util.List;
import java.util.Optional;

/**
 * Contrato da categorizacao. Existem duas implementacoes e o Spring escolhe
 * uma por configuracao. Isto e o ponto da interface: a aplicacao roda sem
 * chave de IA, e os testes rodam sem chamar rede.
 */
public interface Categorizador {

    /**
     * @return o nome da categoria sugerida, ou vazio quando nao houver
     *         confianca suficiente para sugerir.
     */
    Optional<String> sugerir(String descricao, List<String> categoriasDisponiveis);

    /**
     * Se a descricao sai da aplicacao para um servico de terceiro. Quando sim,
     * so pode ser usado com consentimento do usuario (LGPD); o TransacaoService
     * confere isso antes de chamar.
     */
    default boolean enviaDadosParaFora() {
        return false;
    }
}

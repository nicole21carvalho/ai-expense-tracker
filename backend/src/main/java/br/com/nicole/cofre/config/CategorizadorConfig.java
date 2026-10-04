package br.com.nicole.cofre.config;

import br.com.nicole.cofre.service.Categorizador;
import br.com.nicole.cofre.service.CategorizadorPorIA;
import br.com.nicole.cofre.service.CategorizadorPorRegras;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe a implementacao de Categorizador pela chave de IA.
 *
 * Por que um @Bean e nao @ConditionalOnProperty nas classes? Sem havingValue,
 * o @ConditionalOnProperty aceita qualquer valor diferente de "false",
 * inclusive vazio, e nao existe forma de dizer "nao vazio" com ele. O
 * resultado eram os dois beans ativos e a aplicacao sem subir. Aqui a regra
 * fica explicita e so existe um Categorizador no contexto.
 */
@Configuration
public class CategorizadorConfig {

    @Bean
    Categorizador categorizador(@Value("${cofre.ia.api-key:}") String apiKey,
                                @Value("${cofre.ia.endpoint}") String endpoint,
                                @Value("${cofre.ia.modelo}") String modelo) {
        // Chave em branco cai no modo por regras, que nao envia nada para fora.
        return apiKey.isBlank()
                ? new CategorizadorPorRegras()
                : new CategorizadorPorIA(endpoint, apiKey, modelo);
    }
}

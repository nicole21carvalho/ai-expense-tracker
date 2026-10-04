package br.com.nicole.cofre;

import br.com.nicole.cofre.service.Categorizador;
import br.com.nicole.cofre.service.CategorizadorPorIA;
import br.com.nicole.cofre.service.CategorizadorPorRegras;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o contexto inteiro. Sem este teste o mvn verify passava com a
 * aplicacao incapaz de iniciar: dois Categorizador ativos ao mesmo tempo.
 */
class CofreApplicationTest {

    private static final String CHAVE_JWT =
            "cofre.jwt.secret=chave-usada-apenas-nos-testes-com-mais-de-32-bytes";

    @Nested
    @SpringBootTest(properties = {CHAVE_JWT, "cofre.ia.api-key="})
    @DisplayName("sem chave de IA")
    class SemChaveDeIa {

        @Autowired
        Categorizador categorizador;

        @Autowired
        ApplicationContext contexto;

        @Test
        @DisplayName("sobe e usa o categorizador por regras, que nao envia dados para fora")
        void usaRegras() {
            assertThat(categorizador).isInstanceOf(CategorizadorPorRegras.class);
        }

        @Test
        @DisplayName("nao cria usuario em memoria, cuja senha o Spring escreveria no log")
        void semUsuarioEmMemoria() {
            assertThat(contexto.getBeansOfType(UserDetailsService.class)).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(properties = {CHAVE_JWT, "cofre.ia.api-key=chave-falsa-de-teste"})
    @DisplayName("com chave de IA")
    class ComChaveDeIa {

        @Autowired
        Categorizador categorizador;

        @Test
        @DisplayName("sobe e usa o categorizador por IA")
        void usaIa() {
            assertThat(categorizador).isInstanceOf(CategorizadorPorIA.class);
        }
    }
}

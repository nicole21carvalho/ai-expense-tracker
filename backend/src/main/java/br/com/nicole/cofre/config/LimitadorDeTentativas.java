package br.com.nicole.cofre.config;

import br.com.nicole.cofre.exception.MuitasTentativasException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Limita tentativas nos endpoints publicos de autenticacao.
 *
 * Sem isto, /login aceita quantas senhas o atacante conseguir mandar, e
 * /cadastro permite criar contas em massa. Cada chave (IP ou e-mail) tem um
 * balde de fichas que se recarrega com o tempo; acabou a ficha, a API
 * responde 429 ate recarregar.
 *
 * Os contadores ficam em memoria, entao valem por instancia. Com mais de uma
 * instancia atras de um balanceador, eles precisariam ir para um armazenamento
 * compartilhado (Redis, por exemplo), que o Bucket4j tambem suporta.
 */
@Component
public class LimitadorDeTentativas {

    public enum Regra {
        // Segura um unico IP testando muitas senhas ou muitas contas.
        LOGIN_POR_IP(10, Duration.ofMinutes(1)),
        // Segura forca bruta contra uma conta vinda de varios IPs. Conta so
        // falhas: quem acerta a senha nao gasta ficha.
        LOGIN_FALHO_POR_EMAIL(5, Duration.ofMinutes(15)),
        // Segura criacao de contas em massa e reduz a enumeracao de e-mails
        // pelo cadastro.
        CADASTRO_POR_IP(5, Duration.ofHours(1));

        private final int limite;
        private final Duration janela;

        Regra(int limite, Duration janela) {
            this.limite = limite;
            this.janela = janela;
        }
    }

    // Tamanho maximo para um atacante nao estourar a memoria mandando chaves
    // novas sem parar. Um balde parado por mais que a maior janela ja estaria
    // cheio de novo, entao pode ser descartado sem mudar o resultado.
    private final Cache<String, Bucket> baldes = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    /** Gasta uma ficha; sem ficha, lanca MuitasTentativasException. */
    public void consumir(Regra regra, String chave) {
        ConsumptionProbe probe = balde(regra, chave).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new MuitasTentativasException(emSegundos(probe.getNanosToWaitForRefill()));
        }
    }

    /** So confere se ha ficha, sem gastar. Usado antes de saber se foi falha. */
    public void exigirDisponivel(Regra regra, String chave) {
        EstimationProbe probe = balde(regra, chave).estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            throw new MuitasTentativasException(emSegundos(probe.getNanosToWaitForRefill()));
        }
    }

    /** Gasta uma ficha sem bloquear agora; o bloqueio vem na proxima tentativa. */
    public void registrarFalha(Regra regra, String chave) {
        balde(regra, chave).tryConsume(1);
    }

    private Bucket balde(Regra regra, String chave) {
        return baldes.get(regra.name() + ":" + chave, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(regra.limite)
                        .refillGreedy(regra.limite, regra.janela)
                        .build())
                .build());
    }

    private static long emSegundos(long nanos) {
        // Arredonda para cima: dizer "0 segundos" faria o cliente tentar cedo demais.
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanos + 999_999_999L));
    }
}

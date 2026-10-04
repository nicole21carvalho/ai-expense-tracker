package br.com.nicole.cofre.dto;

import br.com.nicole.cofre.domain.TipoTransacao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs como records aninhados. Entidade JPA nunca sai do controller: se
 * sair, o JSON vaza senha_hash, dispara lazy loading fora da transacao e
 * acopla o contrato da API ao schema do banco.
 */
public final class Dtos {

    private Dtos() {}

    public record CadastroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72) String senha) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha) {}

    public record TokenResponse(String token, String nome, String email) {}

    public record TransacaoRequest(
            @NotBlank @Size(max = 255) String descricao,
            // Espelha a coluna NUMERIC(14,2): 12 digitos inteiros e 2 decimais.
            // Sem isto, valor grande estourava no banco (erro 500) e 10.123 era
            // gravado como 10.12 enquanto a resposta dizia 10.123.
            @NotNull @DecimalMin(value = "0.01")
            @Digits(integer = 12, fraction = 2,
                    message = "deve ter no máximo 12 dígitos antes da vírgula e 2 depois")
            BigDecimal valor,
            @NotNull TipoTransacao tipo,
            @NotNull @PastOrPresent LocalDate data,
            Long categoriaId) {}

    public record TransacaoResponse(
            Long id,
            String descricao,
            BigDecimal valor,
            TipoTransacao tipo,
            LocalDate data,
            Long categoriaId,
            String categoriaNome,
            boolean categoriaPorIa) {}

    public record CategoriaRequest(
            @NotBlank @Size(max = 80) String nome,
            @Pattern(regexp = "^#([0-9a-fA-F]{6})$") String cor) {}

    public record CategoriaResponse(Long id, String nome, String cor) {}

    public record FatiaResumo(String categoria, BigDecimal total) {}

    public record ResumoResponse(
            BigDecimal totalReceitas,
            BigDecimal totalDespesas,
            BigDecimal saldo,
            List<FatiaResumo> despesasPorCategoria) {}

    public record ErroResponse(String mensagem, List<String> detalhes) {}

    /**
     * categorizacaoPorIa diz se a aplicacao esta configurada com IA; quando
     * false, nada sai para fora e o front nem pede consentimento.
     */
    public record PrivacidadeResponse(
            boolean categorizacaoPorIa,
            String provedorIa,
            boolean consentimentoIa,
            LocalDateTime consentimentoIaEm) {}

    public record ConsentimentoIaRequest(@NotNull Boolean aceito) {}
}

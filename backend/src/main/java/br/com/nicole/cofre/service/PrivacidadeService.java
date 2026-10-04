package br.com.nicole.cofre.service;

import br.com.nicole.cofre.domain.Usuario;
import br.com.nicole.cofre.dto.Dtos.PrivacidadeResponse;
import br.com.nicole.cofre.exception.RecursoNaoEncontradoException;
import br.com.nicole.cofre.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Consentimento para enviar a descricao dos lancamentos ao servico de IA.
 * Revogar tem que ser tao facil quanto aceitar (LGPD, art. 8, par. 5): os
 * dois sao a mesma chamada, so muda o valor.
 */
@Service
public class PrivacidadeService {

    private final UsuarioRepository usuarios;
    private final Categorizador categorizador;
    private final String provedorIa;

    public PrivacidadeService(UsuarioRepository usuarios, Categorizador categorizador,
                              @Value("${cofre.ia.provedor}") String provedorIa) {
        this.usuarios = usuarios;
        this.categorizador = categorizador;
        this.provedorIa = provedorIa;
    }

    @Transactional(readOnly = true)
    public PrivacidadeResponse consultar(Long usuarioId) {
        return paraResponse(buscar(usuarioId));
    }

    @Transactional
    public PrivacidadeResponse definirConsentimentoIa(Long usuarioId, boolean aceito) {
        Usuario usuario = buscar(usuarioId);
        // Aceitar de novo nao muda a data: ela registra quando o consentimento
        // que esta valendo foi dado.
        if (aceito && usuario.getConsentimentoIaEm() == null) {
            // Truncado em segundos: o relogio da JVM tem mais casas do que a
            // coluna guarda, e a resposta mostraria um horario diferente do
            // gravado, que e o que vale como prova.
            usuario.setConsentimentoIaEm(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        } else if (!aceito) {
            usuario.setConsentimentoIaEm(null);
        }
        return paraResponse(usuario);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private PrivacidadeResponse paraResponse(Usuario u) {
        return new PrivacidadeResponse(
                categorizador.enviaDadosParaFora(),
                provedorIa,
                u.getConsentimentoIaEm() != null,
                u.getConsentimentoIaEm());
    }
}

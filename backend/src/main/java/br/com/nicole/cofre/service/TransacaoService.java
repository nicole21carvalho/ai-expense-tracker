package br.com.nicole.cofre.service;

import br.com.nicole.cofre.domain.Categoria;
import br.com.nicole.cofre.domain.TipoTransacao;
import br.com.nicole.cofre.domain.Transacao;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.exception.RecursoNaoEncontradoException;
import br.com.nicole.cofre.repository.CategoriaRepository;
import br.com.nicole.cofre.repository.TransacaoRepository;
import br.com.nicole.cofre.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransacaoService {

    private final TransacaoRepository transacoes;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final Categorizador categorizador;
    // Usado no lugar da IA para quem nao consentiu. E local e sem estado.
    private final Categorizador semEnvioExterno = new CategorizadorPorRegras();

    public TransacaoService(TransacaoRepository transacoes, CategoriaRepository categorias,
                            UsuarioRepository usuarios, Categorizador categorizador) {
        this.transacoes = transacoes;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.categorizador = categorizador;
    }

    /**
     * O categorizador configurado so e usado se nao mandar dados para fora ou
     * se o usuario consentiu. Sem consentimento, as regras locais sugerem: o
     * usuario continua com sugestao, so que nada sai da aplicacao.
     */
    private Categorizador categorizadorPara(Long usuarioId) {
        if (!categorizador.enviaDadosParaFora()) {
            return categorizador;
        }
        boolean consentiu = usuarios.findById(usuarioId)
                .map(u -> u.getConsentimentoIaEm() != null)
                .orElse(false);
        return consentiu ? categorizador : semEnvioExterno;
    }

    @Transactional(readOnly = true)
    public Page<TransacaoResponse> listar(Long usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        Page<Transacao> pagina = (inicio != null && fim != null)
                ? transacoes.findByUsuarioIdAndDataBetweenOrderByDataDesc(usuarioId, inicio, fim, pageable)
                : transacoes.findByUsuarioIdOrderByDataDesc(usuarioId, pageable);
        return pagina.map(this::paraResponse);
    }

    @Transactional
    public TransacaoResponse criar(Long usuarioId, TransacaoRequest req) {
        Categoria categoria = null;
        boolean porIa = false;

        if (req.categoriaId() != null) {
            categoria = categorias.findByIdAndUsuarioId(req.categoriaId(), usuarioId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
        } else {
            // Usuario nao escolheu categoria: pede sugestao. Falha aqui nao
            // impede o lancamento de ser salvo sem categoria.
            List<Categoria> disponiveis = categorias.findByUsuarioIdOrderByNome(usuarioId);
            Optional<String> sugestao = categorizadorPara(usuarioId).sugerir(
                    req.descricao(), disponiveis.stream().map(Categoria::getNome).toList());

            if (sugestao.isPresent()) {
                categoria = disponiveis.stream()
                        .filter(c -> c.getNome().equalsIgnoreCase(sugestao.get()))
                        .findFirst().orElse(null);
                porIa = categoria != null;
            }
        }

        Transacao salva = transacoes.save(Transacao.builder()
                .descricao(req.descricao())
                .valor(req.valor())
                .tipo(req.tipo())
                .data(req.data())
                .categoria(categoria)
                .categoriaPorIa(porIa)
                .usuario(usuarios.getReferenceById(usuarioId))
                .build());

        return paraResponse(salva);
    }

    @Transactional
    public TransacaoResponse atualizar(Long usuarioId, Long id, TransacaoRequest req) {
        Transacao transacao = transacoes.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada"));

        transacao.setDescricao(req.descricao());
        transacao.setValor(req.valor());
        transacao.setTipo(req.tipo());
        transacao.setData(req.data());

        if (req.categoriaId() != null) {
            transacao.setCategoria(categorias.findByIdAndUsuarioId(req.categoriaId(), usuarioId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada")));
            // Usuario corrigiu a mao: deixa de ser sugestao da maquina.
            transacao.setCategoriaPorIa(false);
        }

        return paraResponse(transacao);
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Transacao transacao = transacoes.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada"));
        transacoes.delete(transacao);
    }

    @Transactional(readOnly = true)
    public ResumoResponse resumo(Long usuarioId, LocalDate inicio, LocalDate fim) {
        var pagina = transacoes.findByUsuarioIdAndDataBetweenOrderByDataDesc(
                usuarioId, inicio, fim, Pageable.unpaged());

        BigDecimal receitas = somar(pagina.getContent(), TipoTransacao.RECEITA);
        BigDecimal despesas = somar(pagina.getContent(), TipoTransacao.DESPESA);

        List<FatiaResumo> porCategoria = transacoes
                .totalDespesaPorCategoria(usuarioId, inicio, fim).stream()
                .map(linha -> new FatiaResumo((String) linha[0], (BigDecimal) linha[1]))
                .toList();

        return new ResumoResponse(receitas, despesas, receitas.subtract(despesas), porCategoria);
    }

    private BigDecimal somar(List<Transacao> lista, TipoTransacao tipo) {
        return lista.stream()
                .filter(t -> t.getTipo() == tipo)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransacaoResponse paraResponse(Transacao t) {
        return new TransacaoResponse(
                t.getId(), t.getDescricao(), t.getValor(), t.getTipo(), t.getData(),
                t.getCategoria() == null ? null : t.getCategoria().getId(),
                t.getCategoria() == null ? null : t.getCategoria().getNome(),
                t.isCategoriaPorIa());
    }
}

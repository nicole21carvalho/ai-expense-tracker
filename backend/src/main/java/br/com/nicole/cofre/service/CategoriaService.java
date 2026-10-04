package br.com.nicole.cofre.service;

import br.com.nicole.cofre.domain.Categoria;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.exception.RecursoNaoEncontradoException;
import br.com.nicole.cofre.exception.RegraNegocioException;
import br.com.nicole.cofre.repository.CategoriaRepository;
import br.com.nicole.cofre.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;

    public CategoriaService(CategoriaRepository categorias, UsuarioRepository usuarios) {
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Long usuarioId) {
        return categorias.findByUsuarioIdOrderByNome(usuarioId).stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNome(), c.getCor()))
                .toList();
    }

    @Transactional
    public CategoriaResponse criar(Long usuarioId, CategoriaRequest req) {
        categorias.findByNomeIgnoreCaseAndUsuarioId(req.nome(), usuarioId)
                .ifPresent(c -> { throw new RegraNegocioException("Categoria já existe"); });

        Categoria salva = categorias.save(Categoria.builder()
                .nome(req.nome())
                .cor(req.cor() == null ? "#6B7280" : req.cor())
                .usuario(usuarios.getReferenceById(usuarioId))
                .build());

        return new CategoriaResponse(salva.getId(), salva.getNome(), salva.getCor());
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Categoria categoria = categorias.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
        categorias.delete(categoria);
    }
}

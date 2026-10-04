package br.com.nicole.cofre.repository;

import br.com.nicole.cofre.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByUsuarioIdOrderByNome(Long usuarioId);
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Categoria> findByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);
}

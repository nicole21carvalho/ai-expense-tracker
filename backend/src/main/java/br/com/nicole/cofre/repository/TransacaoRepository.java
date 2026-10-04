package br.com.nicole.cofre.repository;

import br.com.nicole.cofre.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    // Toda consulta filtra por usuario. Esquecer esse filtro em um unico
    // metodo basta para um usuario ler os dados de outro.
    Page<Transacao> findByUsuarioIdOrderByDataDesc(Long usuarioId, Pageable pageable);

    Optional<Transacao> findByIdAndUsuarioId(Long id, Long usuarioId);

    Page<Transacao> findByUsuarioIdAndDataBetweenOrderByDataDesc(
            Long usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable);

    @Query("""
           SELECT COALESCE(t.categoria.nome, 'Sem categoria') AS categoria,
                  SUM(t.valor) AS total
           FROM Transacao t
           WHERE t.usuario.id = :usuarioId
             AND t.tipo = br.com.nicole.cofre.domain.TipoTransacao.DESPESA
             AND t.data BETWEEN :inicio AND :fim
           GROUP BY t.categoria.nome
           ORDER BY SUM(t.valor) DESC
           """)
    List<Object[]> totalDespesaPorCategoria(@Param("usuarioId") Long usuarioId,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fim") LocalDate fim);
}

package br.com.nicole.cofre.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacao")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String descricao;

    // BigDecimal e nao double. Dinheiro em ponto flutuante acumula erro de
    // arredondamento: 0.1 + 0.2 nao da 0.3 em double. Em sistema financeiro
    // isso vira divergencia de centavos no fechamento.
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(nullable = false)
    private LocalDate data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Marca se a categoria veio do modelo ou do usuario. Serve para medir
    // o acerto da IA depois e para mostrar na interface que foi sugestao.
    @Column(name = "categoria_por_ia", nullable = false)
    private boolean categoriaPorIa;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }
}

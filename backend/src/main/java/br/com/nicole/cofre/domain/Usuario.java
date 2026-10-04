package br.com.nicole.cofre.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // Guarda o hash BCrypt, nunca a senha. Se este campo vazar, o atacante
    // ainda precisa quebrar cada hash individualmente.
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    // Quando o usuario aceitou enviar descricoes ao servico de IA. Nulo quando
    // nunca aceitou ou revogou; so com valor aqui algo sai para fora.
    @Column(name = "consentimento_ia_em")
    private LocalDateTime consentimentoIaEm;

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }
}

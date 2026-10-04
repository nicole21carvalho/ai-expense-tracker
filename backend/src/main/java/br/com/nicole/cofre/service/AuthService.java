package br.com.nicole.cofre.service;

import br.com.nicole.cofre.config.JwtService;
import br.com.nicole.cofre.domain.Categoria;
import br.com.nicole.cofre.domain.Usuario;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.exception.RegraNegocioException;
import br.com.nicole.cofre.repository.CategoriaRepository;
import br.com.nicole.cofre.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final List<String> CATEGORIAS_PADRAO = List.of(
            "Alimentação", "Transporte", "Moradia", "Saúde", "Educação", "Lazer", "Salário");

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, CategoriaRepository categorias,
                       PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.encoder = encoder;
        this.jwt = jwt;
        // Gerado pelo mesmo encoder, entao tem o mesmo custo dos hashes reais.
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TokenResponse cadastrar(CadastroRequest req) {
        // O BCrypt so usa 72 bytes e o Spring Security recusa senha maior com
        // excecao (erro 500). O @Size do DTO conta caracteres, nao bytes: 40
        // letras acentuadas ja passam de 72 bytes em UTF-8.
        if (req.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException(
                    "Senha longa demais: use até 72 bytes (letras acentuadas contam como 2)");
        }


        if (usuarios.existsByEmail(req.email())) {
            // Esta mensagem revela que o e-mail tem conta. Esconder exigiria
            // confirmar o cadastro por e-mail, que o projeto nao tem; o limite
            // CADASTRO_POR_IP torna caro usar isto para varrer uma lista.
            throw new RegraNegocioException("Já existe conta com este e-mail");
        }

        Usuario usuario = usuarios.save(Usuario.builder()
                .nome(req.nome())
                .email(req.email())
                .senhaHash(encoder.encode(req.senha()))
                .build());

        // Conta nova sem categoria nenhuma deixa a tela inicial vazia e a
        // categorizacao sem opcoes. Semeia o basico no cadastro.
        CATEGORIAS_PADRAO.forEach(nome -> categorias.save(Categoria.builder()
                .nome(nome)
                .cor("#6B7280")
                .usuario(usuario)
                .build()));

        return new TokenResponse(
                jwt.gerarToken(usuario.getEmail(), usuario.getId()),
                usuario.getNome(), usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Optional<Usuario> encontrado = usuarios.findByEmail(req.email());

        // O BCrypt roda sempre, mesmo sem conta. Se rodasse so quando o e-mail
        // existe, a resposta para conta real demoraria mais e o tempo entregaria
        // o que a mensagem generica esconde.
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = encoder.matches(req.senha(), hash);

        if (encontrado.isEmpty() || !senhaConfere) {
            // Mensagem generica de proposito. Dizer "e-mail nao existe"
            // entrega ao atacante quais contas sao validas.
            throw new RegraNegocioException("E-mail ou senha inválidos");
        }

        Usuario usuario = encontrado.get();
        return new TokenResponse(
                jwt.gerarToken(usuario.getEmail(), usuario.getId()),
                usuario.getNome(), usuario.getEmail());
    }
}

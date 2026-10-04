package br.com.nicole.cofre.service;

import br.com.nicole.cofre.config.JwtService;
import br.com.nicole.cofre.dto.Dtos.LoginRequest;
import br.com.nicole.cofre.exception.RegraNegocioException;
import br.com.nicole.cofre.repository.CategoriaRepository;
import br.com.nicole.cofre.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    @DisplayName("e-mail sem conta tambem passa pelo BCrypt, para o tempo nao revelar quem tem conta")
    void rodaBcryptSemConta() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("$2a$10$hash-ficticio");
        when(usuarios.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        AuthService service = new AuthService(
                usuarios, mock(CategoriaRepository.class), encoder, mock(JwtService.class));

        assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@x.com", "qualquer")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("E-mail ou senha inválidos");

        verify(encoder, times(1)).matches(eq("qualquer"), eq("$2a$10$hash-ficticio"));
    }
}

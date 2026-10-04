package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.LimitadorDeTentativas;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.exception.RegraNegocioException;
import br.com.nicole.cofre.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

import static br.com.nicole.cofre.config.LimitadorDeTentativas.Regra.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final LimitadorDeTentativas limitador;

    public AuthController(AuthService service, LimitadorDeTentativas limitador) {
        this.service = service;
        this.limitador = limitador;
    }

    // getRemoteAddr ja e o IP do cliente mesmo atras do nginx: no perfil docker
    // o Tomcat le o X-Forwarded-For (server.forward-headers-strategy).

    @PostMapping("/cadastro")
    public ResponseEntity<TokenResponse> cadastrar(@RequestBody @Valid CadastroRequest req,
                                                   HttpServletRequest http) {
        limitador.consumir(CADASTRO_POR_IP, http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(req));
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest req, HttpServletRequest http) {
        limitador.consumir(LOGIN_POR_IP, http.getRemoteAddr());

        // Caixa baixa para "Ana@x.com" e "ana@x.com" dividirem o mesmo limite.
        // O bloqueio vale igual para e-mail que existe e que nao existe, entao
        // nao revela quais contas sao reais.
        String email = req.email().toLowerCase(Locale.ROOT);
        limitador.exigirDisponivel(LOGIN_FALHO_POR_EMAIL, email);

        try {
            return service.login(req);
        } catch (RegraNegocioException e) {
            // No login, RegraNegocioException so sai para credencial invalida.
            limitador.registrarFalha(LOGIN_FALHO_POR_EMAIL, email);
            throw e;
        }
    }
}

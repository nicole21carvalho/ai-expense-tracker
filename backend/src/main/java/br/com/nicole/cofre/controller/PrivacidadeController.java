package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.UsuarioAutenticado;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.service.PrivacidadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/privacidade")
public class PrivacidadeController {

    private final PrivacidadeService service;

    public PrivacidadeController(PrivacidadeService service) {
        this.service = service;
    }

    @GetMapping
    public PrivacidadeResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.consultar(usuario.id());
    }

    @PutMapping("/consentimento-ia")
    public PrivacidadeResponse definirConsentimentoIa(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestBody @Valid ConsentimentoIaRequest req) {
        return service.definirConsentimentoIa(usuario.id(), req.aceito());
    }
}

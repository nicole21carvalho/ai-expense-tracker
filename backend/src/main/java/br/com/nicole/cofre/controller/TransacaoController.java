package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.UsuarioAutenticado;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    // O id do dono vem do token, nunca da requisicao. Se viesse do corpo,
    // bastaria trocar o numero para ler o extrato de outra pessoa.
    @GetMapping
    public Page<TransacaoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(usuario.id(), inicio, fim, pageable);
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestBody @Valid TransacaoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(usuario.id(), req));
    }

    @PutMapping("/{id}")
    public TransacaoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @RequestBody @Valid TransacaoRequest req) {
        return service.atualizar(usuario.id(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        service.excluir(usuario.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resumo")
    public ResumoResponse resumo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.resumo(usuario.id(), inicio, fim);
    }
}

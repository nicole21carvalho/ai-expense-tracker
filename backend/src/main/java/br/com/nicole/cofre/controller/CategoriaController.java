package br.com.nicole.cofre.controller;

import br.com.nicole.cofre.config.UsuarioAutenticado;
import br.com.nicole.cofre.dto.Dtos.*;
import br.com.nicole.cofre.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.listar(usuario.id());
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestBody @Valid CategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(usuario.id(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        service.excluir(usuario.id(), id);
        return ResponseEntity.noContent().build();
    }
}

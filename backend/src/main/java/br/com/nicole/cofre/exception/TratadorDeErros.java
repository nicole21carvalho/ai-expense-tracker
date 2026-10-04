package br.com.nicole.cofre.exception;

import br.com.nicole.cofre.dto.Dtos.ErroResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centraliza o formato de erro da API. Sem isso cada controller inventa o
 * seu, e o front precisa tratar cinco formatos diferentes.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErroResponse("Dados inválidos", detalhes));
    }

    // JSON malformado ou com tipo errado (texto onde se espera numero). Sem
    // isto a resposta sai no formato padrao do Spring, diferente do resto.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse("Corpo da requisição inválido", List.of()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(e.getMessage(), List.of()));
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ResponseEntity<ErroResponse> muitasTentativas(MuitasTentativasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundosParaTentarDeNovo()))
                .body(new ErroResponse(e.getMessage(), List.of()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(e.getMessage(), List.of()));
    }
}

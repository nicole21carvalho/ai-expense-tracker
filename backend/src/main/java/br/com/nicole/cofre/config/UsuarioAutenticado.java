package br.com.nicole.cofre.config;

/**
 * O que o filtro coloca no contexto de seguranca e os controllers recebem.
 * Um record simples evita consultar o banco a cada requisicao so para saber
 * quem e o dono dos dados: o id ja veio assinado dentro do token.
 */
public record UsuarioAutenticado(Long id, String email) {
}

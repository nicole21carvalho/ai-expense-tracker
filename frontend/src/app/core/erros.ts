import { HttpErrorResponse } from '@angular/common/http';
import { ErroApi } from './modelos';

// Nome do campo no DTO do backend -> rótulo que o usuário vê na tela.
const ROTULOS: Record<string, string> = {
  nome: 'Nome',
  email: 'E-mail',
  senha: 'Senha',
  descricao: 'Descrição',
  valor: 'Valor',
  tipo: 'Tipo',
  data: 'Data'
};

/**
 * Texto de erro para mostrar ao usuário. Com erro de validação, a API manda
 * "Dados invalidos" em mensagem e o motivo real em detalhes ("valor: deve
 * ter..."); mostrar só a mensagem deixava o usuário sem saber o que corrigir.
 * Sem corpo (API fora do ar), cai no texto padrão de quem chamou.
 */
export function mensagemDeErro(e: HttpErrorResponse, padrao: string): string {
  const corpo = e.error as Partial<ErroApi> | null;

  if (corpo?.detalhes?.length) {
    return corpo.detalhes.map(rotular).join(' · ');
  }
  return corpo?.mensagem ?? padrao;
}

function rotular(detalhe: string): string {
  const separador = detalhe.indexOf(': ');
  if (separador < 0) return detalhe;
  const campo = detalhe.slice(0, separador);
  return `${ROTULOS[campo] ?? campo}: ${detalhe.slice(separador + 2)}`;
}

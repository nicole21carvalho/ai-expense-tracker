export type TipoTransacao = 'RECEITA' | 'DESPESA';

export interface Transacao {
  id: number;
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  data: string;
  categoriaId: number | null;
  categoriaNome: string | null;
  categoriaPorIa: boolean;
}

export interface NovaTransacao {
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  data: string;
  categoriaId: number | null;
}

export interface Categoria {
  id: number;
  nome: string;
  cor: string;
}

export interface FatiaResumo {
  categoria: string;
  total: number;
}

export interface Resumo {
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  despesasPorCategoria: FatiaResumo[];
}

export interface Sessao {
  token: string;
  nome: string;
  email: string;
}

// O backend devolve Page do Spring Data no formato estável (via-dto): os
// itens em content e a paginação separada em page. Tipar isso evita acessar
// res.content como any e descobrir o erro só em runtime.
export interface Pagina<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

// categorizacaoPorIa: a API está configurada com IA. Quando false, nada sai
// da aplicação e não há o que consentir.
export interface Privacidade {
  categorizacaoPorIa: boolean;
  provedorIa: string;
  consentimentoIa: boolean;
  consentimentoIaEm: string | null;
}

// Formato único de erro da API (ErroResponse no backend).
export interface ErroApi {
  mensagem: string;
  detalhes: string[];
}

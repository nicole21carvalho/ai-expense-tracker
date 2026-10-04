import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria, NovaTransacao, Pagina, Privacidade, Resumo, Transacao } from './modelos';

@Injectable({ providedIn: 'root' })
export class FinanceiroService {

  constructor(private http: HttpClient) {}

  listar(inicio: string, fim: string, pagina = 0): Observable<Pagina<Transacao>> {
    const params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim)
      .set('page', pagina)
      .set('size', 50);
    return this.http.get<Pagina<Transacao>>('/api/transacoes', { params });
  }

  resumo(inicio: string, fim: string): Observable<Resumo> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<Resumo>('/api/transacoes/resumo', { params });
  }

  criar(dados: NovaTransacao): Observable<Transacao> {
    return this.http.post<Transacao>('/api/transacoes', dados);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/transacoes/${id}`);
  }

  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>('/api/categorias');
  }

  privacidade(): Observable<Privacidade> {
    return this.http.get<Privacidade>('/api/privacidade');
  }

  definirConsentimentoIa(aceito: boolean): Observable<Privacidade> {
    return this.http.put<Privacidade>('/api/privacidade/consentimento-ia', { aceito });
  }
}

import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Sessao } from './modelos';

const CHAVE = 'cofre.sessao';

@Injectable({ providedIn: 'root' })
export class SessaoService {

  // Signal e nao BehaviorSubject: o template le direto com sessao() e o
  // Angular so recalcula o que depende dele. Menos cerimonia de subscribe.
  private readonly estado = signal<Sessao | null>(this.recuperar());

  readonly sessao = this.estado.asReadonly();
  readonly autenticado = computed(() => this.estado() !== null);
  readonly nome = computed(() => this.estado()?.nome ?? '');

  constructor(private http: HttpClient) {}

  entrar(email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>('/api/auth/login', { email, senha })
      .pipe(tap(s => this.guardar(s)));
  }

  cadastrar(nome: string, email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>('/api/auth/cadastro', { nome, email, senha })
      .pipe(tap(s => this.guardar(s)));
  }

  sair(): void {
    localStorage.removeItem(CHAVE);
    this.estado.set(null);
  }

  get token(): string | null {
    return this.estado()?.token ?? null;
  }

  private guardar(s: Sessao): void {
    // localStorage e vulneravel a XSS: um script injetado le o token.
    // A alternativa seria cookie httpOnly, que o JavaScript nao enxerga,
    // mas exige o backend setando o cookie e tratamento de CSRF. Para um
    // projeto de portfolio localStorage e aceitavel; em producao bancaria
    // nao seria, e vale dizer isso na entrevista em vez de fingir que e ideal.
    localStorage.setItem(CHAVE, JSON.stringify(s));
    this.estado.set(s);
  }

  private recuperar(): Sessao | null {
    try {
      const bruto = localStorage.getItem(CHAVE);
      return bruto ? JSON.parse(bruto) as Sessao : null;
    } catch {
      return null;
    }
  }
}

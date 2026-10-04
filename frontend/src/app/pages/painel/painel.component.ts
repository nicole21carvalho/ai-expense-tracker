import { Component, OnInit, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { FinanceiroService } from '../../core/financeiro.service';
import { SessaoService } from '../../core/sessao.service';
import { Categoria, Privacidade, Resumo, TipoTransacao, Transacao } from '../../core/modelos';
import { mesAtual, rotuloDoMes } from '../../core/periodo';
import { mensagemDeErro } from '../../core/erros';

/** Cor por posição, seguindo a ordem das cédulas. */
const CORES_NOTA = ['#2E6DA4', '#7B5EA7', '#C44536', '#D99014', '#8C6239', '#2F7A4F'];

@Component({
  selector: 'app-painel',
  imports: [CommonModule, FormsModule],
  template: `
    <header class="topo">
      <div class="faixa">
        <span class="logo">Cofre</span>
        <span class="mes">{{ rotulo }}</span>
        <button class="sair" (click)="sair()">Sair</button>
      </div>

      <!-- O saldo é o herói da tela: tudo em volta fica quieto. -->
      <div class="saldo">
        <p class="legenda">Saldo do mês</p>
        <p class="valor numero" [class.negativo]="(resumo()?.saldo ?? 0) < 0">
          <span class="cifra">R$</span>{{ parteInteira(resumo()?.saldo ?? 0) }}<span
            class="centavos">,{{ parteCentavos(resumo()?.saldo ?? 0) }}</span>
        </p>
        <p class="fluxo">
          <span class="entrou numero">+{{ resumo()?.totalReceitas ?? 0 | number:'1.2-2' }}</span>
          <span class="saiu numero">−{{ resumo()?.totalDespesas ?? 0 | number:'1.2-2' }}</span>
        </p>
      </div>
    </header>

    <main class="corpo">
      <section class="lancar">
        <h2>Novo lançamento</h2>
        <div class="linha-form">
          <input placeholder="Descrição" [(ngModel)]="descricao"
                 (keyup.enter)="lancar()" aria-label="Descrição">
          <input placeholder="0,00" type="number" step="0.01" min="0.01"
                 [(ngModel)]="valor" aria-label="Valor" class="numero campo-valor">
          <select [(ngModel)]="tipo" aria-label="Tipo">
            <option value="DESPESA">Saída</option>
            <option value="RECEITA">Entrada</option>
          </select>
          <select [(ngModel)]="categoriaId" aria-label="Categoria">
            <option [ngValue]="null">{{ iaAtiva() ? 'Deixar a IA escolher' : 'Sugerir automaticamente' }}</option>
            @for (c of categorias(); track c.id) {
              <option [ngValue]="c.id">{{ c.nome }}</option>
            }
          </select>
          <button class="btn-principal" (click)="lancar()" [disabled]="salvando()">
            {{ salvando() ? 'Salvando' : 'Lançar' }}
          </button>
        </div>
        @if (erro()) { <p class="erro">{{ erro() }}</p> }

        <!-- LGPD: a descrição só vai para a IA com permissão explícita, e
             desligar fica a um clique, tão fácil quanto ligar. -->
        @if (privacidade(); as p) {
          @if (p.categorizacaoPorIa) {
            <div class="consentimento">
              @if (p.consentimentoIa) {
                <p>
                  <strong>Sugestão por IA ativa.</strong> A descrição dos lançamentos sem
                  categoria e os nomes das suas categorias são enviados à {{ p.provedorIa }}.
                </p>
                <button class="acao-consentimento" (click)="definirConsentimentoIa(false)">Desativar</button>
              } @else {
                <p>
                  Quer sugestões de categoria mais precisas? Com sua permissão, a descrição
                  dos lançamentos sem categoria e os nomes das suas categorias são enviados
                  à {{ p.provedorIa }}. Valores, datas e dados da sua conta não são enviados.
                  Você pode desativar quando quiser.
                </p>
                <button class="acao-consentimento" (click)="definirConsentimentoIa(true)">Permitir</button>
              }
            </div>
          }
        }
      </section>

      <section class="extrato">
        <h2>Extrato</h2>

        @if (carregando()) {
          <p class="vazio">Carregando…</p>
        } @else if (transacoes().length === 0) {
          <p class="vazio">Nenhum lançamento neste mês. Comece pelo campo acima.</p>
        } @else {
          <table>
            <caption class="oculto">Lançamentos do mês</caption>
            <tbody>
              @for (t of transacoes(); track t.id) {
                <tr>
                  <td class="data numero">{{ t.data | date:'dd/MM' }}</td>
                  <td class="descricao">
                    {{ t.descricao }}
                    @if (t.categoriaNome) {
                      <span class="tag" [style.--cor]="corDe(t.categoriaNome)">
                        {{ t.categoriaNome }}
                      </span>
                    }
                    @if (t.categoriaPorIa) {
                      <span class="sugerido" title="Categoria sugerida automaticamente">sugerido</span>
                    }
                  </td>
                  <td class="valor-linha numero"
                      [class.entrada]="t.tipo === 'RECEITA'"
                      [class.saida]="t.tipo === 'DESPESA'">
                    {{ t.tipo === 'RECEITA' ? '+' : '−' }}{{ t.valor | number:'1.2-2' }}
                  </td>
                  <td class="acao">
                    <button class="excluir" (click)="excluir(t)"
                            [attr.aria-label]="'Excluir ' + t.descricao">×</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>

      @if ((resumo()?.despesasPorCategoria?.length ?? 0) > 0) {
        <section class="quebra">
          <h2>Onde foi o dinheiro</h2>
          @for (f of resumo()!.despesasPorCategoria; track f.categoria; let i = $index) {
            <div class="fatia">
              <span class="nome">{{ f.categoria }}</span>
              <span class="barra">
                <span class="preenche"
                      [style.width.%]="percentual(f.total)"
                      [style.background]="CORES[i % CORES.length]"></span>
              </span>
              <span class="total numero">{{ f.total | number:'1.2-2' }}</span>
            </div>
          }
        </section>
      }
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .topo { background: var(--tinta); color: #fff; padding: 1rem var(--gutter) 2.25rem; }
    .faixa {
      display: flex; align-items: center; gap: 1rem;
      max-width: 54rem; margin: 0 auto 1.75rem;
    }
    .logo { font-weight: 600; font-size: 1.05rem; }
    .mes { color: #AFC4D8; font-size: 0.9rem; flex: 1; }
    .sair {
      background: none; border: 1px solid rgba(255,255,255,.3);
      color: #fff; padding: 0.35rem 0.75rem; font-size: 0.85rem;
    }
    .sair:hover { border-color: #fff; }

    .saldo { max-width: 54rem; margin: 0 auto; }
    .legenda { color: #AFC4D8; font-size: 0.85rem; margin: 0 0 0.25rem; }
    .valor {
      font-size: clamp(2.5rem, 9vw, 4rem);
      font-weight: 600; line-height: 1; margin: 0;
      letter-spacing: -0.03em;
    }
    .valor.negativo { color: #F0A7A0; }
    .cifra { font-size: 0.42em; font-weight: 500; margin-right: 0.3rem; vertical-align: 0.42em; }
    .centavos { font-size: 0.5em; }
    .fluxo { display: flex; gap: 1.25rem; margin: 0.75rem 0 0; font-size: 0.95rem; }
    .entrou { color: #8FD6AC; }
    .saiu { color: #F0A7A0; }

    .corpo { max-width: 54rem; margin: 0 auto; padding: 0 var(--gutter) 4rem; }
    section { margin-top: 2rem; }
    h2 { font-size: 0.95rem; color: var(--sutil); font-weight: 500; margin-bottom: 0.75rem; }

    .linha-form { display: grid; gap: 0.5rem; grid-template-columns: 1fr; }
    @media (min-width: 46rem) {
      .linha-form { grid-template-columns: 2.2fr 1fr 1fr 1.3fr auto; align-items: center; }
    }
    .campo-valor { text-align: right; }

    .consentimento {
      display: flex; gap: 1rem; align-items: flex-start;
      margin-top: 0.9rem; padding: 0.75rem 0.9rem;
      border: 1px solid var(--borda); border-radius: var(--raio);
      font-size: 0.85rem; color: var(--sutil);
    }
    .consentimento p { margin: 0; flex: 1; }
    .consentimento strong { color: var(--texto); font-weight: 500; }
    .acao-consentimento {
      background: none; border: 1px solid var(--borda); color: var(--nota-2);
      padding: 0.35rem 0.75rem; font-size: 0.85rem; white-space: nowrap;
    }
    .acao-consentimento:hover { border-color: var(--nota-2); }

    table { width: 100%; border-collapse: collapse; }
    .oculto {
      position: absolute; width: 1px; height: 1px;
      overflow: hidden; clip: rect(0 0 0 0);
    }
    tr { border-bottom: 1px solid var(--borda); }
    tr:hover .excluir { opacity: 1; }
    td { padding: 0.7rem 0.4rem; vertical-align: baseline; }
    .data { color: var(--sutil); font-size: 0.85rem; width: 3.5rem; }
    .descricao { width: 100%; }
    .valor-linha { text-align: right; white-space: nowrap; font-weight: 500; }
    .entrada { color: var(--entrada); }
    .saida { color: var(--saida); }
    .acao { width: 2rem; text-align: right; }
    .excluir {
      background: none; border: none; color: var(--sutil);
      font-size: 1.1rem; line-height: 1; padding: 0 0.3rem; opacity: 0;
    }
    .excluir:hover, .excluir:focus-visible { color: var(--saida); opacity: 1; }

    .tag {
      display: inline-block; margin-left: 0.5rem;
      font-size: 0.75rem; padding: 0.1rem 0.45rem;
      border-radius: 2px; color: var(--cor);
      border: 1px solid var(--cor);
    }
    .sugerido {
      margin-left: 0.35rem; font-size: 0.7rem;
      color: var(--sutil); font-style: italic;
    }

    .fatia {
      display: grid; grid-template-columns: 8rem 1fr 5.5rem;
      gap: 0.75rem; align-items: center; padding: 0.3rem 0;
    }
    .nome { font-size: 0.9rem; }
    .barra { background: var(--borda); height: 0.55rem; border-radius: 2px; overflow: hidden; }
    .preenche { display: block; height: 100%; }
    .total { text-align: right; font-size: 0.9rem; }

    .vazio { color: var(--sutil); padding: 1.5rem 0; }
  `]
})
export class PainelComponent implements OnInit {

  private api = inject(FinanceiroService);
  private sessaoService = inject(SessaoService);
  private router = inject(Router);

  readonly CORES = CORES_NOTA;
  readonly rotulo = rotuloDoMes();
  private readonly periodo = mesAtual();

  transacoes = signal<Transacao[]>([]);
  categorias = signal<Categoria[]>([]);
  resumo = signal<Resumo | null>(null);
  privacidade = signal<Privacidade | null>(null);

  // A IA só está em uso quando está configurada e o usuário permitiu; fora
  // disso a sugestão vem das regras locais e o rótulo não promete IA.
  iaAtiva = computed(() => !!this.privacidade()?.categorizacaoPorIa && !!this.privacidade()?.consentimentoIa);
  carregando = signal(true);
  salvando = signal(false);
  erro = signal('');

  descricao = '';
  valor: number | null = null;
  tipo: TipoTransacao = 'DESPESA';
  categoriaId: number | null = null;

  private maiorDespesa = computed(() =>
    Math.max(...(this.resumo()?.despesasPorCategoria.map(f => f.total) ?? [1]), 1));

  ngOnInit(): void {
    // Todo subscribe trata erro: sem isso, uma falha deixava a tela pela
    // metade sem aviso (e o Angular registrava ERROR no console). No 401 o
    // interceptor já desloga; a mensagem aqui é para as demais falhas.
    this.api.categorias().subscribe({
      next: c => this.categorias.set(c),
      error: () => this.erro.set('Não foi possível carregar as categorias.')
    });
    this.api.privacidade().subscribe({
      next: p => this.privacidade.set(p),
      // Sem resposta, o aviso de consentimento só não aparece. Nada vai para
      // a IA sem consentimento gravado no servidor, então é seguro seguir.
      error: () => this.privacidade.set(null)
    });
    this.recarregar();
  }

  recarregar(): void {
    this.carregando.set(true);

    this.api.listar(this.periodo.inicio, this.periodo.fim).subscribe({
      next: p => { this.transacoes.set(p.content); this.carregando.set(false); },
      error: () => { this.carregando.set(false); this.erro.set('Não foi possível carregar o extrato.'); }
    });

    this.api.resumo(this.periodo.inicio, this.periodo.fim)
      .subscribe({
        next: r => this.resumo.set(r),
        error: () => this.erro.set('Não foi possível carregar o resumo do mês.')
      });
  }

  lancar(): void {
    this.erro.set('');

    if (!this.descricao.trim() || !this.valor || this.valor <= 0) {
      this.erro.set('Informe descrição e um valor maior que zero.');
      return;
    }

    this.salvando.set(true);

    this.api.criar({
      descricao: this.descricao.trim(),
      valor: this.valor,
      tipo: this.tipo,
      data: this.hoje(),
      categoriaId: this.categoriaId
    }).subscribe({
      next: () => {
        this.descricao = '';
        this.valor = null;
        this.categoriaId = null;
        this.salvando.set(false);
        this.recarregar();
      },
      error: e => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível salvar o lançamento.'));
      }
    });
  }

  excluir(t: Transacao): void {
    if (!confirm(`Excluir "${t.descricao}"?`)) return;
    this.api.excluir(t.id).subscribe({
      next: () => this.recarregar(),
      error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível excluir o lançamento.'))
    });
  }

  definirConsentimentoIa(aceito: boolean): void {
    this.api.definirConsentimentoIa(aceito).subscribe({
      next: p => this.privacidade.set(p),
      error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível salvar sua escolha.'))
    });
  }

  sair(): void {
    this.sessaoService.sair();
    this.router.navigate(['/entrar']);
  }

  percentual(total: number): number {
    // Proporcional à maior despesa, não ao total gasto: com isso a maior
    // barra sempre enche a linha e as diferenças ficam legíveis.
    return (total / this.maiorDespesa()) * 100;
  }

  corDe(nome: string): string {
    // Hash estável do nome: a mesma categoria mantém a cor entre sessões.
    let soma = 0;
    for (const ch of nome) soma += ch.charCodeAt(0);
    return CORES_NOTA[soma % CORES_NOTA.length];
  }

  parteInteira(v: number): string {
    return Math.floor(Math.abs(v)).toLocaleString('pt-BR');
  }

  parteCentavos(v: number): string {
    return Math.round(Math.abs(v) * 100 % 100).toString().padStart(2, '0');
  }

  private hoje(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}

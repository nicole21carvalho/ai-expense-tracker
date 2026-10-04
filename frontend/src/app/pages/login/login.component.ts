import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SessaoService } from '../../core/sessao.service';
import { mensagemDeErro } from '../../core/erros';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <main class="moldura">
      <section class="cartao">
        <p class="marca numero">R$</p>
        <h1>{{ modoCadastro() ? 'Criar conta no Cofre' : 'Entrar no Cofre' }}</h1>
        <p class="apoio">
          {{ modoCadastro()
              ? 'Suas categorias já vêm prontas. Dá para mudar depois.'
              : 'Controle de gastos com categorização automática.' }}
        </p>

        @if (modoCadastro()) {
          <label for="nome">Nome</label>
          <input id="nome" name="nome" [(ngModel)]="nome" autocomplete="name">
        }

        <label for="email">E-mail</label>
        <input id="email" name="email" type="email" [(ngModel)]="email" autocomplete="email">

        <label for="senha">Senha</label>
        <input id="senha" name="senha" type="password" [(ngModel)]="senha"
               [autocomplete]="modoCadastro() ? 'new-password' : 'current-password'">
        @if (modoCadastro()) {
          <p class="dica">Mínimo de 8 caracteres.</p>
        }

        @if (erro()) {
          <p class="erro">{{ erro() }}</p>
        }

        <button class="btn-principal largo" (click)="enviar()" [disabled]="carregando()">
          {{ carregando() ? 'Aguarde' : (modoCadastro() ? 'Criar conta' : 'Entrar') }}
        </button>

        <button class="alternar" (click)="alternar()">
          {{ modoCadastro() ? 'Já tenho conta' : 'Criar uma conta' }}
        </button>
      </section>
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .moldura {
      min-height: 100dvh;
      display: grid;
      place-items: center;
      padding: var(--gutter);
      background: var(--fundo);
    }
    .cartao {
      width: 100%;
      max-width: 23rem;
      background: var(--papel);
      border: 1px solid var(--borda);
      border-radius: var(--raio);
      padding: 2rem 1.75rem;
    }
    /* Único momento de cor forte na tela: o símbolo da moeda. */
    .marca {
      font-size: 2.5rem;
      font-weight: 600;
      color: var(--nota-100);
      margin: 0 0 0.75rem;
      line-height: 1;
    }
    h1 { font-size: 1.35rem; margin-bottom: 0.4rem; }
    .apoio { color: var(--sutil); font-size: 0.92rem; margin: 0 0 1.5rem; }
    label {
      display: block;
      font-size: 0.85rem;
      font-weight: 500;
      margin: 0.9rem 0 0.3rem;
    }
    .dica { color: var(--sutil); font-size: 0.8rem; margin: 0.35rem 0 0; }
    .largo { width: 100%; margin-top: 1.5rem; }
    .alternar {
      width: 100%;
      margin-top: 0.6rem;
      background: none;
      border: none;
      color: var(--nota-2);
      padding: 0.5rem;
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  `]
})
export class LoginComponent {

  private sessao = inject(SessaoService);
  private router = inject(Router);

  nome = '';
  email = '';
  senha = '';

  modoCadastro = signal(false);
  carregando = signal(false);
  erro = signal('');

  alternar(): void {
    this.modoCadastro.update(v => !v);
    this.erro.set('');
  }

  enviar(): void {
    this.erro.set('');

    if (!this.email || !this.senha || (this.modoCadastro() && !this.nome)) {
      this.erro.set('Preencha todos os campos.');
      return;
    }

    this.carregando.set(true);

    const requisicao = this.modoCadastro()
      ? this.sessao.cadastrar(this.nome, this.email, this.senha)
      : this.sessao.entrar(this.email, this.senha);

    requisicao.subscribe({
      next: () => this.router.navigate(['/painel']),
      error: (e) => {
        this.carregando.set(false);
        // Se o servidor estiver fora, não há corpo nenhum — daí a mensagem de rede.
        this.erro.set(mensagemDeErro(e, 'Não foi possível conectar à API.'));
      }
    });
  }
}

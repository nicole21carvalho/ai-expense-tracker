import { Routes } from '@angular/router';
import { guardaDeSessao } from './core/sessao.guard';

export const rotas: Routes = [
  {
    path: 'entrar',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'painel',
    // Lazy loading: o bundle do painel só desce depois do login. Quem nunca
    // entra não baixa a tela inteira.
    loadComponent: () => import('./pages/painel/painel.component').then(m => m.PainelComponent),
    canActivate: [guardaDeSessao]
  },
  { path: '', redirectTo: 'painel', pathMatch: 'full' },
  { path: '**', redirectTo: 'painel' }
];

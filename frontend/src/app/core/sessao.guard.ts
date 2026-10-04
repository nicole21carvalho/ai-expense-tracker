import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessaoService } from './sessao.service';

export const guardaDeSessao: CanActivateFn = () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  // A guarda melhora a navegacao, nao e seguranca. Quem editar o
  // localStorage passa por ela — quem protege de verdade e o backend,
  // que valida a assinatura do token em toda requisicao.
  return sessao.autenticado() ? true : router.createUrlTree(['/entrar']);
};

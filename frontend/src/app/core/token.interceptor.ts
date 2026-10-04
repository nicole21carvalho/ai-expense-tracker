import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessaoService } from './sessao.service';

/**
 * Interceptor funcional (Angular 15+). Anexa o token em toda chamada e
 * derruba a sessao no 401, em vez de cada componente tratar isso sozinho.
 */
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const token = sessao.token;

  // O endpoint de login nao tem token ainda, e mandar header vazio quebra.
  const requisicao = token && !req.url.includes('/api/auth/')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401) {
        sessao.sair();
        router.navigate(['/entrar']);
      }
      return throwError(() => erro);
    })
  );
};

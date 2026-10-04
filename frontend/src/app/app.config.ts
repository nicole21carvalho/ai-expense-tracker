import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';

import { rotas } from './app.routes';
import { tokenInterceptor } from './core/token.interceptor';

// Sem isto o pipe de moeda formata 1,234.50 em vez de 1.234,50.
registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    // Desde o Angular 21 o padrão é sem zone.js; o app segue com zone.js e
    // XHR, como era no 18, para a migração não mudar comportamento junto.
    provideZoneChangeDetection(),
    provideRouter(rotas),
    provideHttpClient(withXhr(), withInterceptors([tokenInterceptor])),
    { provide: LOCALE_ID, useValue: 'pt-BR' }
  ]
};

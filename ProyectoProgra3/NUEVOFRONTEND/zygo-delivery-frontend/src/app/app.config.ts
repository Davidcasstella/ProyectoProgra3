// src/app/app.config.ts

import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { jwtInterceptor } from './interceptors/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    // ⚡ Precargar módulos en segundo plano después de la carga inicial
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(
      withFetch(),
      withInterceptors([jwtInterceptor])
    ),
    // 🎨 Soporte para animaciones (carga asíncrona - más eficiente)
    provideAnimationsAsync()
  ]
};
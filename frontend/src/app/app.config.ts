import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { routes } from './app.routes';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';

export const appConfig: ApplicationConfig = {
  providers: [
    // makes change detection run reliably after async tasks
    provideZoneChangeDetection({ eventCoalescing: false, runCoalescing: false }),

    provideRouter(routes),
    provideHttpClient()
  ]
};

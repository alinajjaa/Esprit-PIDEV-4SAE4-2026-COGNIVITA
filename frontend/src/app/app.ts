import { Component } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { Brain3dComponent } from './brain3d/brain3d.component';
import { NavigationComponent } from './navigation/navigation.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Brain3dComponent, NavigationComponent, CommonModule],
  template: `
    <div class="app-layout">
      <div class="brain-background-container" *ngIf="!isAuthPage()">
        <app-brain3d></app-brain3d>
      </div>
      <app-navigation></app-navigation>
      <div class="content-overlay" [class.auth-page]="isAuthPage()">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .app-layout {
      position: relative;
      width: 100%;
      min-height: 100vh;
    }

    .brain-background-container {
      position: fixed;
      top: 0; left: 0;
      width: 100%;
      height: 100vh;
      z-index: 0;
    }

    .content-overlay {
      position: relative;
      z-index: 1;
      margin-top: 70px;
    }

    /* ✅ Pages auth — pas de margin, plein écran */
    .content-overlay.auth-page {
      margin-top: 0 !important;
      padding: 0 !important;
    }
  `]
})
export class AppComponent {
  constructor(private router: Router) {}

  isAuthPage(): boolean {
    return this.router.url.includes('login') ||
      this.router.url.includes('register') ||
      this.router.url.includes('forgot-password') ||
      this.router.url.includes('reset-password');
  }
}
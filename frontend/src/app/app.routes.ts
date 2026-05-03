import { Routes } from '@angular/router';
import { authGuard, adminGuard, publicGuard } from '../app/guards/auth.guard';

export const routes: Routes = [

  // ✅ Redirect par défaut → login
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  // ── Pages publiques ────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/login/login')
        .then(m => m.LoginComponent),
    canActivate: [publicGuard]
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/register/register')
        .then(m => m.RegisterComponent),
    canActivate: [publicGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/forgot-password/forgot-password')
        .then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/reset-password/reset-password')
        .then(m => m.ResetPasswordComponent)
  },
  {
    path: 'oauth2/callback',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/oauth2-callback/oauth2-callback')
        .then(m => m.Oauth2Callback)
  },

  // ── Pages protégées ────────────────────────────
  {
    path: 'home',
    loadComponent: () =>
      import('./home/home.component')
        .then(m => m.HomeComponent),
    canActivate: [authGuard]
  },
  {
    path: 'mmse',
    loadComponent: () =>
      import('./mmse/mmse-test.component')
        .then(m => m.MMSETestComponent),
    canActivate: [authGuard]
  },
  {
    path: 'cnn',
    loadComponent: () =>
      import('./cnn/cnn-prediction.component')
        .then(m => m.CNNPredictionComponent),
    canActivate: [authGuard]
  },
  {
    path: 'medical-records',
    loadComponent: () =>
      import('./medical-records/medical-records.component')
        .then(m => m.MedicalRecordsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/user-profile/user-profile')
        .then(m => m.UserProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'face-capture',
    loadComponent: () =>
      import('./FrontOffice/UserManagement/face-capture/face-capture')
        .then(m => m.FaceCaptureComponent),
    canActivate: [authGuard]
  },

  // ── Pages ADMIN ────────────────────────────────
  {
    path: 'admin',
    loadComponent: () =>
      import('./admin/admin-dashboard/admin-dashboard.component')
        .then(m => m.AdminDashboardComponent),
    canActivate: [adminGuard]
  },

  // ── Fallback ───────────────────────────────────
  {
    path: '**',
    redirectTo: 'login'
  }

];
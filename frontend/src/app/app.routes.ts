import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { MMSETestComponent } from './mmse/mmse-test.component';
import { CNNPredictionComponent } from './cnn/cnn-prediction.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';
import { MedicalRecordsComponent } from './medical-records/medical-records.component';
import { LoginComponent } from './FrontOffice/UserManagement/login/login';
import { RegisterComponent } from './FrontOffice/UserManagement/register/register';
import { UserProfileComponent } from './FrontOffice/UserManagement/user-profile/user-profile';
import { authGuard, adminGuard, publicGuard } from '../app/guards/auth.guard';
import { ResetPasswordComponent } from './FrontOffice/UserManagement/reset-password/reset-password';
import { ForgotPasswordComponent } from './FrontOffice/UserManagement/forgot-password/forgot-password';
import { Oauth2Callback } from './FrontOffice/UserManagement/oauth2-callback/oauth2-callback';
import { FaceCaptureComponent } from './FrontOffice/UserManagement/face-capture/face-capture';  
export const routes: Routes = [

  // ✅ Redirect par défaut → login
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  // ── Pages publiques (login/register) ──────────
  // Si déjà connecté → redirige vers home
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [publicGuard]
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [publicGuard]
  },

  // ── Pages protégées (connecté requis) ─────────
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [authGuard]
  },
  {
    path: 'mmse',
    component: MMSETestComponent,
    canActivate: [authGuard]
  },
  {
    path: 'cnn',
    component: CNNPredictionComponent,
    canActivate: [authGuard]
  },
  {
    path: 'medical-records',
    component: MedicalRecordsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    component: UserProfileComponent,
    canActivate: [authGuard]
  },

  // ── Pages ADMIN uniquement ─────────────────────
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [adminGuard]
  },
    { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password',  component: ResetPasswordComponent  },
  { path: 'oauth2/callback', component: Oauth2Callback },
  { path: 'face-capture',    component: FaceCaptureComponent    },

  // ── Fallback ───────────────────────────────────
  {
    path: '**',
    redirectTo: 'login'
  }

];
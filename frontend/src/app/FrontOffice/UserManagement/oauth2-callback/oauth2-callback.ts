import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  imports: [CommonModule , RouterModule  ],
  templateUrl: './oauth2-callback.html',
  styleUrl: './oauth2-callback.css',
})
export class Oauth2Callback implements OnInit {

  error = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) {}

ngOnInit(): void {
  const token = this.route.snapshot.queryParamMap.get('token');
  const role  = this.route.snapshot.queryParamMap.get('role');
  const error = this.route.snapshot.queryParamMap.get('error');

  // ── Erreur OAuth ───────────────────────────────
  if (error) {
    this.error = 'Authentication failed: ' + error;
    setTimeout(() => this.router.navigate(['/login']), 2000);
    return;
  }

  // ── Pas de token ───────────────────────────────
  if (!token) {
    this.error = 'Authentication failed. Redirecting...';
    setTimeout(() => this.router.navigate(['/login']), 2000);
    return;
  }

  // ✅ Stocker le token d'abord
  localStorage.setItem('jwt_token', token);

  // ✅ Charger les infos de l'utilisateur
  this.userService.loadCurrentUser().subscribe({
    next: (user: any) => {
      const userRole = user.role || role || 'USER';
      if (userRole === 'ADMIN') {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate(['/home']);
      }
    },
    error: (err: any) => {
      console.error('Erreur chargement user après OAuth2:', err);
      // Fallback si /me échoue
      if (role === 'ADMIN') {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate(['/home']);
      }
    }
  });
}
}
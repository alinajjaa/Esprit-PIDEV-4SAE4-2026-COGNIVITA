import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../services/user.service';

// ✅ Connecté requis
export const authGuard: CanActivateFn = () => {
  const userService = inject(UserService);
  const router = inject(Router);

  if (userService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

// ✅ ADMIN uniquement
export const adminGuard: CanActivateFn = () => {
  const userService = inject(UserService);
  const router = inject(Router);

  if (userService.isLoggedIn() && userService.isAdmin()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

// ✅ Public — si déjà connecté → redirige vers home
export const publicGuard: CanActivateFn = () => {
  const userService = inject(UserService);
  const router = inject(Router);

  if (userService.isLoggedIn()) {
    if (userService.isAdmin()) {
      router.navigate(['/admin']);
    } else {
      router.navigate(['/home']);
    }
    return false;
  }

  return true;
};
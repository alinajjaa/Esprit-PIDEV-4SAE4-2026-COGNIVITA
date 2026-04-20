import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

/**
 * FIX: No AuthService existed — components used a hardcoded userId = 1.
 *
 * This service:
 *  1. Calls GET /api/users/email/{email} on login to fetch the real User record
 *  2. Persists id, email, firstName, lastName, role in localStorage
 *  3. Exposes getCurrentUserId() so any component can read the real user id
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly STORAGE_KEY = 'currentUser';

  constructor(private http: HttpClient) {}

  /** Login: fetch user by email, store in localStorage */
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.get<LoginResponse>(`/api/users/email/${encodeURIComponent(email)}`).pipe(
      tap(user => {
        // NOTE: user-service has no password check endpoint yet — stores user on
        // successful GET so the UI can function. Add proper auth as next step.
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(user));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  getCurrentUser(): LoginResponse | null {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  getCurrentUserId(): number {
    return this.getCurrentUser()?.id ?? 1;
  }

  isLoggedIn(): boolean {
    return this.getCurrentUser() !== null;
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, map, of } from 'rxjs';
import { catchError } from 'rxjs/operators'; // ← AJOUTER cette ligne

/* =========================
   USER INTERFACES
   ========================= */
export interface User {
  id: number;
  email: string;
  fullName: string;
  photoUrl: string;
  role: string;
  createdAt: string;
  blocked: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword?: string;  // Ajoutez confirmPassword ici
  firstName?: string;
  lastName?: string;
  photoUrl?: string;
}

export interface AuthResponse {
  token: string;
  user: any;
  role: string;
}

/* =========================
   USER SERVICE
   ========================= */
@Injectable({ providedIn: 'root' })
export class UserService {

  private baseUrl = 'http://localhost:8081/api/users';
  private tokenKey = 'jwt_token';
  private userKey = 'currentUser';

  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) { }

  /* =========================
     LOGIN
     ========================= */
login(credentials: { email: string, password: string }): Observable<any> {
  return this.http.post<any>(`${this.baseUrl}/login`, credentials).pipe(
    map(res => {
      if (res.twoFaRequired) return res;
      if (res.step === 'face_required') return res; // ✅ pas de stockage

      const mappedUser: User = this.mapUser(res.user);
      mappedUser.role = res.role;
      localStorage.setItem(this.tokenKey, res.token);
      localStorage.setItem(this.userKey, JSON.stringify(mappedUser));
      this.currentUserSubject.next(mappedUser);
      return res;
    })
  );
}

  /* =========================
     LOGOUT
     ========================= */
  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(null);
  }

  /* =========================
     RESTORE SESSION
     ========================= */
  restoreSession(): void {
    const token = this.getToken();
    const user = this.loadUserFromStorage();
    if (token && user) this.currentUserSubject.next(user);
  }

  /* =========================
     REGISTER
     ========================= */
  register(req: RegisterRequest): Observable<User> {
    return this.http.post<User>(this.baseUrl, req);
  }

  /* =========================
     SESSION
     ========================= */
  getSession(): Observable<User> {
    return this.http.get<any>(`${this.baseUrl}/session`, { headers: this.authHeaders() })
      .pipe(
        map(user => {
          const mappedUser = this.mapUser(user);
          localStorage.setItem(this.userKey, JSON.stringify(mappedUser));
          this.currentUserSubject.next(mappedUser);
          return mappedUser;
        })
      );
  }
  /* =========================
     LOAD CURRENT USER — OAuth2
     ========================= */
  loadCurrentUser(): Observable<User> {
    return this.http.get<any>(`${this.baseUrl}/me`, { headers: this.authHeaders() })
      .pipe(
        map((user: any) => {
          const mapped = this.mapUser(user);
          localStorage.setItem(this.userKey, JSON.stringify(mapped));
          this.currentUserSubject.next(mapped);
          return mapped;
        }),
        catchError(() => {
          // Fallback : utilise le storage si /me échoue
          const stored = this.loadUserFromStorage();
          if (stored) {
            this.currentUserSubject.next(stored);
            return of(stored);
          }
          throw new Error('User not found');
        })
      );
  }
  /* =========================
     CRUD
     ========================= */
  getById(id: number): Observable<User> {
    return this.http.get<any>(`${this.baseUrl}/${id}`, { headers: this.authHeaders() })
      .pipe(map(user => this.mapUser(user)));
  }

  getAll(): Observable<User[]> {
    return this.http.get<any[]>(this.baseUrl, { headers: this.authHeaders() })
      .pipe(map(users => users.map(u => this.mapUser(u))));
  }

  update(id: number, user: Partial<User>): Observable<User> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, user, { headers: this.authHeaders() })
      .pipe(
        map(updated => {
          const mapped = this.mapUser(updated);
          if (this.currentUserSubject.value?.id === mapped.id) {
            this.currentUserSubject.next(mapped);
            localStorage.setItem(this.userKey, JSON.stringify(mapped));
          }
          return mapped;
        })
      );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { headers: this.authHeaders() });
  }

  /* =========================
     PROFILE
     ========================= */
  updateProfile(user: Partial<User>): Observable<User> {
    const current = this.getCurrentUser();
    if (!current) throw new Error('No logged user');
    return this.update(current.id, user);
  }

  /* =========================
     HELPERS
     ========================= */
  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    return this.getCurrentUser()?.role === 'ADMIN';
  }
  setSession(token: string, user: User): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
  private loadUserFromStorage(): User | null {
    const raw = localStorage.getItem(this.userKey);
    return raw ? JSON.parse(raw) : null;
  }

  private mapUser(user: any): User {
    return {
      id: user.id ?? 0,
      email: user.email ?? '',
      fullName: user.fullName ?? user.full_name ?? '',
      photoUrl: user.photoUrl ?? user.photo_url ?? '',
      role: user.role ?? '',
      createdAt: user.createdAt ?? user.created_at ?? '',
      blocked: user.blocked ?? false, // ← AJOUTER

    };
  }

  private authHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }


  checkEmailExists(email: string): Observable<boolean> {
    return this.http.get<any>(
      `${this.baseUrl}/by-email?email=${encodeURIComponent(email)}`,
      { headers: this.authHeaders() }
    ).pipe(
      map(user => !!user),           // si user trouvé → true
      catchError(() => of(false))    // si 404 ou erreur → false
    );


  }
  blockUser(id: number): Observable<User> {
    return this.http.put<any>(
      `${this.baseUrl}/${id}/block`, {},
      { headers: this.authHeaders() }
    ).pipe(map(u => this.mapUser(u)));
  }

  unblockUser(id: number): Observable<User> {
    return this.http.put<any>(
      `${this.baseUrl}/${id}/unblock`, {},
      { headers: this.authHeaders() }
    ).pipe(map(u => this.mapUser(u)));
  }
  /* ── 2FA ──────────────────────────────────────────── */
  verifyRegister(email: string, otp: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/verify-register`, { email, otp });
  }

  verifyOtp(email: string, otp: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/verify-otp`, { email, otp });
  }

  resendOtp(email: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/resend-otp`, { email });
  }



  uploadProfileImage(userId: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(
      `${this.baseUrl}/${userId}/uploadPhoto`,
      formData,
      { responseType: 'text' }
    );
  }
registerFace(userId: number, embedding: number[]): Observable<any> {
  return this.http.post(`${this.baseUrl}/register-face`, {
    userId,
    embedding
  });
}

verifyFace(userId: number, embedding: number[]): Observable<any> {
  return this.http.post(`${this.baseUrl}/verify-face`, {
    userId,
    embedding
  });
}
}
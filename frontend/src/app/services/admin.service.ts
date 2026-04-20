import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: string;
  timestamp?: string;
}

/**
 * AdminService — proxy-relative URLs route through proxy.conf.json → gateway (port 9090).
 * No hardcoded localhost ports here; change proxy.conf.json to point at your gateway.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl      = '/api/admin';
  private usersApiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getSuperDashboard(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/super-dashboard`).pipe(map(r => r.data));
  }

  getDashboard(): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/dashboard`).pipe(map(r => r.data));
  }

  getStats(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/stats`).pipe(map(r => r.data));
  }

  searchUsers(query: string): Observable<any[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/search`, { params }).pipe(map(r => r.data));
  }

  filterUsers(role?: string, active?: boolean): Observable<any[]> {
    let params = new HttpParams();
    if (role)              params = params.set('role', role);
    if (active !== undefined) params = params.set('active', active.toString());
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/filter`, { params }).pipe(map(r => r.data));
  }

  exportUsers(): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/export/users`).pipe(map(r => r.data));
  }

  exportMMSE(): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/export/mmse`).pipe(map(r => r.data));
  }

  getActivityLog(limit = 50): Observable<any[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/activity-log`, { params }).pipe(map(r => r.data));
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.usersApiUrl}/${userId}`);
  }

  backupDatabase(): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/backup`, {}).pipe(map(r => r.data));
  }

  submitMMSETest(testData: any): Observable<any> {
    return this.http.post<any>('/api/mmse/submit', testData);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface Community {
  id: number;
  name: string;
  description: string;
}

export interface CommunityPayload {
  name: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class CommunitiesService {
  private readonly baseUrl = `${API_BASE_URL}/api/communities`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Community[]> {
    return this.http.get<Community[]>(this.baseUrl);
  }

  getById(id: number): Observable<Community> {
    return this.http.get<Community>(`${this.baseUrl}/${id}`);
  }

  create(payload: CommunityPayload): Observable<Community> {
    return this.http.post<Community>(this.baseUrl, payload);
  }

  update(id: number, payload: Partial<CommunityPayload>): Observable<Community> {
    return this.http.put<Community>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

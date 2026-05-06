import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface Vote {
  id: number;
  postId?: number;
  username: string;
  value: number;
}

export interface VotePayload {
  post: { id: number };
  username: string;
  value: 1 | -1;
}

@Injectable({ providedIn: 'root' })
export class VotesService {
  private readonly baseUrl = `${API_BASE_URL}/api/votes`;

  constructor(private readonly http: HttpClient) {}

  getAll(postId?: number): Observable<Vote[]> {
    if (postId != null) {
      return this.http.get<Vote[]>(`${this.baseUrl}?postId=${postId}`);
    }
    return this.http.get<Vote[]>(this.baseUrl);
  }

  createOrUpdate(payload: VotePayload): Observable<Vote> {
    return this.http.post<Vote>(this.baseUrl, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}


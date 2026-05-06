import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface Comment {
  id: number;
  postId?: number;
  post?: { id: number };
  content: string;
  author: string;
}

export interface CommentPayload {
  post: { id: number };
  content: string;
  author: string;
}

@Injectable({ providedIn: 'root' })
export class CommentsService {
  private readonly baseUrl = `${API_BASE_URL}/api/comments`;

  constructor(private readonly http: HttpClient) {}

  getAll(postId?: number): Observable<Comment[]> {
    if (postId != null) {
      return this.http.get<Comment[]>(`${this.baseUrl}?postId=${postId}`);
    }
    return this.http.get<Comment[]>(this.baseUrl);
  }

  create(payload: CommentPayload): Observable<Comment> {
    return this.http.post<Comment>(this.baseUrl, payload);
  }

  update(id: number, payload: Partial<CommentPayload>, actingUser: string): Observable<Comment> {
    return this.http.put<Comment>(`${this.baseUrl}/${id}`, payload, {
      headers: { 'X-User': actingUser }
    });
  }

  delete(id: number, actingUser: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, {
      headers: { 'X-User': actingUser }
    });
  }
}

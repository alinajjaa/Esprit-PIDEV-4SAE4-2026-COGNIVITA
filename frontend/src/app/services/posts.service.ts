import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface Post {
  id: number;
  communityId: number;
  title: string;
  content: string;
  author: string;
}

export interface PostPayload {
  communityId: number;
  title: string;
  content: string;
  author: string;
}

@Injectable({ providedIn: 'root' })
export class PostsService {
  private readonly baseUrl = `${API_BASE_URL}/api/posts`;

  constructor(private readonly http: HttpClient) {}

  getAll(communityId?: number): Observable<Post[]> {
    if (communityId != null) {
      return this.http.get<Post[]>(`${this.baseUrl}?communityId=${communityId}`);
    }
    return this.http.get<Post[]>(this.baseUrl);
  }

  create(payload: PostPayload): Observable<Post> {
    return this.http.post<Post>(this.baseUrl, payload);
  }

  update(id: number, payload: Partial<PostPayload>, actingUser: string): Observable<Post> {
    return this.http.put<Post>(`${this.baseUrl}/${id}`, payload, {
      headers: { 'X-User': actingUser }
    });
  }

  delete(id: number, actingUser: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, {
      headers: { 'X-User': actingUser }
    });
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {

  private baseUrl = 'http://localhost:8081/api/users';

  constructor(private http: HttpClient) {}

  uploadProfileImage(userId: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(`${this.baseUrl}/${userId}/uploadPhoto`, formData, {
      responseType: 'text'
    });
  }
}
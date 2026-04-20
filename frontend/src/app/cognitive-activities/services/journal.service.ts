// src/app/cognitive-activities/services/journal.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JournalEntry, MoodStats } from '../models/journal-entry.model';

@Injectable({
  providedIn: 'root'
})
export class JournalService {
  private apiUrl = 'http://localhost:9090/api/journal';

  constructor(private http: HttpClient) {}

  // Récupérer toutes les entrées
  getAllEntries(): Observable<any> {
    return this.http.get<any>(this.apiUrl);
  }

  // Récupérer une entrée par ID
  getEntryById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // Créer une nouvelle entrée
  createEntry(entry: JournalEntry): Observable<any> {
    return this.http.post<any>(this.apiUrl, entry);
  }

  // Mettre à jour une entrée
  updateEntry(id: number, entry: JournalEntry): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, entry);
  }

  // Supprimer une entrée
  deleteEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Récupérer les statistiques
  getMoodStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }
}

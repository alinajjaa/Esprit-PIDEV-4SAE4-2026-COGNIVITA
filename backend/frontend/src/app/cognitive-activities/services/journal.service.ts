// src/app/cognitive-activities/services/journal.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JournalEntry, MoodStats } from '../models/journal-entry.model';

@Injectable({
  providedIn: 'root'
})
export class JournalService {
  // ✅ URL spécifique pour le journal (double /api)
  private apiUrl = 'http://localhost:8082/api/api/journal';

  constructor(private http: HttpClient) {
    console.log('📡 JournalService connecté à:', this.apiUrl);
  }

  // Récupérer toutes les entrées
  getAllEntries(): Observable<JournalEntry[]> {
    console.log('📥 Récupération des entrées depuis:', this.apiUrl);
    return this.http.get<JournalEntry[]>(this.apiUrl);
  }

  // Récupérer une entrée par ID
  getEntryById(id: number): Observable<JournalEntry> {
    return this.http.get<JournalEntry>(`${this.apiUrl}/${id}`);
  }

  // Créer une nouvelle entrée
  createEntry(entry: JournalEntry): Observable<JournalEntry> {
    console.log('📤 Création entrée:', entry);
    return this.http.post<JournalEntry>(this.apiUrl, entry);
  }

  // Mettre à jour une entrée
  updateEntry(id: number, entry: JournalEntry): Observable<JournalEntry> {
    return this.http.put<JournalEntry>(`${this.apiUrl}/${id}`, entry);
  }

  // Supprimer une entrée
  deleteEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Récupérer les statistiques
  getMoodStats(): Observable<MoodStats> {
    return this.http.get<MoodStats>(`${this.apiUrl}/stats`);
  }
}

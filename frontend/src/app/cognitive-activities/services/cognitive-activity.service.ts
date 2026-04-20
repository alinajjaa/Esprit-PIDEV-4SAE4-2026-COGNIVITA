// src/app/cognitive-activities/services/cognitive-activity.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CognitiveActivity {
  id?: number;
  title: string;
  type: 'MEMORY' | 'ATTENTION' | 'LOGIC';
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  description?: string;
  instructions?: string;
  content?: string;

  // ✅ Pour MEMORY
  words?: string[];

  // ✅ Pour ATTENTION (Stroop)
  stroopWord?: string;
  stroopColor?: string;
  stroopCorrect?: string;

  // ✅ Pour LOGIC (Séquence)
  sequence?: number[];
  sequenceAnswer?: number;

  // ✅ Paramètres communs
  timeLimit?: number;
  maxScore?: number;
  imageUrl?: string;
  isActive?: boolean;
  createdAt?: Date;
  updatedAt?: Date;

  // ✅ Pour les statistiques
  participations?: any[];
}

@Injectable({
  providedIn: 'root'
})
export class CognitiveActivityService {
  private apiUrl = 'http://localhost:9090/api/activities';

  constructor(private http: HttpClient) {}

  getAllActivities(): Observable<CognitiveActivity[]> {
    return this.http.get<CognitiveActivity[]>(this.apiUrl);
  }

  getActivityById(id: number): Observable<CognitiveActivity> {
    return this.http.get<CognitiveActivity>(`${this.apiUrl}/${id}`);
  }

  createActivity(activity: CognitiveActivity): Observable<CognitiveActivity> {
    return this.http.post<CognitiveActivity>(this.apiUrl, activity);
  }

  updateActivity(id: number, activity: CognitiveActivity): Observable<CognitiveActivity> {
    return this.http.put<CognitiveActivity>(`${this.apiUrl}/${id}`, activity);
  }

  deleteActivity(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  completeActivity(id: number, score: number, timeSpent: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/complete`, { score, timeSpent });
  }

  getActivitiesByType(type: string): Observable<CognitiveActivity[]> {
    return this.http.get<CognitiveActivity[]>(`${this.apiUrl}/type/${type}`);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MedicationLog {
  id: number;
  patientUserId: number;
  medicationId: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  scheduledTime: string;
  scheduledDate: string;
  status: 'PENDING' | 'TAKEN' | 'MISSED' | 'SKIPPED';
  takenAt: string | null;
  patientNotes: string | null;
  alertSent: boolean;
}

export interface AdherenceScore {
  patientUserId: number;
  weeklyScore: number;
  monthlyScore: number;
  streakDays: number;
  totalScheduled: number;
  totalTaken: number;
  totalMissed: number;
  lastCalculated: string;
  adherenceLevel: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';
}

export interface InteractionAlert {
  type: 'INTERACTION' | 'RISK_FLAG' | 'PROTECTIVE';
  severity: 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  drug1: string;
  drug2: string | null;
  message: string;
  recommendation: string;
}

export interface DrugInteractionReport {
  checkedMedications: string[];
  alerts: InteractionAlert[];
  overallRisk: 'HIGH' | 'MEDIUM' | 'LOW';
}

@Injectable({ providedIn: 'root' })
export class MedicationAdherenceService {
  private readonly base = '/api/adherence';

  constructor(private http: HttpClient) {}

  getTodaysDoses(patientUserId: number): Observable<MedicationLog[]> {
    return this.http.get<MedicationLog[]>(`${this.base}/today/${patientUserId}`);
  }

  getDosesForDate(patientUserId: number, date: string): Observable<MedicationLog[]> {
    return this.http.get<MedicationLog[]>(`${this.base}/${patientUserId}/date/${date}`);
  }

  getAdherenceScore(patientUserId: number): Observable<AdherenceScore> {
    return this.http.get<AdherenceScore>(`${this.base}/${patientUserId}/score`);
  }

  getHistory(patientUserId: number): Observable<MedicationLog[]> {
    return this.http.get<MedicationLog[]>(`${this.base}/${patientUserId}/history`);
  }

  checkIn(patientUserId: number, medicationId: number, date: string, notes = ''): Observable<MedicationLog> {
    return this.http.post<MedicationLog>(`${this.base}/check-in`, {
      patientUserId, medicationId, date, notes
    });
  }

  skipDose(patientUserId: number, medicationId: number, date: string, reason = ''): Observable<MedicationLog> {
    return this.http.post<MedicationLog>(`${this.base}/skip`, {
      patientUserId, medicationId, date, reason
    });
  }

  checkInteractions(medicationNames: string[]): Observable<DrugInteractionReport> {
    return this.http.post<DrugInteractionReport>(`${this.base}/drug-interactions/check`, {
      medicationNames
    });
  }

  recalculate(patientUserId: number): Observable<any> {
    return this.http.post(`${this.base}/${patientUserId}/recalculate`, {});
  }
}

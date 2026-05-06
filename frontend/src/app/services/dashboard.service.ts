import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface AlertTrendResponseDto {
  date: string;
  totalAlerts: number;
  outOfZoneCount: number;
  noMovementCount: number;
}

export interface AlertTypePressureDto {
  type: 'OUT_OF_ZONE' | 'NO_MOVEMENT' | string;
  totalCount: number;
  unreadCount: number;
  weightedSeverity: number;
  avgAgeHours: number | null;
}

export interface PatientSafetyBurdenDto {
  patientId: number;
  totalAlerts: number;
  unreadAlerts: number;
  noMovementCount: number;
  outOfZoneCount: number;
  riskScore: number;
}

export interface AppointmentComplianceDto {
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
  appointmentType: string;
  totalAppointments: number;
  completedCount: number;
  missedCount: number;
  cancelledCount: number;
  avgDelayHours: number | null;
}

export interface MmseMonthlySeverityDto {
  month: string;
  totalTests: number;
  normalCount: number;
  mildCount: number;
  moderateCount: number;
  severeCount: number;
  avgScore: number | null;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${API_BASE_URL}/api/dashboard`;

  constructor(private readonly http: HttpClient) {}

  getAlertTrend(from?: string, to?: string): Observable<AlertTrendResponseDto[]> {
    const range = this.resolveDateTimeRange(from, to, 7);
    return this.http.get<AlertTrendResponseDto[]>(`${this.baseUrl}/alerts/trend`, {
      params: this.buildDateTimeParams(range.from, range.to)
    });
  }

  getAlertPressure(from?: string, to?: string): Observable<AlertTypePressureDto[]> {
    const range = this.resolveDateTimeRange(from, to, 7);
    return this.http.get<AlertTypePressureDto[]>(`${this.baseUrl}/alerts/pressure`, {
      params: this.buildDateTimeParams(range.from, range.to)
    });
  }

  getPatientRanking(from?: string, to?: string): Observable<PatientSafetyBurdenDto[]> {
    const range = this.resolveDateTimeRange(from, to, 7);
    return this.http.get<PatientSafetyBurdenDto[]>(`${this.baseUrl}/alerts/ranking`, {
      params: this.buildDateTimeParams(range.from, range.to)
    });
  }

  getAppointmentCompliance(from?: string, to?: string): Observable<AppointmentComplianceDto[]> {
    const range = this.resolveDateTimeRange(from, to, 20);
    return this.http.get<AppointmentComplianceDto[]>(`${this.baseUrl}/appointments/compliance`, {
      params: this.buildDateTimeParams(range.from, range.to)
    });
  }

  getMmseSeverity(fromDate?: string, toDate?: string): Observable<MmseMonthlySeverityDto[]> {
    const range = this.resolveDateRange(fromDate, toDate, 90);
    const params = new HttpParams()
      .set('fromDate', range.fromDate)
      .set('toDate', range.toDate)
      .set('_', Date.now().toString());
    return this.http.get<MmseMonthlySeverityDto[]>(`${this.baseUrl}/mmse/severity`, { params });
  }

  private buildDateTimeParams(from: string, to: string): HttpParams {
    return new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('_', Date.now().toString());
  }

  private resolveDateTimeRange(from: string | undefined, to: string | undefined, pastDays: number): { from: string; to: string } {
    if (from && to) {
      return { from, to };
    }
    const endDate = new Date();
    const startDate = new Date(endDate);
    startDate.setDate(startDate.getDate() - (pastDays - 1));
    startDate.setHours(0, 0, 0, 0);
    return {
      from: this.formatLocalDateTime(startDate),
      to: this.formatLocalDateTime(endDate)
    };
  }

  private resolveDateRange(fromDate: string | undefined, toDate: string | undefined, pastDays: number): { fromDate: string; toDate: string } {
    if (fromDate && toDate) {
      return { fromDate, toDate };
    }
    const endDate = new Date();
    const startDate = new Date(endDate);
    startDate.setDate(startDate.getDate() - (pastDays - 1));
    return {
      fromDate: this.formatDateOnly(startDate),
      toDate: this.formatDateOnly(endDate)
    };
  }

  private formatLocalDateTime(date: Date): string {
    const pad = (value: number): string => value.toString().padStart(2, '0');
    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const min = pad(date.getMinutes());
    const ss = pad(date.getSeconds());
    return `${yyyy}-${mm}-${dd}T${hh}:${min}:${ss}`;
  }

  private formatDateOnly(date: Date): string {
    const pad = (value: number): string => value.toString().padStart(2, '0');
    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    return `${yyyy}-${mm}-${dd}`;
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../core/api';

export interface NameCount  { name: string; count: number; }
export interface DayCount   { day: string; count: number; }
export interface HourCount  { hour: number; count: number; }
export interface WeekdayCount { weekday: number; count: number; }

export interface DoctorStatsDto {
  medecinId: number;
  from: string;
  to: string;
  total: number;
  upcoming: number;
  today: number;
  uniquePatients: number;
  byStatus: Record<string, number>;
  cancellationRate: number;
  rdvPerDay: DayCount[];
  rdvPerHour: HourCount[];
  rdvPerWeekday: WeekdayCount[];
  topRooms: NameCount[];
  topNurses: NameCount[];
  topMedications: NameCount[];
  topPatients: NameCount[];
}

@Injectable({ providedIn: 'root' })
export class DoctorStatsService {
  private base = `${API.DOCTORS_API}`;

  constructor(private http: HttpClient) {}

  getStats(medecinId: number, from: string, to: string): Observable<DoctorStatsDto> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<DoctorStatsDto>(`${this.base}/${medecinId}/stats`, { params });
  }
}

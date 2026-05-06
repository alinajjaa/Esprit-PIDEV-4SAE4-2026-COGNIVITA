import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface TrackingLocation {
  id: number;
  patientId: number;
  latitude: number;
  longitude: number;
  timestamp: string;
  motionState: string | null;
}

export interface GeofenceZone {
  id: number;
  patientId: number;
  centerLatitude: number;
  centerLongitude: number;
  radiusMeters: number;
}

export interface GeofencePayload {
  patientId: number;
  centerLatitude: number;
  centerLongitude: number;
  radiusMeters: number;
}

export interface TrackingAlert {
  id: number;
  patientId: number;
  type: 'OUT_OF_ZONE' | 'NO_MOVEMENT';
  message: string;
  createdAt: string;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class TrackingService {
  private readonly baseUrl = `${API_BASE_URL}/api/track`;

  constructor(private readonly http: HttpClient) {}

  getLatestLocations(): Observable<TrackingLocation[]> {
    const params = new HttpParams().set('_', Date.now().toString());
    return this.http.get<TrackingLocation[]>(`${this.baseUrl}/location/latest`, { params });
  }

  getGeofence(patientId: number): Observable<GeofenceZone> {
    return this.http.get<GeofenceZone>(`${this.baseUrl}/geofence/${patientId}`);
  }

  upsertGeofence(payload: GeofencePayload): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/geofence`, payload);
  }

  deleteGeofence(patientId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/geofence/${patientId}`);
  }

  getAlerts(patientId?: number, unreadOnly = false): Observable<TrackingAlert[]> {
    let params = new HttpParams().set('unreadOnly', String(unreadOnly)).set('_', Date.now().toString());
    if (patientId != null) {
      params = params.set('patientId', patientId);
    }
    return this.http.get<TrackingAlert[]>(`${this.baseUrl}/alerts`, { params });
  }

  markAlertAsRead(alertId: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.baseUrl}/alerts/${alertId}/read`, {});
  }
}

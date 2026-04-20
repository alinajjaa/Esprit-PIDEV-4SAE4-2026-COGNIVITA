import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationRecord {
  id: number;
  userId: number;
  referenceId: number | null;
  notificationType: string;
  channel: 'EMAIL' | 'SMS' | 'PUSH' | 'IN_APP';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  title: string;
  message: string;
  status: 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED' | 'READ';
  sentAt: string | null;
  escalationLevel: number;
  createdAt: string;
}

export interface EscalationRule {
  id: number;
  patientUserId: number;
  escalationType: string;
  currentLevel: number;
  active: boolean;
  contextMessage: string;
  triggeredAt: string;
  lastEscalatedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly base = '/api/notifications';

  constructor(private http: HttpClient) {}

  getUserNotifications(userId: number): Observable<NotificationRecord[]> {
    return this.http.get<NotificationRecord[]>(`${this.base}/user/${userId}`);
  }

  getRecentNotifications(userId: number): Observable<NotificationRecord[]> {
    return this.http.get<NotificationRecord[]>(`${this.base}/user/${userId}/recent`);
  }

  getUnreadCount(userId: number): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.base}/user/${userId}/unread-count`);
  }

  dismissNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getEscalations(patientUserId: number): Observable<EscalationRule[]> {
    return this.http.get<EscalationRule[]>(`${this.base}/escalations/patient/${patientUserId}`);
  }

  getActiveEscalations(): Observable<EscalationRule[]> {
    return this.http.get<EscalationRule[]>(`${this.base}/escalations/active`);
  }

  /** FIX: was missing — required to update badge count correctly */
  markAsRead(id: number): Observable<NotificationRecord> {
    return this.http.patch<NotificationRecord>(`${this.base}/${id}/read`, {});
  }

  markAllAsRead(userId: number): Observable<{ markedRead: number }> {
    return this.http.patch<{ markedRead: number }>(`${this.base}/user/${userId}/read-all`, {});
  }
}

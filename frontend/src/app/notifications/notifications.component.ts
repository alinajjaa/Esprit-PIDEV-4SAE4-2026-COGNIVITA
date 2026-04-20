import { Component, OnInit, OnDestroy } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { NotificationService, NotificationRecord, EscalationRule } from '../services/notification.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notifications-page">
      <div class="page-header">
        <h1>🔔 Notifications Center</h1>
        <p class="subtitle">All system alerts, reminders, and escalation events</p>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button [class.active]="activeTab === 'notifications'" (click)="activeTab = 'notifications'">
          📬 All Notifications
          <span class="badge" *ngIf="unreadCount > 0">{{ unreadCount }}</span>
        </button>
        <button [class.active]="activeTab === 'escalations'" (click)="activeTab = 'escalations'; loadEscalations()">
          🚨 Active Escalations
          <span class="badge danger" *ngIf="activeEscalations.length > 0">{{ activeEscalations.length }}</span>
        </button>
      </div>

      <!-- Notifications List -->
      <div *ngIf="activeTab === 'notifications'">
        <div class="toolbar">
          <span class="count">{{ notifications.length }} total · {{ unreadCount }} unread</span>
          <button class="btn-secondary" (click)="loadNotifications()">↻ Refresh</button>
        </div>

        <div class="empty-state" *ngIf="notifications.length === 0">
          No notifications to display.
        </div>

        <div class="notif-list">
          <div class="notif-card" *ngFor="let n of notifications"
               [class]="'notif-' + n.severity.toLowerCase()">
            <div class="notif-left">
              <span class="notif-icon">{{ severityIcon(n.severity) }}</span>
              <div class="notif-content">
                <div class="notif-title">{{ n.title }}</div>
                <div class="notif-message">{{ n.message }}</div>
                <div class="notif-meta">
                  <span class="chip chip-{{ n.notificationType.toLowerCase() }}">{{ formatType(n.notificationType) }}</span>
                  <span class="chip chip-channel">{{ n.channel }}</span>
                  <span class="chip chip-{{ n.status.toLowerCase() }}">{{ n.status }}</span>
                  <span class="notif-time">{{ timeAgo(n.createdAt) }}</span>
                </div>
              </div>
            </div>
            <button class="btn-dismiss" (click)="dismiss(n)" title="Dismiss">✕</button>
          </div>
        </div>
      </div>

      <!-- Escalations List -->
      <div *ngIf="activeTab === 'escalations'">
        <div class="toolbar">
          <span class="count">{{ activeEscalations.length }} active escalation(s)</span>
          <button class="btn-secondary" (click)="loadEscalations()">↻ Refresh</button>
        </div>

        <div class="empty-state" *ngIf="activeEscalations.length === 0">
          ✅ No active escalations. All patients are being monitored normally.
        </div>

        <div class="escalation-list">
          <div class="escalation-card" *ngFor="let e of activeEscalations">
            <div class="esc-header">
              <span class="esc-badge">{{ escalationTypeLabel(e.escalationType) }}</span>
              <span class="esc-level">Level {{ e.currentLevel }}: {{ levelLabel(e.currentLevel) }}</span>
            </div>
            <div class="esc-patient">Patient ID: <strong>{{ e.patientUserId }}</strong></div>
            <div class="esc-message">{{ e.contextMessage }}</div>
            <div class="esc-timeline">
              <div class="step" [class.done]="e.currentLevel >= 0">🔴 Patient notified</div>
              <div class="step" [class.done]="e.currentLevel >= 1">👨‍👩‍👧 Caregiver alerted</div>
              <div class="step" [class.done]="e.currentLevel >= 2">👨‍⚕️ Doctor escalated</div>
            </div>
            <div class="esc-time">Triggered: {{ formatDate(e.triggeredAt) }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .notifications-page { max-width: 900px; margin: 90px auto 40px; padding: 0 20px; font-family: sans-serif; color: #e2e8f0; }
    .page-header h1 { font-size: 2rem; margin-bottom: 6px; }
    .subtitle { color: #94a3b8; margin-bottom: 24px; }

    .tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid #334155; }
    .tabs button { background: none; border: none; color: #94a3b8; padding: 10px 18px; cursor: pointer;
                   font-size: 0.95rem; position: relative; border-bottom: 2px solid transparent; transition: all .2s; }
    .tabs button.active { color: #6366f1; border-bottom-color: #6366f1; }
    .badge { background: #6366f1; color: white; border-radius: 10px; padding: 2px 7px; font-size: 0.7rem; margin-left: 6px; }
    .badge.danger { background: #ef4444; }

    .toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .count { color: #64748b; font-size: 0.9rem; }
    .btn-secondary { background: #334155; color: #e2e8f0; border: none; border-radius: 8px; padding: 7px 14px; cursor: pointer; }
    .empty-state { color: #64748b; text-align: center; padding: 48px; font-size: 1rem; }

    .notif-list { display: flex; flex-direction: column; gap: 10px; }
    .notif-card { display: flex; justify-content: space-between; align-items: flex-start;
                  background: #1e293b; border-radius: 10px; padding: 14px 16px; border-left: 4px solid #334155; }
    .notif-card.notif-critical { border-left-color: #ef4444; }
    .notif-card.notif-warning  { border-left-color: #f97316; }
    .notif-card.notif-info     { border-left-color: #3b82f6; }
    .notif-left { display: flex; gap: 14px; align-items: flex-start; flex: 1; }
    .notif-icon { font-size: 1.4rem; }
    .notif-title { font-weight: 600; font-size: 0.95rem; margin-bottom: 4px; }
    .notif-message { color: #94a3b8; font-size: 0.85rem; margin-bottom: 8px; line-height: 1.4; }
    .notif-meta { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
    .notif-time { color: #475569; font-size: 0.78rem; margin-left: 4px; }
    .chip { font-size: 0.72rem; padding: 2px 8px; border-radius: 10px; background: #334155; }
    .chip-sent  { background: #22c55e22; color: #22c55e; }
    .chip-failed { background: #ef444422; color: #ef4444; }
    .btn-dismiss { background: none; border: none; color: #475569; cursor: pointer; font-size: 1rem; padding: 4px 8px;
                   border-radius: 6px; transition: all .15s; }
    .btn-dismiss:hover { background: #ef444422; color: #ef4444; }

    .escalation-list { display: flex; flex-direction: column; gap: 14px; }
    .escalation-card { background: #1e293b; border-radius: 12px; padding: 18px 20px; border: 1px solid #ef444444; }
    .esc-header { display: flex; justify-content: space-between; margin-bottom: 10px; }
    .esc-badge { background: #ef444422; color: #ef4444; padding: 4px 12px; border-radius: 12px; font-size: 0.82rem; font-weight: 600; }
    .esc-level { color: #f97316; font-size: 0.85rem; }
    .esc-patient { color: #94a3b8; font-size: 0.9rem; margin-bottom: 6px; }
    .esc-message { color: #e2e8f0; font-size: 0.88rem; margin-bottom: 14px; line-height: 1.5; }
    .esc-timeline { display: flex; gap: 0; margin-bottom: 12px; }
    .step { flex: 1; text-align: center; padding: 8px 4px; font-size: 0.78rem; color: #475569;
            border-bottom: 2px solid #334155; position: relative; }
    .step.done { color: #22c55e; border-bottom-color: #22c55e; }
    .esc-time { color: #475569; font-size: 0.78rem; }
  `]
})
export class NotificationsComponent implements OnInit, OnDestroy {
  activeTab: 'notifications' | 'escalations' = 'notifications';
  notifications: NotificationRecord[] = [];
  activeEscalations: EscalationRule[] = [];
  unreadCount = 0;

  userId: number;
  private pollSub?: Subscription;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService
  ) {
    // FIX: read real userId from auth session instead of hardcoded 1
    this.userId = this.authService.getCurrentUserId();
  }

  ngOnInit() {
    this.loadNotifications();
    this.loadUnreadCount();
    // Auto-refresh every 60 s so new alerts appear without manual refresh
    this.pollSub = interval(60_000).subscribe(() => {
      this.loadNotifications();
      this.loadUnreadCount();
    });
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  loadNotifications() {
    this.notificationService.getUserNotifications(this.userId).subscribe({
      next: n => this.notifications = n,
      error: () => {}
    });
  }

  loadUnreadCount() {
    this.notificationService.getUnreadCount(this.userId).subscribe({
      next: r => this.unreadCount = r.unreadCount,
      error: () => {}
    });
  }

  loadEscalations() {
    this.notificationService.getActiveEscalations().subscribe({
      next: e => this.activeEscalations = e,
      error: () => {}
    });
  }

  dismiss(n: NotificationRecord) {
    // FIX: mark as read first so badge count updates correctly, then delete
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        this.notificationService.dismissNotification(n.id).subscribe({
          next: () => {
            this.notifications = this.notifications.filter(x => x.id !== n.id);
            this.loadUnreadCount();
          }
        });
      },
      error: () => {
        // Still delete even if mark-read fails
        this.notificationService.dismissNotification(n.id).subscribe({
          next: () => this.notifications = this.notifications.filter(x => x.id !== n.id)
        });
      }
    });
  }

  severityIcon(s: string): string {
    return { CRITICAL: '🔴', WARNING: '🟠', INFO: '🔵' }[s] ?? '⚪';
  }

  formatType(t: string): string {
    return t.replace(/_/g, ' ');
  }

  levelLabel(level: number): string {
    return ['Patient', 'Caregiver', 'Doctor'][level] ?? 'Unknown';
  }

  escalationTypeLabel(t: string): string {
    return { MISSED_DOSE: '💊 Missed Dose', MISSED_APPOINTMENT: '📅 Missed Appointment', HIGH_RISK_SCORE: '⚠️ High Risk' }[t] ?? t;
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1) return 'just now';
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}

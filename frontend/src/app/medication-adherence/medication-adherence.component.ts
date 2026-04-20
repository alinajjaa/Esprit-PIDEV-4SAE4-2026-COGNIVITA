import { AuthService } from '../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MedicationAdherenceService, MedicationLog, AdherenceScore, DrugInteractionReport } from '../services/medication-adherence.service';

@Component({
  selector: 'app-medication-adherence',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="adherence-page">
      <div class="page-header">
        <h1>💊 Medication Adherence Tracker</h1>
        <p class="subtitle">Track daily medication intake, monitor adherence, and check drug interactions</p>
      </div>

      <!-- Patient ID input (demo) -->
      <div class="patient-bar">
        <label>Patient ID</label>
        <input type="number" [(ngModel)]="patientUserId" (change)="loadAll()" placeholder="Enter patient ID" />
        <button class="btn-primary" (click)="loadAll()">Load</button>
      </div>

      <!-- Score cards -->
      <div class="score-grid" *ngIf="score">
        <div class="score-card" [class]="'score-' + score.adherenceLevel.toLowerCase()">
          <div class="score-label">Weekly Adherence</div>
          <div class="score-value">{{ score.weeklyScore | number:'1.0-0' }}%</div>
          <div class="score-badge">{{ score.adherenceLevel }}</div>
        </div>
        <div class="score-card neutral">
          <div class="score-label">Monthly Adherence</div>
          <div class="score-value">{{ score.monthlyScore | number:'1.0-0' }}%</div>
        </div>
        <div class="score-card streak">
          <div class="score-label">Current Streak</div>
          <div class="score-value">{{ score.streakDays }}</div>
          <div class="score-badge">days 🔥</div>
        </div>
        <div class="score-card neutral">
          <div class="score-label">Taken / Missed</div>
          <div class="score-value">{{ score.totalTaken }} / {{ score.totalMissed }}</div>
        </div>
      </div>

      <!-- Today's doses -->
      <div class="section">
        <div class="section-header">
          <h2>📅 Today's Doses — {{ today }}</h2>
          <button class="btn-secondary" (click)="loadToday()">↻ Refresh</button>
        </div>

        <div class="empty-state" *ngIf="todayLogs.length === 0">
          No doses scheduled for today.
        </div>

        <div class="dose-list">
          <div class="dose-card" *ngFor="let log of todayLogs" [class]="'dose-' + log.status.toLowerCase()">
            <div class="dose-left">
              <span class="dose-status-icon">{{ statusIcon(log.status) }}</span>
              <div>
                <div class="dose-name">{{ log.medicationName }}</div>
                <div class="dose-meta">{{ log.dosage }} · {{ log.frequency }} · {{ formatTime(log.scheduledTime) }}</div>
                <div class="dose-taken" *ngIf="log.takenAt">✓ Taken at {{ formatTime(log.takenAt) }}</div>
                <div class="dose-note" *ngIf="log.patientNotes">Note: {{ log.patientNotes }}</div>
              </div>
            </div>
            <div class="dose-actions" *ngIf="log.status === 'PENDING'">
              <button class="btn-taken" (click)="confirmTaken(log)">✓ Taken</button>
              <button class="btn-skip"  (click)="skipDose(log)">Skip</button>
            </div>
            <div class="dose-done" *ngIf="log.status === 'TAKEN'">✓ Done</div>
            <div class="dose-done missed" *ngIf="log.status === 'MISSED'">✗ Missed</div>
            <div class="dose-done skipped" *ngIf="log.status === 'SKIPPED'">~ Skipped</div>
          </div>
        </div>
      </div>

      <!-- Drug Interaction Checker -->
      <div class="section interaction-section">
        <h2>⚠️ Drug Interaction Checker</h2>
        <p class="hint">Enter medication names (comma-separated) to check for known cognitive risk interactions.</p>
        <div class="interaction-input-row">
          <input type="text" [(ngModel)]="interactionInput"
                 placeholder="e.g. donepezil, amitriptyline, lorazepam"
                 class="interaction-input" />
          <button class="btn-primary" (click)="checkInteractions()">Check Interactions</button>
        </div>

        <div class="interaction-results" *ngIf="interactionReport">
          <div class="overall-risk" [class]="'risk-' + interactionReport.overallRisk.toLowerCase()">
            Overall Risk: <strong>{{ interactionReport.overallRisk }}</strong>
          </div>
          <div class="alert-empty" *ngIf="interactionReport.alerts.length === 0">
            ✅ No known interactions or risk flags found.
          </div>
          <div class="interaction-alert" *ngFor="let alert of interactionReport.alerts"
               [class]="'alert-' + alert.severity.toLowerCase()">
            <div class="alert-header">
              <span class="alert-badge">{{ alert.type === 'INTERACTION' ? '⚡ Interaction' : alert.type === 'PROTECTIVE' ? '🛡️ Protective' : '⚠️ Risk Flag' }}</span>
              <span class="alert-severity">{{ alert.severity }}</span>
            </div>
            <div class="alert-drugs">
              <strong>{{ alert.drug1 }}</strong>
              <span *ngIf="alert.drug2"> + <strong>{{ alert.drug2 }}</strong></span>
            </div>
            <div class="alert-message">{{ alert.message }}</div>
            <div class="alert-rec">💡 {{ alert.recommendation }}</div>
          </div>
        </div>
      </div>

      <!-- Recent history (last 7 days) -->
      <div class="section">
        <h2>📊 Recent History (Last 30 Logs)</h2>
        <div class="history-table" *ngIf="history.length > 0">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Medication</th>
                <th>Dosage</th>
                <th>Status</th>
                <th>Taken At</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let log of history.slice(0, 30)" [class]="'row-' + log.status.toLowerCase()">
                <td>{{ log.scheduledDate }}</td>
                <td>{{ log.medicationName }}</td>
                <td>{{ log.dosage }}</td>
                <td><span class="status-chip" [class]="'chip-' + log.status.toLowerCase()">{{ log.status }}</span></td>
                <td>{{ log.takenAt ? formatTime(log.takenAt) : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="empty-state" *ngIf="history.length === 0">No history found.</div>
      </div>

      <!-- Toast messages -->
      <div class="toast" *ngIf="toastMsg" [class.toast-error]="toastIsError">{{ toastMsg }}</div>
    </div>
  `,
  styles: [`
    .adherence-page { max-width: 1000px; margin: 90px auto 40px; padding: 0 20px; font-family: sans-serif; color: #e2e8f0; }
    .page-header h1 { font-size: 2rem; margin-bottom: 6px; }
    .subtitle { color: #94a3b8; margin-bottom: 24px; }
    .patient-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 28px; }
    .patient-bar label { color: #94a3b8; }
    .patient-bar input { background: #1e293b; border: 1px solid #334155; color: #e2e8f0; border-radius: 8px; padding: 8px 12px; width: 140px; }

    .score-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 32px; }
    .score-card { background: #1e293b; border-radius: 12px; padding: 20px; text-align: center; border: 1px solid #334155; }
    .score-card.score-excellent { border-color: #22c55e; }
    .score-card.score-good      { border-color: #3b82f6; }
    .score-card.score-fair      { border-color: #f59e0b; }
    .score-card.score-poor      { border-color: #ef4444; }
    .score-card.streak          { border-color: #f97316; }
    .score-label { font-size: 0.8rem; color: #94a3b8; margin-bottom: 8px; }
    .score-value { font-size: 2rem; font-weight: 700; color: #f1f5f9; }
    .score-badge { font-size: 0.75rem; color: #94a3b8; margin-top: 4px; }

    .section { background: #1e293b; border-radius: 12px; padding: 24px; margin-bottom: 28px; border: 1px solid #334155; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .section h2 { font-size: 1.2rem; margin: 0 0 16px; }
    .empty-state { color: #64748b; text-align: center; padding: 24px; }

    .dose-list { display: flex; flex-direction: column; gap: 12px; }
    .dose-card { display: flex; justify-content: space-between; align-items: center;
                 background: #0f172a; border-radius: 10px; padding: 14px 18px; border-left: 4px solid #334155; }
    .dose-card.dose-taken   { border-left-color: #22c55e; }
    .dose-card.dose-missed  { border-left-color: #ef4444; }
    .dose-card.dose-skipped { border-left-color: #f59e0b; }
    .dose-card.dose-pending { border-left-color: #3b82f6; }
    .dose-left { display: flex; gap: 14px; align-items: flex-start; }
    .dose-status-icon { font-size: 1.5rem; }
    .dose-name { font-weight: 600; font-size: 1rem; }
    .dose-meta { color: #94a3b8; font-size: 0.85rem; margin-top: 2px; }
    .dose-taken { color: #22c55e; font-size: 0.8rem; margin-top: 3px; }
    .dose-note { color: #f59e0b; font-size: 0.8rem; font-style: italic; margin-top: 2px; }
    .dose-actions { display: flex; gap: 8px; }
    .dose-done { font-size: 0.85rem; padding: 4px 10px; border-radius: 6px; background: #22c55e22; color: #22c55e; }
    .dose-done.missed  { background: #ef444422; color: #ef4444; }
    .dose-done.skipped { background: #f59e0b22; color: #f59e0b; }

    .btn-primary  { background: #6366f1; color: white; border: none; border-radius: 8px; padding: 8px 16px; cursor: pointer; font-size: 0.9rem; }
    .btn-secondary { background: #334155; color: #e2e8f0; border: none; border-radius: 8px; padding: 8px 14px; cursor: pointer; }
    .btn-taken { background: #22c55e22; color: #22c55e; border: 1px solid #22c55e; border-radius: 7px; padding: 6px 14px; cursor: pointer; font-weight: 600; }
    .btn-skip  { background: #f59e0b22; color: #f59e0b; border: 1px solid #f59e0b; border-radius: 7px; padding: 6px 12px; cursor: pointer; }

    .interaction-section .hint { color: #94a3b8; font-size: 0.9rem; margin: -8px 0 14px; }
    .interaction-input-row { display: flex; gap: 12px; margin-bottom: 18px; }
    .interaction-input { flex: 1; background: #0f172a; border: 1px solid #334155; color: #e2e8f0; border-radius: 8px; padding: 9px 14px; font-size: 0.9rem; }
    .overall-risk { font-size: 1rem; padding: 10px 16px; border-radius: 8px; margin-bottom: 14px; background: #0f172a; }
    .overall-risk.risk-high   { border-left: 4px solid #ef4444; color: #ef4444; }
    .overall-risk.risk-medium { border-left: 4px solid #f59e0b; color: #f59e0b; }
    .overall-risk.risk-low    { border-left: 4px solid #22c55e; color: #22c55e; }
    .interaction-alert { background: #0f172a; border-radius: 10px; padding: 14px 16px; margin-bottom: 10px; border-left: 4px solid #334155; }
    .alert-high   { border-left-color: #ef4444; }
    .alert-medium { border-left-color: #f59e0b; }
    .alert-low    { border-left-color: #3b82f6; }
    .alert-info   { border-left-color: #22c55e; }
    .alert-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
    .alert-badge  { font-size: 0.8rem; font-weight: 600; }
    .alert-severity { font-size: 0.75rem; background: #1e293b; border-radius: 4px; padding: 2px 8px; }
    .alert-drugs  { font-size: 0.95rem; margin-bottom: 6px; }
    .alert-message { color: #94a3b8; font-size: 0.85rem; margin-bottom: 6px; }
    .alert-rec { color: #fbbf24; font-size: 0.82rem; }
    .alert-empty { color: #22c55e; padding: 12px 0; }

    .history-table { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.88rem; }
    th { color: #64748b; font-weight: 500; padding: 10px 12px; text-align: left; border-bottom: 1px solid #334155; }
    td { padding: 9px 12px; border-bottom: 1px solid #1e293b; }
    tr:hover td { background: #1e293b55; }
    .status-chip { padding: 3px 9px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; }
    .chip-taken   { background: #22c55e22; color: #22c55e; }
    .chip-missed  { background: #ef444422; color: #ef4444; }
    .chip-skipped { background: #f59e0b22; color: #f59e0b; }
    .chip-pending { background: #3b82f622; color: #3b82f6; }

    .toast { position: fixed; bottom: 24px; right: 24px; background: #22c55e; color: white;
             padding: 12px 20px; border-radius: 10px; font-size: 0.9rem; z-index: 9999; }
    .toast.toast-error { background: #ef4444; }
  `]
})
export class MedicationAdherenceComponent implements OnInit {
  patientUserId!: number;
  today = new Date().toISOString().split('T')[0];

  todayLogs: MedicationLog[] = [];
  history: MedicationLog[] = [];
  score: AdherenceScore | null = null;
  interactionReport: DrugInteractionReport | null = null;
  interactionInput = '';
  toastMsg = '';
  toastIsError = false;

  constructor(private adherenceService: MedicationAdherenceService,
    private authService: AuthService) {}

  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.loadToday();
    this.loadScore();
    this.loadHistory();
  }

  loadToday() {
    this.adherenceService.getTodaysDoses(this.patientUserId).subscribe({
      next: logs => this.todayLogs = logs,
      error: () => this.showToast('Failed to load today\'s doses', true)
    });
  }

  loadScore() {
    this.adherenceService.getAdherenceScore(this.patientUserId).subscribe({
      next: s => this.score = s,
      error: () => {}
    });
  }

  loadHistory() {
    this.adherenceService.getHistory(this.patientUserId).subscribe({
      next: h => this.history = h,
      error: () => {}
    });
  }

  confirmTaken(log: MedicationLog) {
    this.adherenceService.checkIn(this.patientUserId, log.medicationId, log.scheduledDate).subscribe({
      next: updated => {
        const idx = this.todayLogs.findIndex(l => l.id === log.id);
        if (idx > -1) this.todayLogs[idx] = updated;
        this.loadScore();
        this.showToast(`✓ ${log.medicationName} marked as taken`);
      },
      error: () => this.showToast('Failed to confirm dose', true)
    });
  }

  skipDose(log: MedicationLog) {
    this.adherenceService.skipDose(this.patientUserId, log.medicationId, log.scheduledDate, 'Patient skipped').subscribe({
      next: updated => {
        const idx = this.todayLogs.findIndex(l => l.id === log.id);
        if (idx > -1) this.todayLogs[idx] = updated;
        this.loadScore();
        this.showToast(`Skipped ${log.medicationName}`);
      },
      error: () => this.showToast('Failed to skip dose', true)
    });
  }

  checkInteractions() {
    const names = this.interactionInput.split(',').map(s => s.trim()).filter(Boolean);
    if (!names.length) return;
    this.adherenceService.checkInteractions(names).subscribe({
      next: report => this.interactionReport = report,
      error: () => this.showToast('Interaction check failed', true)
    });
  }

  statusIcon(status: string): string {
    return { PENDING: '⏳', TAKEN: '✅', MISSED: '❌', SKIPPED: '⏭️' }[status] ?? '❓';
  }

  formatTime(dt: string): string {
    if (!dt) return '';
    return new Date(dt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  private showToast(msg: string, isError = false) {
    this.toastMsg = msg;
    this.toastIsError = isError;
    setTimeout(() => this.toastMsg = '', 3500);
  }
}

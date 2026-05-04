// src/app/cognitive-activities/journal/journal-history/journal-history.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JournalService } from '../../services/journal.service';
import { JournalEntry } from '../../models/journal-entry.model';

@Component({
  selector: 'app-journal-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="history-container">
      <h2>📋 Historique des entrées</h2>

      <!-- Loading -->
      <div *ngIf="loading" class="loading">
        <div class="spinner"></div>
        <p>Chargement...</p>
      </div>

      <!-- Error -->
      <div *ngIf="error" class="error-message">
        ⚠️ {{ error }}
      </div>

      <!-- Empty state -->
      <div *ngIf="!loading && !error && entries.length === 0" class="empty-state">
        <p>Aucune entrée pour le moment</p>
        <a routerLink="/journal/new" class="btn-primary">Créer votre première entrée</a>
      </div>

      <!-- Liste des entrées -->
      <div *ngIf="!loading && entries.length > 0" class="entries-list">
        <div *ngFor="let entry of entries" class="entry-card">
          <div class="entry-header">
            <span class="entry-date">{{ entry.date | date:'dd/MM/yyyy' }}</span>
            <span class="entry-mood">
              {{ moodEmoji(entry.mood) }} {{ entry.mood }}/5
            </span>
          </div>

          <div class="entry-stats">
            <span class="stat">⚡ {{ entry.energy }}/5</span>
            <span class="stat">😰 {{ entry.stress }}/5</span>
            <span class="stat">😴 {{ entry.sleepHours }}h</span>
          </div>

          <div class="entry-activities" *ngIf="entry.activities.length">
            <span *ngFor="let activity of entry.activities" class="activity-tag">
              {{ activity }}
            </span>
          </div>

          <p *ngIf="entry.notes" class="entry-notes">"{{ entry.notes }}"</p>

          <div class="entry-actions">
            <ng-container *ngIf="hasEntryId(entry)">
              <a [routerLink]="['/journal', entry.id]" class="btn-view">Voir</a>
              <a [routerLink]="['/journal/edit', entry.id]" class="btn-edit">Modifier</a>
            </ng-container>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .history-container {
      max-width: 800px;
      margin: 0 auto;
      padding: 20px;
    }
    h2 {
      color: #2c3e50;
      margin-bottom: 25px;
    }
    .loading {
      text-align: center;
      padding: 40px;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #f3f3f3;
      border-top: 3px solid #3498db;
      border-radius: 50%;
      margin: 0 auto 10px;
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    .error-message {
      background: #fee;
      color: #c0392b;
      padding: 15px;
      border-radius: 8px;
      margin: 20px 0;
      text-align: center;
    }
    .empty-state {
      text-align: center;
      padding: 60px 20px;
      background: #f8f9fa;
      border-radius: 12px;
      color: #7f8c8d;
    }
    .btn-primary {
      display: inline-block;
      background: #3498db;
      color: white;
      padding: 12px 30px;
      border-radius: 8px;
      text-decoration: none;
      margin-top: 20px;
      transition: background 0.3s;
    }
    .btn-primary:hover {
      background: #2980b9;
    }
    .entries-list {
      display: flex;
      flex-direction: column;
      gap: 15px;
    }
    .entry-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      border: 1px solid #e0e0e0;
      transition: transform 0.2s;
    }
    .entry-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    .entry-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      padding-bottom: 10px;
      border-bottom: 1px solid #eee;
    }
    .entry-date {
      font-weight: 600;
      color: #2c3e50;
    }
    .entry-mood {
      font-size: 1.1rem;
    }
    .entry-stats {
      display: flex;
      gap: 20px;
      margin-bottom: 10px;
    }
    .stat {
      color: #7f8c8d;
      font-size: 0.95rem;
    }
    .entry-activities {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin: 10px 0;
    }
    .activity-tag {
      background: #e0e0e0;
      padding: 4px 12px;
      border-radius: 16px;
      font-size: 0.85rem;
      color: #2c3e50;
    }
    .entry-notes {
      color: #7f8c8d;
      font-style: italic;
      margin: 10px 0;
      padding: 10px;
      background: #f8f9fa;
      border-radius: 8px;
    }
    .entry-actions {
      display: flex;
      gap: 10px;
      margin-top: 15px;
    }
    .btn-view, .btn-edit {
      padding: 8px 16px;
      border-radius: 6px;
      text-decoration: none;
      font-size: 0.9rem;
      transition: all 0.3s;
    }
    .btn-view {
      background: #3498db;
      color: white;
    }
    .btn-edit {
      background: #f39c12;
      color: white;
    }
    .btn-view:hover {
      background: #2980b9;
    }
    .btn-edit:hover {
      background: #e67e22;
    }
  `]
})
export class JournalHistoryComponent implements OnInit {
  private static readonly MOOD_EMOJIS = ['😢', '😞', '😐', '🙂', '😊'];

  entries: JournalEntry[] = [];
  loading = false;
  error = '';

  constructor(private journalService: JournalService) {}

  ngOnInit() {
    this.loadEntries();
  }

  /** Safe display when API sends mood outside 1–5 or missing. */
  moodEmoji(mood: number): string {
    const m = Number.isFinite(mood) ? Math.min(5, Math.max(1, Math.floor(mood))) : 3;
    return JournalHistoryComponent.MOOD_EMOJIS[m - 1] ?? '😐';
  }

  hasEntryId(entry: JournalEntry): boolean {
    return entry.id !== undefined && entry.id !== null;
  }

  loadEntries() {
    this.loading = true;
    this.error = '';
    this.journalService.getAllEntries().subscribe({
      next: (data) => {
        const list = Array.isArray(data) ? data : [];
        this.entries = list.map((e) => this.normalizeEntry(e));
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement du journal';
        this.loading = false;
      }
    });
  }

  private normalizeEntry(raw: Partial<JournalEntry>): JournalEntry {
    const date = this.parseEntryDate(raw.date);
    return {
      ...raw,
      id: raw.id,
      date,
      mood: this.clampScore(raw.mood, 3),
      energy: this.clampScore(raw.energy, 3),
      stress: this.clampScore(raw.stress, 3),
      sleepHours: typeof raw.sleepHours === 'number' && !Number.isNaN(raw.sleepHours) ? raw.sleepHours : Number(raw.sleepHours) || 0,
      activities: Array.isArray(raw.activities) ? raw.activities : [],
      notes: raw.notes ?? ''
    };
  }

  private clampScore(v: unknown, fallback: number): number {
    const n = typeof v === 'number' ? v : Number(v);
    if (!Number.isFinite(n)) return fallback;
    return Math.min(5, Math.max(1, Math.round(n)));
  }

  /** Avoid Invalid Date breaking the template date pipe when the API sends bad or missing dates. */
  private parseEntryDate(raw: unknown): Date {
    let d: Date;
    if (raw instanceof Date) {
      d = raw;
    } else if (typeof raw === 'string' || typeof raw === 'number') {
      d = new Date(raw);
    } else {
      return new Date();
    }
    return Number.isNaN(d.getTime()) ? new Date() : d;
  }
}

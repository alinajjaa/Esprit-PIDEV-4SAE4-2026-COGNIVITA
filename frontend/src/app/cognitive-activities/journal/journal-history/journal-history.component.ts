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
              {{ ['😢', '😞', '😐', '🙂', '😊'][entry.mood - 1] }} {{ entry.mood }}/5
            </span>
          </div>

          <div class="entry-stats">
            <span class="stat">⚡ {{ entry.energy }}/5</span>
            <span class="stat">😰 {{ entry.stress }}/5</span>
            <span class="stat">😴 {{ entry.sleepHours }}h</span>
          </div>

          <div class="entry-activities" *ngIf="entry.activities?.length">
            <span *ngFor="let activity of entry.activities" class="activity-tag">
              {{ activity }}
            </span>
          </div>

          <p *ngIf="entry.notes" class="entry-notes">"{{ entry.notes }}"</p>

          <div class="entry-actions">
            <a [routerLink]="['/journal', entry.id]" class="btn-view">Voir</a>
            <a [routerLink]="['/journal/edit', entry.id]" class="btn-edit">Modifier</a>
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
  entries: JournalEntry[] = [];
  loading = false;
  error = '';

  constructor(private journalService: JournalService) {}

  ngOnInit() {
    this.loadEntries();
  }

  loadEntries() {
    this.loading = true;
    this.journalService.getAllEntries().subscribe({
      next: (data) => {
        console.log('✅ Entrées chargées:', data);
        this.entries = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Erreur détaillée:', err);
        this.error = 'Erreur lors du chargement du journal';
        this.loading = false;
      }
    });
  }
}

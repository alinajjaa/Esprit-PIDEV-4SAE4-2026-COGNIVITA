// src/app/cognitive-activities/journal/journal-entry-form/journal-entry-form.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { JournalService } from '../../services/journal.service';
import { JournalEntry } from '../../models/journal-entry.model';

@Component({
  selector: 'app-journal-entry-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './journal-entry-form.component.html',
  styleUrls: ['./journal-entry-form.component.css']
})
export class JournalEntryFormComponent implements OnInit {
  entry: JournalEntry = {
    date: new Date(),
    mood: 3,
    energy: 3,
    stress: 3,
    sleepHours: 7,
    activities: [],
    notes: ''
  };

  isEditMode = false;
  loading = false;
  error = '';

  availableActivities = [
    '🧠 Exercice cognitif',
    '🚶 Marche',
    '📚 Lecture',
    '🧘 Méditation',
    '🎵 Musique',
    '👥 Socialisation',
    '🍳 Cuisine',
    '🎨 Art',
    '🏃 Sport',
    '🌳 Nature'
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private journalService: JournalService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.loadEntry(Number(id));
    } else {
      // Formater la date pour l'input date
      const today = new Date();
      this.entry.date = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    }
  }

  loadEntry(id: number) {
    this.loading = true;
    this.journalService.getEntryById(id).subscribe({
      next: (entry) => {
        this.entry = entry;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur de chargement';
        this.loading = false;
      }
    });
  }

  toggleActivity(activity: string) {
    const index = this.entry.activities.indexOf(activity);
    if (index > -1) {
      this.entry.activities.splice(index, 1);
    } else {
      this.entry.activities.push(activity);
    }
  }

  onSubmit() {
    this.loading = true;

    if (this.isEditMode && this.entry.id) {
      // Mode édition
      this.journalService.updateEntry(this.entry.id, this.entry).subscribe({
        next: () => {
          this.router.navigate(['/journal']);
        },
        error: (err) => {
          this.error = 'Erreur de mise à jour';
          this.loading = false;
        }
      });
    } else {
      // Mode création
      this.journalService.createEntry(this.entry).subscribe({
        next: () => {
          this.router.navigate(['/journal']);
        },
        error: (err) => {
          this.error = 'Erreur de création';
          this.loading = false;
        }
      });
    }
  }

  cancel() {
    this.router.navigate(['/journal']);
  }
}

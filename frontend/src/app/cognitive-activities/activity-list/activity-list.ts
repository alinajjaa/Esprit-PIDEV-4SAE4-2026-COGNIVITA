// src/app/cognitive-activities/activity-list/activity-list.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CognitiveActivityService, CognitiveActivity } from '../services/cognitive-activity.service';

@Component({
  selector: 'app-activity-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './activity-list.html',
  styleUrls: ['./activity-list.css']
})
export class ActivityList implements OnInit {
  // Données
  allActivities: CognitiveActivity[] = [];      // Toutes les activités (cache)
  filteredActivities: CognitiveActivity[] = []; // Activités filtrées (affichées)

  // États
  loading = false;
  error = '';

  // Filtres
  activeTypeFilter = 'ALL';
  activeDifficultyFilter = 'ALL';

  constructor(private activityService: CognitiveActivityService) {}

  ngOnInit() {
    this.loadActivities();
  }

  // ✅ Charge TOUTES les activités une seule fois (instantané après)
  loadActivities() {
    this.loading = true;
    this.error = '';
    this.activityService.getAllActivities().subscribe({
      next: (data) => {
        this.allActivities = data;
        this.applyFilters(); // Applique les filtres par défaut
        this.loading = false;
        console.log('✅ Activities loaded:', data.length);
      },
      error: (err) => {
        this.error = 'Failed to load activities';
        this.loading = false;
        console.error(err);
      }
    });
  }

  // ✅ FILTRAGE INSTANTANÉ (côté frontend)
  filterByType(type: string) {
    console.log('Filtering by type:', type);
    this.activeTypeFilter = type;
    this.applyFilters();
  }

  filterByDifficulty(difficulty: string) {
    console.log('Filtering by difficulty:', difficulty);
    this.activeDifficultyFilter = difficulty;
    this.applyFilters();
  }

  resetFilters() {
    console.log('Resetting all filters');
    this.activeTypeFilter = 'ALL';
    this.activeDifficultyFilter = 'ALL';
    this.applyFilters();
  }

  // ✅ Applique tous les filtres instantanément
  private applyFilters() {
    let result = [...this.allActivities];

    // Filtre par type
    if (this.activeTypeFilter !== 'ALL') {
      result = result.filter(a => a.type === this.activeTypeFilter);
    }

    // Filtre par difficulté
    if (this.activeDifficultyFilter !== 'ALL') {
      result = result.filter(a => a.difficulty === this.activeDifficultyFilter);
    }

    this.filteredActivities = result;
    console.log(`✅ Filters applied: ${result.length} activities shown`);
  }

  deleteActivity(id: number) {
    if (confirm('Are you sure you want to delete this activity?')) {
      this.activityService.deleteActivity(id).subscribe({
        next: () => {
          this.allActivities = this.allActivities.filter(a => a.id !== id);
          this.applyFilters(); // Re-filtrer après suppression
        },
        error: (err) => {
          alert('Failed to delete activity');
          console.error(err);
        }
      });
    }
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      'MEMORY': '🧠',
      'ATTENTION': '👀',
      'LOGIC': '🔢'
    };
    return icons[type] || '📝';
  }

  getDifficultyClass(difficulty: string): string {
    const classes: Record<string, string> = {
      'EASY': 'badge-easy',
      'MEDIUM': 'badge-medium',
      'HARD': 'badge-hard'
    };
    return classes[difficulty] || '';
  }

  // ✅ Pour les statistiques
  getTotalCount(): number {
    return this.allActivities.length;
  }

  getFilteredCount(): number {
    return this.filteredActivities.length;
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CognitiveActivityService, CognitiveActivity } from './services/cognitive-activity.service';
import { DailyChallengeComponent } from './daily-challenge/daily-challenge.component';
import { StreakDisplayComponent } from './streak-display/streak-display.component';

@Component({
  selector: 'app-cognitive-activities',
  standalone: true,
  imports: [CommonModule, RouterModule, DailyChallengeComponent, StreakDisplayComponent],
  templateUrl: './cognitive-activities.html',
  styleUrls: ['./cognitive-activities.css']
})
export class CognitiveActivities implements OnInit {
  activities: CognitiveActivity[] = [];
  filteredActivities: CognitiveActivity[] = [];
  loading = true;
  error = '';

  activityTypes = [
    { value: 'ALL', label: 'Tous', icon: '📋' },
    { value: 'MEMORY', label: 'Mémoire', icon: '🧠' },
    { value: 'ATTENTION', label: 'Attention', icon: '👀' },
    { value: 'LOGIC', label: 'Logique', icon: '🔢' }
  ];

  selectedType: string = 'ALL';
  searchTerm: string = '';

  constructor(private readonly activityService: CognitiveActivityService) {}

  ngOnInit() {
    this.loadActivities();
  }

  loadActivities() {
    this.loading = true;
    this.activityService.getAllActivities().subscribe({
      next: (data) => {
        this.activities = data;
        this.filteredActivities = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des activités';
        this.loading = false;
      }
    });
  }

  filterByType(type: string) {
    this.selectedType = type;
    this.applyFilters();
  }

  onSearch(event: any) {
    this.searchTerm = event.target.value.toLowerCase();
    this.applyFilters();
  }

  applyFilters() {
    this.filteredActivities = this.activities.filter(activity => {
      if (this.selectedType !== 'ALL' && activity.type !== this.selectedType) {
        return false;
      }
      if (this.searchTerm && !activity.title?.toLowerCase().includes(this.searchTerm)) {
        return false;
      }
      return true;
    });
  }

  getDifficultyClass(difficulty: string): string {
    return difficulty?.toLowerCase() || 'easy';
  }

  getDifficultyLabel(difficulty: string): string {
    const labels: Record<string, string> = {
      'EASY': 'Facile',
      'MEDIUM': 'Moyen',
      'HARD': 'Difficile'
    };
    return labels[difficulty] || difficulty;
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      'MEMORY': '🧠',
      'ATTENTION': '👀',
      'LOGIC': '🔢'
    };
    return icons[type] || '📝';
  }

  deleteActivity(id: number) {
    if (confirm('Voulez-vous vraiment supprimer cette activité ?')) {
      this.activityService.deleteActivity(id).subscribe({
        next: () => {
          this.loadActivities();
        },
        error: () => {
          this.error = 'Erreur lors de la suppression';
        }
      });
    }
  }
}

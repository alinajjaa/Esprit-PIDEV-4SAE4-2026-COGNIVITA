// src/app/cognitive-activities/progressive-challenges/progressive-challenges.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChallengeService } from '../services/challenge.service';
import { ProgressiveChallenge, UserProgression } from '../models/challenge.model';

@Component({
  selector: 'app-progressive-challenges',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progressive-challenges.component.html',
  styleUrls: ['./progressive-challenges.component.css']
})
export class ProgressiveChallengesComponent implements OnInit, OnDestroy {
  challenges: ProgressiveChallenge[] = [];
  progression: UserProgression | null = null;
  loading = true;
  activeTab: 'MEMORY' | 'ATTENTION' | 'LOGIC' | 'ALL' = 'ALL';

  constructor(private challengeService: ChallengeService) {}

  ngOnInit() {
    this.loadData();
    window.addEventListener('challenges-updated', this.handleUpdate.bind(this));
  }

  ngOnDestroy() {
    window.removeEventListener('challenges-updated', this.handleUpdate.bind(this));
  }

  handleUpdate() {
    console.log('🔄 Challenges updated event received');
    this.loadData();
  }

  loadData() {
    this.loading = true;

    this.challengeService.getProgressiveChallenges().subscribe({
      next: (challenges) => {
        this.challenges = challenges;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading challenges', err);
        this.loading = false;
      }
    });

    this.challengeService.getProgression().subscribe({
      next: (progression) => {
        this.progression = progression;
      }
    });
  }

  getFilteredChallenges(): ProgressiveChallenge[] {
    if (this.activeTab === 'ALL') {
      return this.challenges;
    }
    return this.challenges.filter(c => c.type === this.activeTab);
  }

  getProgressPercentage(challenge: ProgressiveChallenge): number {
    return (challenge.currentCount / challenge.targetCount) * 100;
  }

  getProgressColor(challenge: ProgressiveChallenge): string {
    if (challenge.completed) return '#48bb78';
    const percentage = this.getProgressPercentage(challenge);
    if (percentage >= 75) return '#fbbf24';
    if (percentage >= 50) return '#4299e1';
    return '#cbd5e0';
  }

  getLevelLabel(level: number): string {
    const labels = ['', 'Débutant', 'Intermédiaire', 'Expert'];
    return labels[level] || '';
  }

  setActiveTab(tab: 'MEMORY' | 'ATTENTION' | 'LOGIC' | 'ALL') {
    this.activeTab = tab;
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      'MEMORY': '🧠',
      'ATTENTION': '👀',
      'LOGIC': '🔢',
      'ALL': '🏆'
    };
    return icons[type] || '📝';
  }
}

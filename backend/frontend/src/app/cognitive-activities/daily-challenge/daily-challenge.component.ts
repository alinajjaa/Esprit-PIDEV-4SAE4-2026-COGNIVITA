// src/app/cognitive-activities/daily-challenge/daily-challenge.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ChallengeService } from '../services/challenge.service';
import { ProgressiveChallenge, UserProgression } from '../models/challenge.model';

@Component({
  selector: 'app-daily-challenge',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './daily-challenge.component.html',
  styleUrls: ['./daily-challenge.component.css']
})
export class DailyChallengeComponent implements OnInit, OnDestroy {
  challenges: ProgressiveChallenge[] = [];
  progression: UserProgression | null = null;
  loading = true;

  constructor(private challengeService: ChallengeService) {}

  ngOnInit() {
    this.loadData();
    window.addEventListener('challenges-updated', this.handleChallengesUpdate.bind(this));
    console.log('✅ DailyChallengeComponent initialisé avec écouteur');
  }

  handleChallengesUpdate() {
    console.log('🔄 Challenges updated event received');
    this.loadData();
  }

  loadData() {
    this.loading = true;

    this.challengeService.getProgressiveChallenges().subscribe({
      next: (challenges) => {
        this.challenges = challenges;
        this.loading = false;
        console.log('✅ Défis chargés:', challenges.length);
      },
      error: (err) => {
        console.error('❌ Error loading challenges', err);
        this.loading = false;
      }
    });

    this.challengeService.getProgression().subscribe({
      next: (progression) => {
        this.progression = progression;
        console.log('✅ Progression chargée:', progression);
      },
      error: (err) => {
        console.error('❌ Error loading progression', err);
      }
    });
  }

  getPendingChallenges(): ProgressiveChallenge[] {
    return this.challenges.filter(c => !c.completed).slice(0, 3);
  }

  getRecentCompletedChallenges(): ProgressiveChallenge[] {
    return this.challenges.filter(c => c.completed).slice(0, 2);
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

  getChallengeIcon(type: string): string {
    const icons: Record<string, string> = {
      'MEMORY': '🧠',
      'ATTENTION': '👀',
      'LOGIC': '🔢',
      'ALL': '🏆'
    };
    return icons[type] || '🎯';
  }

  ngOnDestroy() {
    window.removeEventListener('challenges-updated', this.handleChallengesUpdate.bind(this));
    console.log('❌ DailyChallengeComponent nettoyé');
  }
}

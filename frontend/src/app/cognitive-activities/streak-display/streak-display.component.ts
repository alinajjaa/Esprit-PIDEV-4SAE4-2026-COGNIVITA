// src/app/cognitive-activities/streak-display/streak-display.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChallengeService } from '../services/challenge.service';
import { UserProgression, ProgressiveChallenge } from '../models/challenge.model';

@Component({
  selector: 'app-streak-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './streak-display.component.html',
  styleUrls: ['./streak-display.component.css']
})
export class StreakDisplayComponent implements OnInit, OnDestroy {
  progression: UserProgression | null = null;
  challenges: ProgressiveChallenge[] = [];
  loading = true;

  constructor(private challengeService: ChallengeService) {}

  ngOnInit() {
    this.loadData();
    window.addEventListener('challenges-updated', this.handleUpdate.bind(this));
  }

  ngOnDestroy() {
    window.removeEventListener('challenges-updated', this.handleUpdate.bind(this));
  }

  handleUpdate() {
    this.loadData();
  }

  loadData() {
    this.loading = true;

    this.challengeService.getProgression().subscribe({
      next: (progression) => {
        this.progression = progression;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading progression', err);
        this.loading = false;
      }
    });

    this.challengeService.getProgressiveChallenges().subscribe({
      next: (challenges) => {
        this.challenges = challenges;
      },
      error: (err) => {
        console.error('Error loading challenges', err);
      }
    });
  }

  getStreakMessage(): string {
    if (!this.progression) return '';

    const total = this.progression.totalCount;

    if (total === 0) {
      return 'Commence ta progression aujourd\'hui !';
    } else if (total < 5) {
      return `🔥 ${total} exercice${total > 1 ? 's' : ''} ! Bon début !`;
    } else if (total < 15) {
      return `⚡ ${total} exercices ! Continue comme ça !`;
    } else if (total < 30) {
      return `🌟 ${total} exercices ! Impressionnant !`;
    } else {
      return `🏆 ${total} exercices ! Tu es une légende !`;
    }
  }

  getStreakColor(): string {
    if (!this.progression) return '#718096';
    const total = this.progression.totalCount;
    if (total >= 30) return '#fbbf24';
    if (total >= 15) return '#a0aec0';
    if (total >= 5) return '#4299e1';
    return '#cbd5e0';
  }

  getRecentBadges(): string[] {
    return this.progression?.badges.slice(-3) || [];
  }

  getCompletedChallengesCount(): number {
    return this.challenges.filter(c => c.completed).length;
  }
}

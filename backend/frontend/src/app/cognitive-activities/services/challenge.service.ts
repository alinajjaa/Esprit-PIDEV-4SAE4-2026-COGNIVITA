// src/app/cognitive-activities/services/challenge.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ProgressiveChallenge, UserProgression, Badge } from '../models/challenge.model';

@Injectable({
  providedIn: 'root'
})
export class ChallengeService {
  private readonly PROGRESSION_KEY = 'cognitive_progression';
  private readonly CHALLENGES_KEY = 'cognitive_progressive_challenges';

  constructor() { }

  // ==================== DÉFIS PROGRESSIFS ====================

  // Initialiser ou récupérer les défis
  getProgressiveChallenges(): Observable<ProgressiveChallenge[]> {
    const challenges = this.loadChallenges();
    return of(challenges);
  }

  private loadChallenges(): ProgressiveChallenge[] {
    const saved = localStorage.getItem(this.CHALLENGES_KEY);
    if (saved) {
      return JSON.parse(saved);
    }

    // Défis progressifs par paliers
    const initialChallenges: ProgressiveChallenge[] = [
      // Niveau 1 - Mémoire
      {
        id: 'memory_1',
        title: '🧠 Apprenti mémoire',
        description: 'Fais 5 exercices de mémoire',
        icon: '🧠',
        targetCount: 5,
        currentCount: 0,
        rewardPoints: 50,
        rewardBadge: '🧠 Bronze',
        completed: false,
        type: 'MEMORY',
        level: 1
      },
      {
        id: 'memory_2',
        title: '🧠 Expert mémoire',
        description: 'Fais 15 exercices de mémoire',
        icon: '🧠',
        targetCount: 15,
        currentCount: 0,
        rewardPoints: 150,
        rewardBadge: '🧠 Argent',
        completed: false,
        type: 'MEMORY',
        level: 2
      },
      {
        id: 'memory_3',
        title: '🧠 Maître mémoire',
        description: 'Fais 30 exercices de mémoire',
        icon: '🧠',
        targetCount: 30,
        currentCount: 0,
        rewardPoints: 300,
        rewardBadge: '🧠 Or',
        completed: false,
        type: 'MEMORY',
        level: 3
      },

      // Niveau 1 - Attention
      {
        id: 'attention_1',
        title: '👀 Apprenti attention',
        description: 'Fais 5 exercices d\'attention',
        icon: '👀',
        targetCount: 5,
        currentCount: 0,
        rewardPoints: 50,
        rewardBadge: '👀 Bronze',
        completed: false,
        type: 'ATTENTION',
        level: 1
      },
      {
        id: 'attention_2',
        title: '👀 Expert attention',
        description: 'Fais 15 exercices d\'attention',
        icon: '👀',
        targetCount: 15,
        currentCount: 0,
        rewardPoints: 150,
        rewardBadge: '👀 Argent',
        completed: false,
        type: 'ATTENTION',
        level: 2
      },
      {
        id: 'attention_3',
        title: '👀 Maître attention',
        description: 'Fais 30 exercices d\'attention',
        icon: '👀',
        targetCount: 30,
        currentCount: 0,
        rewardPoints: 300,
        rewardBadge: '👀 Or',
        completed: false,
        type: 'ATTENTION',
        level: 3
      },

      // Niveau 1 - Logique
      {
        id: 'logic_1',
        title: '🔢 Apprenti logique',
        description: 'Fais 5 exercices de logique',
        icon: '🔢',
        targetCount: 5,
        currentCount: 0,
        rewardPoints: 50,
        rewardBadge: '🔢 Bronze',
        completed: false,
        type: 'LOGIC',
        level: 1
      },
      {
        id: 'logic_2',
        title: '🔢 Expert logique',
        description: 'Fais 15 exercices de logique',
        icon: '🔢',
        targetCount: 15,
        currentCount: 0,
        rewardPoints: 150,
        rewardBadge: '🔢 Argent',
        completed: false,
        type: 'LOGIC',
        level: 2
      },
      {
        id: 'logic_3',
        title: '🔢 Maître logique',
        description: 'Fais 30 exercices de logique',
        icon: '🔢',
        targetCount: 30,
        currentCount: 0,
        rewardPoints: 300,
        rewardBadge: '🔢 Or',
        completed: false,
        type: 'LOGIC',
        level: 3
      },

      // Défis généraux
      {
        id: 'total_1',
        title: '🏆 Débutant',
        description: 'Fais 10 exercices au total',
        icon: '🏆',
        targetCount: 10,
        currentCount: 0,
        rewardPoints: 100,
        rewardBadge: '🏆 Bronze',
        completed: false,
        type: 'ALL',
        level: 1
      },
      {
        id: 'total_2',
        title: '🏆 Confirmé',
        description: 'Fais 25 exercices au total',
        icon: '🏆',
        targetCount: 25,
        currentCount: 0,
        rewardPoints: 250,
        rewardBadge: '🏆 Argent',
        completed: false,
        type: 'ALL',
        level: 2
      },
      {
        id: 'total_3',
        title: '🏆 Champion',
        description: 'Fais 50 exercices au total',
        icon: '🏆',
        targetCount: 50,
        currentCount: 0,
        rewardPoints: 500,
        rewardBadge: '🏆 Or',
        completed: false,
        type: 'ALL',
        level: 3
      }
    ];

    localStorage.setItem(this.CHALLENGES_KEY, JSON.stringify(initialChallenges));
    return initialChallenges;
  }

  // Mettre à jour la progression après un exercice
  updateProgression(type: 'MEMORY' | 'ATTENTION' | 'LOGIC'): void {
    const progression = this.loadProgression();

    // Mettre à jour les compteurs
    if (type === 'MEMORY') progression.memoryCount++;
    if (type === 'ATTENTION') progression.attentionCount++;
    if (type === 'LOGIC') progression.logicCount++;
    progression.totalCount++;

    this.saveProgression(progression);

    // Vérifier les défis à compléter
    this.checkChallengesCompletion(type, progression);
  }

  private checkChallengesCompletion(type: 'MEMORY' | 'ATTENTION' | 'LOGIC', progression: UserProgression): void {
    const challenges = this.loadChallenges();
    let updated = false;

    challenges.forEach(challenge => {
      if (!challenge.completed) {
        let current = 0;

        if (challenge.type === 'ALL') {
          current = progression.totalCount;
        } else if (challenge.type === type) {
          if (type === 'MEMORY') current = progression.memoryCount;
          if (type === 'ATTENTION') current = progression.attentionCount;
          if (type === 'LOGIC') current = progression.logicCount;
        }

        // Mettre à jour le compteur actuel
        challenge.currentCount = current;

        // Si le palier est atteint
        if (current >= challenge.targetCount && !challenge.completed) {
          challenge.completed = true;
          challenge.completedAt = new Date();

          // Ajouter les points et le badge
          progression.totalPoints += challenge.rewardPoints;
          if (challenge.rewardBadge) {
            progression.badges.push(challenge.rewardBadge);
          }
          progression.completedChallenges.push(challenge.id);

          updated = true;
          console.log(`✅ Défi complété : ${challenge.title} +${challenge.rewardPoints} points`);
        }
      }
    });

    if (updated) {
      this.saveChallenges(challenges);
      this.saveProgression(progression);
      // Déclencher une mise à jour de l'affichage
      window.dispatchEvent(new CustomEvent('challenges-updated'));
    }
  }

  private loadProgression(): UserProgression {
    const saved = localStorage.getItem(this.PROGRESSION_KEY);
    if (saved) {
      return JSON.parse(saved);
    }

    return {
      memoryCount: 0,
      attentionCount: 0,
      logicCount: 0,
      totalCount: 0,
      totalPoints: 0,
      badges: [],
      completedChallenges: []
    };
  }

  private saveProgression(progression: UserProgression): void {
    localStorage.setItem(this.PROGRESSION_KEY, JSON.stringify(progression));
  }

  private saveChallenges(challenges: ProgressiveChallenge[]): void {
    localStorage.setItem(this.CHALLENGES_KEY, JSON.stringify(challenges));
  }

  getProgression(): Observable<UserProgression> {
    return of(this.loadProgression());
  }

  resetAllData(): void {
    localStorage.removeItem(this.PROGRESSION_KEY);
    localStorage.removeItem(this.CHALLENGES_KEY);
  }
}

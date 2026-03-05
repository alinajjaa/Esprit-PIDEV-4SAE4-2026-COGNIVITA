// src/app/cognitive-activities/models/challenge.model.ts
export interface ProgressiveChallenge {
  id: string;
  title: string;
  description: string;
  icon: string;
  targetCount: number;
  currentCount: number;
  rewardPoints: number;
  rewardBadge?: string;
  completed: boolean;
  completedAt?: Date;
  type: 'MEMORY' | 'ATTENTION' | 'LOGIC' | 'ALL';
  level: number;
}

export interface UserProgression {
  memoryCount: number;
  attentionCount: number;
  logicCount: number;
  totalCount: number;
  totalPoints: number;
  badges: string[];
  completedChallenges: string[];
}

export interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string;
  unlockedAt?: Date;
}

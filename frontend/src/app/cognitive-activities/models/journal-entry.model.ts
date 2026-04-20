// src/app/cognitive-activities/models/journal-entry.model.ts
export interface JournalEntry {
  id?: number;
  date: Date;
  mood: number;  // 1-5
  energy: number; // 1-5
  stress: number; // 1-5
  sleepHours: number;
  activities: string[];
  notes: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface MoodStats {
  averageMood: number;
  averageEnergy: number;
  averageStress: number;
  averageSleep: number;
  entriesCount: number;
  streak: number;
}

// src/app/cognitive-activities/activity-form/activity-form.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CognitiveActivityService } from '../services/cognitive-activity.service';

@Component({
  selector: 'app-activity-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './activity-form.html',
  styleUrls: ['./activity-form.css']
})
export class ActivityForm implements OnInit {
  isEditMode = false;
  activityId: number | null = null;

  activity: any = {
    title: '',
    description: '',
    type: 'MEMORY',
    difficulty: 'EASY',
    maxScore: 10,
    timeLimit: 120,
    instructions: '',
    isActive: true,
    stroopWord: '',
    stroopColor: 'red',
    stroopCorrect: 'red',
    sequenceAnswer: null
  };

  // Separate boolean bound to the checkbox to avoid ngModel assignment to nested activity property
  isActive: boolean = true;

  words: string[] = [''];
  sequence: number[] = [2, 4, 6];
  generatedContent: any = null;

  loading = false;
  submitting = false;
  error = '';
  success = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly activityService: CognitiveActivityService
  ) {}

  // Helper to safely parse JSON without scattering try/catch blocks
  private safeParseJSON(jsonStr: string | null): any {
    if (!jsonStr) return null;
    try {
      return JSON.parse(jsonStr);
    } catch (e) {
      console.warn('Invalid JSON content:', e);
      return null;
    }
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.activityId = Number(id);
      this.loadActivity(this.activityId);
    } else {
      // Mode création - initialiser avec des valeurs par défaut
      this.isActive = this.activity.isActive;
      this.generateContentPreview();
    }
  }

  loadActivity(id: number) {
    this.loading = true;
    this.activityService.getActivityById(id).subscribe({
      next: (data: any) => {
        this.activity = data;

        // Initialiser les champs spécifiques selon le type
        if (this.activity.type === 'MEMORY') {
          // ✅ Récupérer les mots depuis activity.words
          if (this.activity.words?.length) {
            this.words = [...this.activity.words];
          } else {
            const content = this.safeParseJSON(this.activity.content);
            if (content?.words) {
              this.words = content.words;
            } else {
              this.words = [''];
            }
          }
        }

        if (this.activity.type === 'LOGIC') {
          const content = this.safeParseJSON(this.activity.content);
          if (content?.sequences?.[0]) {
            this.sequence = content.sequences[0];
            this.activity.sequenceAnswer = content.answer ?? this.activity.sequenceAnswer;
          } else {
            this.sequence = [2, 4, 6];
          }
        }

        if (this.activity.type === 'ATTENTION') {
          const content = this.safeParseJSON(this.activity.content);
          if (content?.items?.[0]) {
            this.activity.stroopWord = content.items[0].word ?? this.activity.stroopWord;
            this.activity.stroopColor = content.items[0].color ?? this.activity.stroopColor;
            this.activity.stroopCorrect = content.items[0].correct ?? this.activity.stroopCorrect;
          }
        }

        this.generateContentPreview();
        this.loading = false;
        // sync isActive
        this.isActive = this.activity.isActive ?? true;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement';
        this.loading = false;
        console.error(err);
      }
    });
  }

  // ✅ Méthode pour vérifier si des mots valides existent
  hasValidWords(): boolean {
    return !!(this.words?.some(w => !!w?.trim()));
  }

  // ✅ Méthode pour obtenir les mots valides sous forme de chaîne
  getValidWordsString(): string {
    return this.words?.filter(w => !!w?.trim()).join(', ') ?? '';
  }

  // ✅ Méthode pour vérifier si une séquence valide existe
  hasValidSequence(): boolean {
    return !!(this.sequence?.some(n => n !== null && n !== undefined));
  }

  // ✅ Méthode pour obtenir la séquence valide sous forme de chaîne
  getValidSequenceString(): string {
    if (!this.sequence) return '';
    return this.sequence
      .filter(n => n !== null && n !== undefined)
      .map(n => n.toString())
      .join(' → ');
  }

  // ✅ MÉTHODE POUR METTRE À JOUR LES MOTS
  updateActivityWords() {
    console.log('Mise à jour des mots:', this.words);
    this.generateContentPreview();
  }

  // ✅ MÉTHODE POUR METTRE À JOUR LA SÉQUENCE
  updateSequenceAnswer() {
    console.log('Mise à jour de la séquence:', this.sequence);
    this.generateContentPreview();
  }

  // ✅ MÉTHODE POUR GÉNÉRER L'APERÇU DU CONTENU
  generateContentPreview() {
    if (this.activity.type === 'MEMORY') {
      const validWords = this.words.filter(w => w?.trim());
      if (validWords.length > 0) {
        this.generatedContent = {
          words: validWords
        };
      } else {
        this.generatedContent = null;
      }
    }
    else if (this.activity.type === 'ATTENTION') {
      if (this.activity.stroopWord && this.activity.stroopColor && this.activity.stroopCorrect) {
        this.generatedContent = {
          items: [{
            word: this.activity.stroopWord,
            color: this.activity.stroopColor,
            correct: this.activity.stroopCorrect
          }]
        };
      } else {
        this.generatedContent = null;
      }
    }
    else if (this.activity.type === 'LOGIC') {
      const validSequence = this.sequence.filter(n => n !== null && n !== undefined);
      if (validSequence.length > 0 && this.activity.sequenceAnswer != null) {
        this.generatedContent = {
          sequences: [validSequence],
          answer: this.activity.sequenceAnswer
        };
      } else {
        this.generatedContent = null;
      }
    } else {
      this.generatedContent = null;
    }
  }

  onTypeChange() {
    this.activity.stroopWord = '';
    this.activity.stroopColor = 'red';
    this.activity.stroopCorrect = 'red';
    this.activity.sequenceAnswer = null;

    if (this.activity.type === 'MEMORY') {
      this.words = [''];
    } else if (this.activity.type === 'LOGIC') {
      this.sequence = [2, 4, 6];
    }

    this.generateContentPreview();
  }

  addWord() {
    this.words = [...this.words, ''];
    this.generateContentPreview();
  }

  removeWord(index: number) {
    if (this.words.length > 1) {
      this.words = this.words.filter((_, i) => i !== index);
      this.generateContentPreview();
    }
  }

  addNumber() {
    this.sequence = [...this.sequence, 0];
    this.generateContentPreview();
  }

  removeNumber(index: number) {
    if (this.sequence.length > 2) {
      this.sequence = this.sequence.filter((_, i) => i !== index);
      this.generateContentPreview();
    }
  }

  // trackBy function used by *ngFor in the template to improve rendering
  trackByIndex(index: number, _item: any): number {
    return index;
  }

  isValid(): boolean {
    if (!this.activity.title?.trim()) return false;

    if (this.activity.type === 'MEMORY') {
      return (this.words?.filter(w => !!w?.trim()).length ?? 0) >= 3;
    }
    if (this.activity.type === 'ATTENTION') {
      return !!(this.activity.stroopWord && this.activity.stroopColor && this.activity.stroopCorrect);
    }
    if (this.activity.type === 'LOGIC') {
      return (this.sequence?.filter(n => n !== null).length ?? 0) >= 3 && this.activity.sequenceAnswer != null;
    }
    return false;
  }

  onSubmit() {
    if (!this.isValid()) {
      this.error = 'Veuillez remplir tous les champs requis';
      return;
    }

    // ✅ Préparer l'objet à sauvegarder
    const activityToSave: any = {
      title: this.activity.title,
      description: this.activity.description,
      type: this.activity.type,
      difficulty: this.activity.difficulty,
      maxScore: this.activity.maxScore,
      timeLimit: this.activity.timeLimit,
      instructions: this.activity.instructions,
      isActive: this.activity.isActive
    };

    // ✅ Ajouter les champs spécifiques selon le type
    if (this.activity.type === 'MEMORY') {
      activityToSave.words = this.words.filter(w => w?.trim());
      // Générer le contenu JSON pour le champ content
      activityToSave.content = JSON.stringify({
        words: this.words.filter(w => w?.trim())
      });
    }
    else if (this.activity.type === 'ATTENTION') {
      activityToSave.stroopWord = this.activity.stroopWord;
      activityToSave.stroopColor = this.activity.stroopColor;
      activityToSave.stroopCorrect = this.activity.stroopCorrect;
      // Générer le contenu JSON pour le champ content
      activityToSave.content = JSON.stringify({
        items: [{
          word: this.activity.stroopWord,
          color: this.activity.stroopColor,
          correct: this.activity.stroopCorrect
        }]
      });
    }
    else if (this.activity.type === 'LOGIC') {
      const validSequence = this.sequence.filter(n => n !== null);
      activityToSave.sequence = validSequence;
      activityToSave.sequenceAnswer = this.activity.sequenceAnswer;
      // Générer le contenu JSON pour le champ content
      activityToSave.content = JSON.stringify({
        sequences: [validSequence],
        answer: this.activity.sequenceAnswer
      });
    }

    this.submitting = true;
    // sync checkbox value
    activityToSave.isActive = this.isActive;

    const request = this.isEditMode && this.activityId
      ? this.activityService.updateActivity(this.activityId, activityToSave)
      : this.activityService.createActivity(activityToSave);

    request.subscribe({
      next: (created: any) => {
        this.success = 'Activité ' + (this.isEditMode ? 'mise à jour' : 'créée') + ' avec succès !';
        this.submitting = false;
        setTimeout(() => {
          if (created && created.id) {
            this.router.navigate(['/activities', created.id]);
          } else {
            this.router.navigate(['/activities']);
          }
        }, 1500);
      },
      error: (err) => {
        this.error = 'Erreur lors de la ' + (this.isEditMode ? 'mise à jour' : 'création');
        this.submitting = false;
        console.error(err);
      }
    });
  }

  cancel() {
    if (this.isEditMode && this.activityId) {
      this.router.navigate(['/activities', this.activityId]);
    } else {
      this.router.navigate(['/activities']);
    }
  }
}

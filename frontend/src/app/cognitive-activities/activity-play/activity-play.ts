// src/app/cognitive-activities/activity-play/activity-play.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CognitiveActivityService, CognitiveActivity } from '../services/cognitive-activity.service';
import { ChallengeService } from '../services/challenge.service';

interface GameState {
  activityId: number;
  currentPhase: 'MEMORIZE' | 'RECALL' | 'QUESTION' | 'COMPLETED' | 'ABANDONED';
  currentQuestionIndex: number;
  score: number;
  timeLeft: number;
  totalQuestions: number;
  answers: any[];
  startTime: Date;
  timerActive: boolean;
}

@Component({
  selector: 'app-activity-play',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './activity-play.html',
  styleUrls: ['./activity-play.css']
})
export class ActivityPlay implements OnInit, OnDestroy {
  activity: CognitiveActivity | null = null;
  loading = true;
  error = '';

  // Game state
  gameState: GameState = {
    activityId: 0,
    currentPhase: 'MEMORIZE',
    currentQuestionIndex: 0,
    score: 0,
    timeLeft: 30,
    totalQuestions: 3,
    answers: [],
    startTime: new Date(),
    timerActive: true
  };

  // Game content
  memoryWords: string[] = [];
  memoryGrid: string[][] = [];
  currentQuestion: any = null;
  questions: any[] = [];

  // User inputs
  userAnswer = '';
  selectedOption: number | null = null;
  sequenceAnswer = '';
  // gridAnswer was unused — removed to avoid unused-field warnings

  // Timer
  private timerInterval: any;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly activityService: CognitiveActivityService,
    private readonly challengeService: ChallengeService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadActivity(id);
  }

  ngOnDestroy() {
    this.stopTimer();
  }

  private stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.gameState.timerActive = false;
  }

  private startTimer() {
    if (!this.gameState.timerActive ||
      this.gameState.currentPhase === 'COMPLETED' ||
      this.gameState.currentPhase === 'ABANDONED') {
      return;
    }

    this.stopTimer();

    this.timerInterval = setInterval(() => {
      if (!this.gameState.timerActive ||
        this.gameState.currentPhase === 'COMPLETED' ||
        this.gameState.currentPhase === 'ABANDONED') {
        this.stopTimer();
        return;
      }

      this.gameState.timeLeft--;

      if (this.gameState.timeLeft <= 0) {
        this.handleTimeOut();
      }
    }, 1000);
  }

  loadActivity(id: number) {
    this.loading = true;
    this.activityService.getActivityById(id).subscribe({
      next: (data) => {
        this.activity = data;
        console.log('📦 Activity loaded:', this.activity);
        console.log('📝 Words from database:', this.activity?.words);
        this.loading = false;
        this.initializeGame();
      },
      error: (err) => {
        this.error = 'Failed to load activity';
        this.loading = false;
        console.error('Error loading activity:', err);
      }
    });
  }

  initializeGame() {
    if (!this.activity) return;

    // Reset game state
    this.gameState = {
      activityId: this.activity.id || 0,
      currentPhase: 'MEMORIZE',
      currentQuestionIndex: 0,
      score: 0,
      timeLeft: 30,
      totalQuestions: 3,
      answers: [],
      startTime: new Date(),
      timerActive: true
    };

    try {
      const content = JSON.parse(this.activity.content || '{}');
      console.log('Game content:', content);

      // Clear previous game data
      this.memoryWords = [];
      this.memoryGrid = [];
      this.questions = [];

      switch (this.activity.type) {
        case 'MEMORY':
          this.initializeMemoryGame(content);
          break;
        case 'ATTENTION':
          this.initializeAttentionGame(content);
          break;
        case 'LOGIC':
          this.initializeLogicGame(content);
          break;
        default:
          this.error = 'Unknown game type';
      }

      this.startTimer();
    } catch (e) {
      console.error('Error parsing content:', e);
      this.error = 'Invalid game content';
    }
  }

  // 🧠 MEMORY GAME - CORRIGÉ !
  initializeMemoryGame(content: any) {
    console.log('Initializing MEMORY game');

    // Reset memory-specific structures
    this.memoryGrid = [];
    this.questions = [];

    // ✅ PRIORITÉ 1 : Utiliser les mots de l'activité (de la BD)
    if (this.activity?.words && this.activity.words.length > 0) {
      console.log('✅ Using words from activity (database):', this.activity.words);
      // copy to avoid accidental mutation of the original array
      this.memoryWords = Array.isArray(this.activity.words) ? [...this.activity.words] : [];
    }
    // ✅ PRIORITÉ 2 : Utiliser content.words (si existant)
    else if (content.words) {
      console.log('✅ Using words from content:', content.words);
      this.memoryWords = Array.isArray(content.words) ? [...content.words] : [];
    }
    // ✅ PRIORITÉ 3 : Paires (pays/capitale etc.)
    else if (content.pairs) {
      console.log('✅ Using pairs from content');
      this.questions = content.pairs;
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 180;
      this.currentQuestion = this.questions[0];
      return;
    }
    // ✅ PRIORITÉ 4 : Grille d'emojis
    else if (content.grid) {
      console.log('✅ Using grid from content');
      this.memoryGrid = content.grid;
      this.memoryWords = content.grid.flat();
      this.gameState.totalQuestions = 3;
      this.gameState.currentPhase = 'MEMORIZE';
      this.gameState.timeLeft = 45;

      this.questions = [
        { type: 'grid', question: 'What is in top-left corner?', row: 0, col: 0, answer: this.memoryGrid[0][0] },
        { type: 'grid', question: 'What is in the center?', row: 1, col: 1, answer: this.memoryGrid[1][1] },
        { type: 'grid', question: 'What is in bottom-right corner?', row: 2, col: 2, answer: this.memoryGrid[2][2] }
      ];
      return;
    }
    else {
      // ⚠️ Si vraiment pas de mots, afficher une erreur
      console.error('❌ No words found for memory game!');
      this.error = 'Cette activité de mémoire n\'a pas de mots configurés';
      return;
    }

    // Configuration par défaut pour les listes de mots simples
    // Single recall question that asks the user to list the words
    this.gameState.totalQuestions = 1;
    this.gameState.currentPhase = 'MEMORIZE';
    this.gameState.timeLeft = this.activity?.timeLimit || 30;

    console.log('✅ Memory game initialized with words:', this.memoryWords);
  }

  // 👀 ATTENTION GAME
  initializeAttentionGame(content: any) {
    console.log('Initializing ATTENTION game', content);

    if (content.items) {
      this.questions = content.items;
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 150;
      this.currentQuestion = this.questions[0];

      console.log('✅ Questions loaded:', this.questions);
      console.log('✅ First question:', this.questions[0]);
    }
    else if (content.sequences) {
      this.questions = content.sequences.map((seq: any) => ({
        type: 'reverse',
        sequence: seq,
        correct: [...seq].reverse()
      }));
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 240;
      this.currentQuestion = this.questions[0];
    }
    else {
      // Default attention game
      this.questions = [
        { word: 'ROUGE', color: 'blue', correct: 'blue' },
        { word: 'VERT', color: 'red', correct: 'red' },
        { word: 'BLEU', color: 'green', correct: 'green' }
      ];
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = 150;
      this.currentQuestion = this.questions[0];
    }
  }

  // 🔢 LOGIC GAME
  initializeLogicGame(content: any) {
    console.log('Initializing LOGIC game');

    if (content.sequences) {
      this.questions = content.sequences.map((seq: any) => {
        if (Array.isArray(seq)) {
          return {
            type: 'sequence',
            sequence: seq.slice(0, -1),
            correct: seq[seq.length - 1]
          };
        }
        return seq;
      });
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 180;
      this.currentQuestion = this.questions[0];
    }
    else if (content.problems) {
      this.questions = content.problems;
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 300;
      this.currentQuestion = this.questions[0];
    }
    else if (content.riddles) {
      this.questions = content.riddles;
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = this.activity?.timeLimit || 240;
      this.currentQuestion = this.questions[0];
    }
    else {
      // Default logic game
      this.questions = [
        { type: 'sequence', sequence: [2, 4, 6, 8], correct: 10 },
        { type: 'sequence', sequence: [5, 10, 15, 20], correct: 25 }
      ];
      this.gameState.totalQuestions = this.questions.length;
      this.gameState.currentPhase = 'QUESTION';
      this.gameState.timeLeft = 180;
      this.currentQuestion = this.questions[0];
    }
  }

  handleTimeOut() {
    console.log('Time out');
    this.stopTimer();

    if (this.gameState.currentPhase === 'MEMORIZE') {
      this.gameState.currentPhase = 'RECALL';
      this.gameState.timeLeft = this.activity?.timeLimit || 120;
      this.gameState.timerActive = true;
      this.startTimer();
    } else {
      this.moveToNextQuestion();
    }
  }

  startRecall() {
    console.log('Starting recall phase');
    this.stopTimer();
    this.gameState.currentPhase = 'RECALL';
    this.gameState.timeLeft = this.activity?.timeLimit || 120;
    this.gameState.currentQuestionIndex = 0;
    this.currentQuestion = this.questions[0];
    this.gameState.timerActive = true;
    this.startTimer();
  }

  submitAnswer() {
    if (!this.activity) return;

    console.log('Submitting answer for:', this.activity.type);

    switch (this.activity.type) {
      case 'MEMORY':
        this.submitMemoryAnswer();
        break;
      case 'ATTENTION':
        this.submitAttentionAnswer();
        break;
      case 'LOGIC':
        this.submitLogicAnswer();
        break;
    }
  }

  submitMemoryAnswer() {
    // Si c'est une grille
    if (this.memoryGrid.length > 0 && this.currentQuestion?.type === 'grid') {
      this.submitGridAnswer();
      return;
    }

    // Sinon c'est une liste de mots
    if (!this.memoryWords || this.memoryWords.length === 0) {
      console.error('submitMemoryAnswer called but no memory words are configured');
      this.error = 'Aucun mot disponible pour cette activité de mémorisation.';
      return;
    }

    const userWords = (this.userAnswer || '').toLowerCase().split(',').map(w => w.trim()).filter(w => w.length > 0);
    const correctWordsSet = new Set(this.memoryWords.map(w => w.toString().toLowerCase()));

    const correctCount = userWords.filter(w => correctWordsSet.has(w)).length;
    const pointsEarned = Math.floor((correctCount / this.memoryWords.length) * 10) || 0;
    const isCorrect = correctCount >= Math.ceil(this.memoryWords.length * 0.5);

    this.gameState.answers.push({
      type: 'memory',
      userAnswer: this.userAnswer,
      correctCount: correctCount,
      totalWords: this.memoryWords.length,
      pointsEarned: pointsEarned,
      correct: isCorrect
    });

    this.gameState.score += pointsEarned;
    console.log('Memory score:', this.gameState.score);
    this.completeGame();
  }

  submitGridAnswer() {
    const isCorrect = this.userAnswer === this.currentQuestion.answer;
    const pointsEarned = isCorrect ? 10 : 0;

    this.gameState.answers.push({
      question: this.gameState.currentQuestionIndex,
      userAnswer: this.userAnswer,
      correct: isCorrect,
      pointsEarned: pointsEarned,
      correctAnswer: this.currentQuestion.answer
    });

    if (isCorrect) {
      this.gameState.score += pointsEarned;
    }

    this.moveToNextQuestion();
  }

  // ✅ ATTENTION ANSWER
  submitAttentionAnswer() {
    if (!this.currentQuestion) {
      console.error('No current question');
      return;
    }

    console.log('=== ATTENTION ANSWER DEBUG ===');
    console.log('Current question:', this.currentQuestion);
    console.log('Selected option (0=Rouge, 1=Bleu, 2=Vert):', this.selectedOption);

    // ✅ Déterminer la couleur sélectionnée en FRANÇAIS
    let selectedColorFrench = '';
    let selectedColorEnglish = '';

    if (this.selectedOption === 0) {
      selectedColorFrench = 'rouge';
      selectedColorEnglish = 'red';
    } else if (this.selectedOption === 1) {
      selectedColorFrench = 'bleu';
      selectedColorEnglish = 'blue';
    } else if (this.selectedOption === 2) {
      selectedColorFrench = 'vert';
      selectedColorEnglish = 'green';
    } else {
      console.warn('No option selected');
      return;
    }

    // ✅ La bonne réponse est en anglais dans les données
    const correctAnswerEnglish = this.currentQuestion.correct || this.currentQuestion.color;

    // ✅ Comparer avec la version anglaise
    const isCorrect = selectedColorEnglish === correctAnswerEnglish;
    const pointsEarned = isCorrect ? 10 : 0;

    console.log('Selected (French):', selectedColorFrench);
    console.log('Selected (English):', selectedColorEnglish);
    console.log('Correct answer (English):', correctAnswerEnglish);
    console.log('Is correct:', isCorrect);
    console.log('Points earned:', pointsEarned);

    // Sauvegarder la réponse
    this.gameState.answers.push({
      questionIndex: this.gameState.currentQuestionIndex,
      word: this.currentQuestion.word,
      selectedColorFrench: selectedColorFrench,
      selectedColorEnglish: selectedColorEnglish,
      correctColor: correctAnswerEnglish,
      isCorrect: isCorrect,
      pointsEarned: pointsEarned,
      timestamp: new Date()
    });

    // Ajouter les points si correct
    if (isCorrect) {
      this.gameState.score += pointsEarned;
    }

    console.log('New total score:', this.gameState.score);
    console.log('=============================');

    // Passer à la question suivante
    this.moveToNextQuestion();
  }

  // ✅ LOGIC ANSWER
  submitLogicAnswer() {
    if (!this.currentQuestion) return;

    let isCorrect = false;
    let pointsEarned = 0;

    // Pour les séquences logiques
    if (this.currentQuestion.type === 'sequence') {
      const userNum = Number(this.sequenceAnswer);
      isCorrect = userNum === this.currentQuestion.correct;
      pointsEarned = isCorrect ? 10 : 0;
      console.log('Sequence answer:', { userNum, correct: this.currentQuestion.correct, isCorrect });
    }
    // Pour les problèmes mathématiques
    else if (this.currentQuestion.question) {
      const userAnswerLower = this.userAnswer.toLowerCase().trim();
      const correctAnswer = (this.currentQuestion.answer || this.currentQuestion.correct).toString().toLowerCase().trim();
      isCorrect = userAnswerLower === correctAnswer;
      pointsEarned = isCorrect ? 10 : 0;
      console.log('Problem answer:', { userAnswer: userAnswerLower, correct: correctAnswer, isCorrect });
    }
    // Pour les énigmes
    else if (this.currentQuestion.riddle) {
      const userAnswerLower = this.userAnswer.toLowerCase().trim();
      const correctAnswer = this.currentQuestion.answer.toString().toLowerCase().trim();
      isCorrect = userAnswerLower === correctAnswer;
      pointsEarned = isCorrect ? 10 : 0;
      console.log('Riddle answer:', { userAnswer: userAnswerLower, correct: correctAnswer, isCorrect });
    }

    // Sauvegarder la réponse
    this.gameState.answers.push({
      question: this.gameState.currentQuestionIndex,
      userAnswer: this.sequenceAnswer || this.userAnswer,
      correct: isCorrect,
      pointsEarned: pointsEarned,
      correctAnswer: this.currentQuestion.answer || this.currentQuestion.correct
    });

    // Ajouter les points si correct
    if (isCorrect) {
      this.gameState.score += pointsEarned;
    }

    console.log('Logic answer:', { isCorrect, score: this.gameState.score });
    this.moveToNextQuestion();
  }

  moveToNextQuestion() {
    if (this.gameState.currentQuestionIndex < this.gameState.totalQuestions - 1) {
      this.gameState.currentQuestionIndex++;
      this.currentQuestion = this.questions[this.gameState.currentQuestionIndex];
      this.userAnswer = '';
      this.sequenceAnswer = '';
      this.selectedOption = null;

      this.stopTimer();
      this.gameState.timeLeft = this.activity?.timeLimit || 120;
      this.gameState.timerActive = true;
      this.startTimer();

      console.log('Moving to question:', this.gameState.currentQuestionIndex + 1);
    } else {
      this.completeGame();
    }
  }

  completeGame() {
    console.log('Game completed with score:', this.gameState.score);
    this.stopTimer();
    this.gameState.currentPhase = 'COMPLETED';
    this.gameState.timerActive = false;

    // ✅ Vérifie que cette ligne est bien présente
    if (this.activity && this.activity.type) {
      this.challengeService.updateProgression(this.activity.type);
      console.log('📊 Progression mise à jour pour:', this.activity.type);
    }

    if (this.activity && this.activity.id) {
      const timeSpent = Math.floor((Date.now() - this.gameState.startTime.getTime()) / 1000);
      this.activityService.completeActivity(
        this.activity.id,
        this.gameState.score,
        timeSpent
      ).subscribe({
        next: () => console.log('Results saved'),
        error: (err) => console.error('Error saving results', err)
      });
    }
  }

  abandonGame() {
    this.stopTimer();
    this.gameState.currentPhase = 'ABANDONED';
    this.gameState.timerActive = false;
  }

  restartGame() {
    this.stopTimer();
    this.userAnswer = '';
    this.sequenceAnswer = '';
    this.selectedOption = null;
    this.initializeGame();
  }

  viewResults() {
    this.router.navigate(['/activities', this.activity?.id]);
  }

  formatTime(seconds: number): string {
    if (seconds < 0) seconds = 0;
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  getProgressPercentage(): number {
    if (this.gameState.currentPhase === 'COMPLETED' || this.gameState.currentPhase === 'ABANDONED') {
      return 100;
    }
    if (this.gameState.currentPhase === 'MEMORIZE') {
      return 0;
    }
    return ((this.gameState.currentQuestionIndex) / this.gameState.totalQuestions) * 100;
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      'MEMORY': '🧠',
      'ATTENTION': '👀',
      'LOGIC': '🔢'
    };
    return icons[type] || '📝';
  }

  getStatusMessage(): string {
    if (this.gameState.currentPhase === 'MEMORIZE') {
      return `Memorize... Time left: ${this.formatTime(this.gameState.timeLeft)}`;
    }
    if (this.gameState.currentPhase === 'RECALL' || this.gameState.currentPhase === 'QUESTION') {
      return `Question ${this.gameState.currentQuestionIndex + 1}/${this.gameState.totalQuestions}`;
    }
    if (this.gameState.currentPhase === 'COMPLETED') {
      return `Game Complete! Score: ${this.gameState.score}`;
    }
    if (this.gameState.currentPhase === 'ABANDONED') {
      return `Game Abandoned`;
    }
    return '';
  }

  getMaxPossibleScore(): number {
    if (!this.activity) return 10;

    switch (this.activity.type) {
      case 'ATTENTION':
        return (this.questions?.length || 3) * 10;
      case 'MEMORY':
        if (this.memoryGrid.length > 0) {
          return 30; // 3 questions * 10 points
        }
        return (this.memoryWords?.length || 5) * 10;
      case 'LOGIC':
        return (this.questions?.length || 3) * 10;
      default:
        return 10;
    }
  }

  getScoreDisplay(): string {
    return `${this.gameState.score} / ${this.getMaxPossibleScore()}`;
  }

  getScorePercentage(): number {
    const maxScore = this.getMaxPossibleScore();
    if (maxScore === 0) return 0;
    return (this.gameState.score / maxScore) * 100;
  }

  getMotivationalMessage(): string {
    const percentage = this.getScorePercentage();

    if (percentage >= 90) {
      return "🏆 Exceptionnel ! Tu es un champion ! Continue comme ça !";
    } else if (percentage >= 75) {
      return "🌟 Très bien ! Tu progresses énormément !";
    } else if (percentage >= 50) {
      return "👍 Bon travail ! Continue à t'entraîner !";
    } else if (percentage >= 25) {
      return "💪 Pas mal ! Chaque essai te rend plus fort !";
    } else {
      return "🌱 C'est un début ! La pratique rend parfait !";
    }
  }

  getTip(): string {
    const percentage = this.getScorePercentage();

    if (this.activity?.type === 'MEMORY') {
      if (percentage < 50) {
        return "💡 Essaie de regrouper les mots par catégories pour mieux les mémoriser !";
      } else {
        return "💡 Super ! Maintenant essaie de mémoriser dans l'ordre !";
      }
    } else if (this.activity?.type === 'ATTENTION') {
      if (percentage < 50) {
        return "💡 Concentre-toi sur la couleur, pas sur le mot écrit. Respire profondément !";
      } else {
        return "💡 Excellent ! Tu peux maintenant essayer d'aller plus vite !";
      }
    } else if (this.activity?.type === 'LOGIC') {
      if (percentage < 50) {
        return "💡 Cherche la règle cachée dans la séquence. Parfois c'est une addition, parfois une multiplication !";
      } else {
        return "💡 Bravo ! Essaie de créer tes propres séquences !";
      }
    }
    return "💡 Continue comme ça, tu vas y arriver !";
  }

  getCorrectAnswersCount(): number {
    if (!this.gameState.answers || this.gameState.answers.length === 0) {
      return 0;
    }

    // Pour l'attention, on utilise isCorrect
    if (this.activity?.type === 'ATTENTION') {
      return this.gameState.answers.filter(a => a.isCorrect === true).length;
    }

    // Pour la mémoire (grille ou mots)
    if (this.activity?.type === 'MEMORY') {
      if (this.memoryGrid.length > 0) {
        return this.gameState.answers.filter(a => a.correct === true).length;
      }
      return this.gameState.answers.filter(a => a.pointsEarned > 0).length;
    }

    // Pour la logique
    return this.gameState.answers.filter(a => a.correct === true).length;
  }

  getTotalPoints(): number {
    return this.gameState.score;
  }

  getTimeSpent(): string {
    if (!this.gameState.startTime) return '0:00';
    const timeSpentSeconds = Math.floor((Date.now() - this.gameState.startTime.getTime()) / 1000);
    return this.formatTime(timeSpentSeconds);
  }
}

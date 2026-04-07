import {
  Component, Output, EventEmitter,
  OnInit, OnDestroy, ViewChild,
  ElementRef, ChangeDetectionStrategy, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as faceapi from 'face-api.js';

@Component({
  standalone: true,
  selector: 'app-face-capture',
  templateUrl: './face-capture.html',
  styleUrls: ['./face-capture.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule]
})
export class FaceCaptureComponent implements OnInit, OnDestroy {

  @ViewChild('videoEl', { static: false }) videoEl!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvasEl', { static: false }) canvasEl!: ElementRef<HTMLCanvasElement>;

  @Output() embeddingReady = new EventEmitter<number[]>();
  @Output() captureError = new EventEmitter<string>();
  @Output() photoReady = new EventEmitter<string>();
  @Output() emotionDetected = new EventEmitter<string>();


  modelsLoaded = false;
  cameraActive = false;
  capturing = false;
  captured = false;
  faceDetectedCount = 0;       // ✅ compteur auto-détection
  statusMessage = 'Chargement des modèles...';
  statusType: 'info' | 'success' | 'error' = 'info';
  currentEmotion = '';
emotionEmoji   = '😐';

  private stream?: MediaStream | null = null;
  private detectionInterval?: ReturnType<typeof setInterval>;

  constructor(private cdr: ChangeDetectorRef) { }

  async ngOnInit(): Promise<void> {
    await this.loadModels();
  }

  ngOnDestroy(): void {
    this.stopAll();
  }

  private mark(): void { this.cdr.markForCheck(); }

  // ══════════════════════════════════════════
  // CHARGER LES MODÈLES
  // ══════════════════════════════════════════
async loadModels(): Promise<void> {
  try {
    this.statusMessage = 'Chargement des modèles IA...';
    this.statusType = 'info';
    this.mark();

    const MODEL_URL = '/models';
    await Promise.all([
      faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
      faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
      faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL),
      faceapi.nets.faceExpressionNet.loadFromUri(MODEL_URL) // ✅ AJOUTER
    ]);

    this.modelsLoaded = true;
    this.statusMessage = 'Modèles chargés ✅ Activez la caméra';
    this.statusType = 'success';
    this.mark();

  } catch (err) {
    this.statusMessage = 'Erreur chargement modèles ❌';
    this.statusType = 'error';
    this.captureError.emit('Erreur chargement modèles');
    this.mark();
  }
}

  // ══════════════════════════════════════════
  // ACTIVER LA CAMÉRA + démarrer boucle auto
  // ══════════════════════════════════════════
  async startCamera(): Promise<void> {
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 320, height: 240, facingMode: 'user' }
      });

      setTimeout(() => {
        if (this.videoEl?.nativeElement) {
          this.videoEl.nativeElement.srcObject = this.stream!;
          this.videoEl.nativeElement.play();
          this.startDetectionLoop(); // ✅ démarrer boucle après caméra prête
        }
      }, 300);

      this.cameraActive = true;
      this.faceDetectedCount = 0;
      this.statusMessage = 'Placez votre visage dans le cadre...';
      this.statusType = 'info';
      this.mark();

    } catch (err) {
      this.statusMessage = 'Accès caméra refusé ❌';
      this.statusType = 'error';
      this.captureError.emit('Accès caméra refusé');
      this.mark();
    }
  }

  // ══════════════════════════════════════════
  // BOUCLE AUTO-DÉTECTION (toutes les 100ms)
  // ══════════════════════════════════════════
private startDetectionLoop(): void {
  this.detectionInterval = setInterval(async () => {
    if (!this.videoEl?.nativeElement || this.capturing || this.captured) return;

    const detection = await faceapi
      .detectSingleFace(this.videoEl.nativeElement, new faceapi.TinyFaceDetectorOptions())
      .withFaceLandmarks()
      .withFaceExpressions(); // ✅ AJOUTER

    if (detection) {
      this.faceDetectedCount++;

      // ✅ Analyser l'émotion dominante
      const expressions = detection.expressions as any;
      const dominant = Object.entries(expressions)
        .sort((a: any, b: any) => b[1] - a[1])[0][0] as string;
      this.currentEmotion = dominant;
      this.emotionEmoji   = this.getEmoji(dominant);
      this.updateEmotionMessage(dominant);

      if (this.faceDetectedCount >= 20) {
        clearInterval(this.detectionInterval);
        await this.captureFace();
      }
    } else {
      this.faceDetectedCount = 0;
      this.statusMessage = 'Placez votre visage dans le cadre...';
      this.statusType = 'info';
    }

    this.mark();
  }, 100);
}

  // ══════════════════════════════════════════
  // CAPTURE AUTO
  // ══════════════════════════════════════════
async captureFace(): Promise<void> {
  if (!this.videoEl?.nativeElement || this.capturing) return;

  this.capturing = true;
  this.statusMessage = 'Extraction du visage...';
  this.statusType = 'info';
  this.mark();

  try {
    const detection = await faceapi
      .detectSingleFace(this.videoEl.nativeElement, new faceapi.TinyFaceDetectorOptions())
      .withFaceLandmarks()
      .withFaceDescriptor();

    if (!detection) {
      this.capturing = false;
      this.faceDetectedCount = 0;
      this.statusMessage = 'Aucun visage détecté ❌ Réessayez';
      this.statusType = 'error';
      this.startDetectionLoop();
      this.mark();
      return;
    }

    const embedding = Array.from(detection.descriptor);

    this.captured  = true;
    this.capturing = false;
    this.statusMessage = 'Visage capturé ✅';
    this.statusType = 'success';
    this.mark();

    this.drawDetection(detection);

    // ✅ Capturer la photo
    const snapCanvas = document.createElement('canvas');
    snapCanvas.width  = this.videoEl.nativeElement.videoWidth;
    snapCanvas.height = this.videoEl.nativeElement.videoHeight;
    snapCanvas.getContext('2d')?.drawImage(this.videoEl.nativeElement, 0, 0);
    const photoBase64 = snapCanvas.toDataURL('image/jpeg', 0.85);

    this.embeddingReady.emit(embedding);    // ✅ une seule fois
    this.photoReady.emit(photoBase64);
    this.emotionDetected.emit(this.currentEmotion); // ✅ émettre émotion
    this.stopAll();

  } catch (err) {
    this.capturing = false;
    this.faceDetectedCount = 0;
    this.statusMessage = 'Erreur de détection ❌';
    this.statusType = 'error';
    this.captureError.emit('Erreur détection visage');
    this.mark();
  }
}
  // ══════════════════════════════════════════
  // DESSINER LA DÉTECTION
  // ══════════════════════════════════════════
  private drawDetection(detection: any): void {
    const canvas = this.canvasEl?.nativeElement;
    const video = this.videoEl?.nativeElement;
    if (!canvas || !video) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const dims = faceapi.matchDimensions(canvas, video, true);
    const resized = faceapi.resizeResults(detection, dims);
    faceapi.draw.drawDetections(canvas, resized);
    faceapi.draw.drawFaceLandmarks(canvas, resized);
  }

  // ══════════════════════════════════════════
  // RÉESSAYER
  // ══════════════════════════════════════════
  retry(): void {
    this.captured = false;
    this.cameraActive = false;
    this.faceDetectedCount = 0;
    this.statusMessage = 'Modèles chargés ✅ Activez la caméra';
    this.statusType = 'success';
    this.mark();
    this.startCamera();
  }

  // ══════════════════════════════════════════
  // ARRÊTER TOUT
  // ══════════════════════════════════════════
  private stopAll(): void {
    clearInterval(this.detectionInterval);
    this.stream?.getTracks().forEach(t => t.stop());
    this.stream = null;
    this.cameraActive = false;
    this.mark();
  }
  // ✅ Méthodes à ajouter
private getEmoji(emotion: string): string {
  const map: { [key: string]: string } = {
    happy: '😊', neutral: '😐', sad: '😢',
    angry: '😠', fearful: '😨', disgusted: '🤢', surprised: '😮'
  };
  return map[emotion] || '😐';
}

private updateEmotionMessage(emotion: string): void {
  const pct = Math.round((this.faceDetectedCount / 20) * 100);
  switch (emotion) {
    case 'happy':
      this.statusMessage = `😊 Parfait ! Maintien... ${pct}%`;
      this.statusType = 'success'; break;
    case 'neutral':
      this.statusMessage = `😐 Détecté — Maintien... ${pct}%`;
      this.statusType = 'success'; break;
    case 'sad': case 'fearful': case 'disgusted':
      this.statusMessage = `⚠️ Semblez contraint — continuez si vous êtes libre`;
      this.statusType = 'error'; break;
    case 'surprised':
      this.statusMessage = `😮 Détecté — Restez naturel... ${pct}%`;
      this.statusType = 'info'; break;
    default:
      this.statusMessage = `Visage détecté — Maintien... ${pct}%`;
      this.statusType = 'success';
  }
}

  stopCamera(): void { this.stopAll(); }
}
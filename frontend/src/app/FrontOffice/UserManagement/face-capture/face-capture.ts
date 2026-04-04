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
  @Output() captureError   = new EventEmitter<string>();

  modelsLoaded      = false;
  cameraActive      = false;
  capturing         = false;
  captured          = false;
  faceDetectedCount = 0;       // ✅ compteur auto-détection
  statusMessage     = 'Chargement des modèles...';
  statusType: 'info' | 'success' | 'error' = 'info';

  private stream?: MediaStream | null = null;
  private detectionInterval?: ReturnType<typeof setInterval>;

  constructor(private cdr: ChangeDetectorRef) {}

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
        faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL)
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
        .withFaceLandmarks();

      if (detection) {
        this.faceDetectedCount++;
        const pct = Math.round((this.faceDetectedCount / 20) * 100);
        this.statusMessage = `Visage détecté — Maintien... ${pct}%`;
        this.statusType = 'success';

        // ✅ 20 frames consécutives (~2s) → capture automatique
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
        this.startDetectionLoop(); // reprendre la boucle
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
      this.embeddingReady.emit(embedding);
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
    const video  = this.videoEl?.nativeElement;
    if (!canvas || !video) return;

    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;

    const dims    = faceapi.matchDimensions(canvas, video, true);
    const resized = faceapi.resizeResults(detection, dims);
    faceapi.draw.drawDetections(canvas, resized);
    faceapi.draw.drawFaceLandmarks(canvas, resized);
  }

  // ══════════════════════════════════════════
  // RÉESSAYER
  // ══════════════════════════════════════════
  retry(): void {
    this.captured          = false;
    this.cameraActive      = false;
    this.faceDetectedCount = 0;
    this.statusMessage     = 'Modèles chargés ✅ Activez la caméra';
    this.statusType        = 'success';
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

  stopCamera(): void { this.stopAll(); }
}
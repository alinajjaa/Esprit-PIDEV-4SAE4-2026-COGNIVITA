import {
  Component, ChangeDetectionStrategy, ChangeDetectorRef
} from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../services/user.service';
import { FaceCaptureComponent } from '../face-capture/face-capture';


@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, FaceCaptureComponent]
})
export class LoginComponent {
  form: FormGroup;
  otpForm: FormGroup;

  loading = false;
  error = '';
  isBlocked = false;
  showPassword = false;

  // OTP step
  showOtpStep = false;
  pendingEmail = '';
  otpLoading = false;
  otpError = '';
  resendLoading = false;
  resendCooldown = 0;

  // ✅ NOUVEAU — Face step
  showFaceStep = false;
  pendingUserId: number | null = null;
  faceLoading = false;
  faceError = '';
  detectedEmotion: string = 'neutral';
  cardShake = false;
  cardSuccess = false;
  faceScore: number | null = null;
  faceConfidence: string = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  get email() { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get otp() { return this.otpForm.get('otp')!; }

  private mark(): void { this.cdr.markForCheck(); }

  private triggerShake(): void {
    this.cardShake = true; this.mark();
    setTimeout(() => { this.cardShake = false; this.mark(); }, 500);
  }

  private triggerSuccess(): void {
    this.cardSuccess = true; this.mark();
    setTimeout(() => { this.cardSuccess = false; this.mark(); }, 800);
  }

  // ══════════════════════════════════════════
  // ÉTAPE 1 — Credentials
  // ══════════════════════════════════════════
  login(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.triggerShake(); return; }

    this.loading = true;
    this.error = '';
    this.isBlocked = false;
    this.mark();

    this.userService.login({
      email: this.email.value,
      password: this.password.value
    }).subscribe({
      next: (res: any) => {
        this.loading = false;

        if (res.step === 'face_required') {
          // ✅ Spring Boot répond avec step: face_required → afficher face scan
          this.pendingUserId = res.userId;
          this.showFaceStep = true;
          this.mark();
          return;
        }

        if (res.twoFaRequired) {
          this.pendingEmail = res.email;
          this.showOtpStep = true;
          this.mark();
          return;
        }

        this.triggerSuccess();
        localStorage.setItem('jwt_token', res.token);
        setTimeout(() => {
          this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
        }, 600);
      },
      error: (err) => {
        this.loading = false;
        this.isBlocked = err.status === 403;
        this.error = err?.error?.message ||
          (this.isBlocked ? 'Your account has been blocked.' : 'Invalid credentials');
        this.mark();
        this.triggerShake();
      }
    });
  }

  // ══════════════════════════════════════════
  // ÉTAPE 2 — OTP (si 2FA activé)
  // ══════════════════════════════════════════
  verifyOtp(): void {
    if (this.otpForm.invalid) { this.otpForm.markAllAsTouched(); this.triggerShake(); return; }

    this.otpLoading = true;
    this.otpError = '';
    this.mark();

    this.userService.verifyOtp(this.pendingEmail, this.otp.value).subscribe({
      next: (res: any) => {
        this.otpLoading = false;

        // ✅ Après OTP → passer au face scan
        if (res.step === 'face_required') {
          this.pendingUserId = res.userId;
          this.showOtpStep = false;
          this.showFaceStep = true;
          this.mark();
          return;
        }

        this.triggerSuccess();
        localStorage.setItem('jwt_token', res.token);
        setTimeout(() => {
          this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
        }, 600);
      },
      error: (err) => {
        this.otpLoading = false;
        this.otpError = err?.error?.message || 'Invalid or expired code.';
        this.mark();
        this.triggerShake();
      }
    });
  }

  // ══════════════════════════════════════════
  // ÉTAPE 3 — Face ID
  // ══════════════════════════════════════════
  onFaceEmbeddingReady(embedding: number[]): void {
    if (!this.pendingUserId) return;

    this.faceLoading = true;
    this.faceError = '';
    this.mark();

    this.userService.verifyFace(this.pendingUserId, embedding).subscribe({
      next: (res: any) => {
        this.faceLoading = false;
        this.faceScore = res.score;
        this.faceConfidence = res.confidence_level;
        this.mark();

        this.triggerSuccess();
        this.userService.setSession(res.token, res.user);
        localStorage.setItem('jwt_token', res.token);

        // ✅ Sauvegarder l'émotion
        const userId = res.user?.id;
        const emotion = this.detectedEmotion || 'neutral'; // ← ton champ émotion
        if (userId && emotion) {
          this.userService.updateEmotion(userId, emotion).subscribe({
            next: () => console.log('✅ Émotion sauvegardée:', emotion),
            error: (err) => console.error('❌ Erreur émotion:', err)
          });
        }

        setTimeout(() => {
          this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
        }, 1500);
      },
      error: (err) => {
        this.faceLoading = false;
        this.faceScore = err?.error?.score ?? null;
        this.faceConfidence = err?.error?.confidence_level ?? '';
        this.faceError = err?.error?.message || 'Face verification failed ❌';
        this.mark();
        this.triggerShake();
      }
    });
  }
  onFaceCaptureError(error: string): void {
    this.faceError = error;
    this.mark();
  }

  // ══════════════════════════════════════════
  // OTP helpers
  // ══════════════════════════════════════════
  resendOtp(): void {
    if (this.resendCooldown > 0) return;
    this.resendLoading = true;
    this.mark();

    this.userService.resendOtp(this.pendingEmail).subscribe({
      next: () => {
        this.resendLoading = false;
        this.resendCooldown = 60;
        this.mark();
        const interval = setInterval(() => {
          this.resendCooldown--;
          this.mark();
          if (this.resendCooldown <= 0) clearInterval(interval);
        }, 1000);
      },
      error: () => { this.resendLoading = false; this.mark(); }
    });
  }


onEmotionDetected(emotion: string): void {
  this.detectedEmotion = emotion; // ← juste stocker, pas appeler l'API
}
}
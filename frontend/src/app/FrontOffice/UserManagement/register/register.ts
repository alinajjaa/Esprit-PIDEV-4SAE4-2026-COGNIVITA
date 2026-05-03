import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import {
  FormBuilder, FormGroup, Validators,
  AbstractControl, ValidationErrors, ReactiveFormsModule,
  AsyncValidatorFn
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable, of, timer } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { UserService } from '../../../services/user.service';
import { FaceCaptureComponent } from '../face-capture/face-capture';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirm = control.get('confirmPassword');
  if (password && confirm && password.value !== confirm.value) {
    confirm.setErrors({ mismatch: true });
    return { mismatch: true };
  }
  if (confirm?.errors?.['mismatch']) confirm.setErrors(null);
  return null;
}

@Component({
  standalone: true,
  selector: 'app-register',
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, FaceCaptureComponent]
})
export class RegisterComponent {
  form: FormGroup;
  otpForm: FormGroup;

  loading = false;
  error = '';
  success = '';
  showPassword = false;
  showConfirm = false;

  showOtpStep = false;
  pendingEmail = '';
  otpLoading = false;
  otpError = '';
  resendLoading = false;
  resendCooldown = 0;

  wrapperShake = false;
  wrapperSuccess = false;

  passwordStrength = 0;
  passwordStrengthLabel = '';
  passwordStrengthColor = '';
  showFaceStep = false;
  faceLoading = false;
  faceError = '';
  savedUserId: number | null = null;
  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email], [this.emailExistsValidator()]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    this.form.get('password')?.valueChanges.subscribe(val => this.checkPasswordStrength(val));
  }

  emailExistsValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value || control.value.length < 3) return of(null);
      return timer(500).pipe(
        switchMap(() =>
          this.userService.checkEmailExists(control.value).pipe(
            map(exists => exists ? { emailExists: true } : null),
            catchError(() => of(null))
          )
        )
      );
    };
  }

  get firstName() { return this.form.get('firstName')!; }
  get lastName() { return this.form.get('lastName')!; }
  get email() { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get confirmPassword() { return this.form.get('confirmPassword')!; }
  get otp() { return this.otpForm.get('otp')!; }
  get emailChecking() { return this.email.status === 'PENDING'; }

  private mark(): void {
    this.cdr.markForCheck();
  }

  checkPasswordStrength(password: string): void {
    if (!password) {
      this.passwordStrength = 0;
      this.passwordStrengthLabel = '';
      this.passwordStrengthColor = '';
      return;
    }
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/\d/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    this.passwordStrength = strength;
    if (strength <= 1) {
      this.passwordStrengthLabel = 'Weak';
      this.passwordStrengthColor = '#ff3250';
    } else if (strength <= 3) {
      this.passwordStrengthLabel = 'Medium';
      this.passwordStrengthColor = '#ffd700';
    } else {
      this.passwordStrengthLabel = 'Strong';
      this.passwordStrengthColor = '#00ff80';
    }
  }

  private triggerShake(): void {
    this.wrapperShake = true;
    this.mark();
    setTimeout(() => { this.wrapperShake = false; this.mark(); }, 500);
  }

  private triggerSuccess(): void {
    this.wrapperSuccess = true;
    this.mark();
    setTimeout(() => { this.wrapperSuccess = false; this.mark(); }, 800);
  }

  register(): void {
    if (this.form.invalid || this.emailChecking) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }

    this.loading = true;
    this.error = '';
    this.mark();

    this.userService.register({
      firstName: this.firstName.value,
      lastName: this.lastName.value,
      email: this.email.value,
      password: this.password.value
    }).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.pendingEmail = res.email;

        this.email.clearAsyncValidators();
        this.email.updateValueAndValidity();

        this.showOtpStep = true;
        this.triggerSuccess();
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Registration failed.';
        this.mark();
        this.triggerShake();
      }
    });
  }

  verifyRegister(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      this.triggerShake();
      return;
    }

    this.otpLoading = true;
    this.otpError = '';
    this.mark();

    this.userService.verifyRegister(this.pendingEmail, this.otp.value).subscribe({
      next: (res: any) => {
        this.otpLoading = false;
        this.savedUserId = res.userId;  // ✅ plus de res.user
        this.showOtpStep = false;
        this.showFaceStep = true;
        this.triggerSuccess();
        this.mark();
      },
      error: (err) => {
        this.otpLoading = false;
        this.otpError = err?.error?.message || 'Invalid or expired code.';
        this.mark();
        this.triggerShake();
      }
    });
  }
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
      error: () => {
        this.resendLoading = false;
        this.mark();
      }
    });
  }
  // Ajouter ces méthodes
  onFaceEmbeddingReady(embedding: number[]): void {
    if (!this.savedUserId) return;

    this.faceLoading = true;
    this.faceError = '';
    this.mark();

    this.userService.registerFace(this.savedUserId, embedding).subscribe({
      next: (res: any) => {
        this.faceLoading = false;
        this.userService.setSession(res.token, res.user);
        this.triggerSuccess();
        this.success = 'Account created & face registered! Redirecting...';
        this.mark();
        setTimeout(() => {
          this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
        }, 1500);
      },
      error: (err) => {
        this.faceLoading = false;
        this.faceError = err?.error?.message || 'Face registration failed ❌';
        this.mark();
        this.triggerShake();
      }
    });
  }
  onFaceCaptureError(error: string): void {
    this.faceError = error;
    this.mark();
  }

  skipFace(): void {
    // Optionnel : permettre de sauter le face registration
    this.router.navigate(['/home']);
  }

  backToRegister(): void {
    this.email.setAsyncValidators([this.emailExistsValidator()]);
    this.email.updateValueAndValidity();
    this.showOtpStep = false;
    this.mark();
  }

  onPhotoReady(photoBase64: string): void {
    if (!this.savedUserId) return;

    const blob = this.base64ToBlob(photoBase64, 'image/jpeg');
    const file = new File([blob], 'profile.jpg', { type: 'image/jpeg' });

    this.userService.uploadProfileImage(this.savedUserId, file).subscribe({
      next: (url: string) => {
        console.log('✅ Photo profil sauvegardée sur Cloudinary:', url);
      },
      error: (err) => {
        console.error('❌ Upload Cloudinary failed:', err);
      }
    });
  }

  private base64ToBlob(base64: string, type: string): Blob {
    const byteString = atob(base64.split(',')[1]);
    const ab = new ArrayBuffer(byteString.length);
    const ia = new Uint8Array(ab);
    for (let i = 0; i < byteString.length; i++) {
      ia[i] = byteString.charCodeAt(i);
    }
    return new Blob([ab], { type });
  }
  onEmotionDetected(emotion: string): void {
    if (!this.savedUserId) return;
    this.userService.updateEmotion(this.savedUserId, emotion).subscribe({
      next: () => console.log('✅ Émotion sauvegardée:', emotion),
      error: (err) => console.error('❌ Erreur:', err)
    });
  }


}
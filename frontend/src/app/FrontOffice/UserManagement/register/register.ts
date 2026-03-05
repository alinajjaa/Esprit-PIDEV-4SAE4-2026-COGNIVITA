import { Component } from '@angular/core';
import {
  FormBuilder, FormGroup, Validators,
  AbstractControl, ValidationErrors, ReactiveFormsModule,
  AsyncValidatorFn
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, switchMap } from 'rxjs/operators';
import { UserService } from '../../../services/user.service';

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
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class RegisterComponent {
  form: FormGroup;
  otpForm: FormGroup;

  // États
  loading = false;
  error = '';
  success = '';
  showPassword = false;
  showConfirm = false;

  // ✅ Étape OTP
  showOtpStep = false;
  pendingEmail = '';
  otpLoading = false;
  otpError = '';
  resendLoading = false;
  resendCooldown = 0;

  // Animations
  wrapperShake = false;
  wrapperSuccess = false;

  // Password strength
  passwordStrength = 0;
  passwordStrengthLabel = '';
  passwordStrengthColor = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['',
        [Validators.required, Validators.email],
        [this.emailExistsValidator()]
      ],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    this.form.get('password')?.valueChanges.subscribe(val => {
      this.checkPasswordStrength(val);
    });
  }

  /* ── Async validator email ────────────────────────── */
  emailExistsValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value || control.value.length < 3) return of(null);
      return of(control.value).pipe(
        debounceTime(600),
        switchMap(email =>
          this.userService.checkEmailExists(email).pipe(
            map(exists => exists ? { emailExists: true } : null),
            catchError(() => of(null))
          )
        )
      );
    };
  }

  /* ── Getters ──────────────────────────────────────── */
  get firstName() { return this.form.get('firstName')!; }
  get lastName() { return this.form.get('lastName')!; }
  get email() { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get confirmPassword() { return this.form.get('confirmPassword')!; }
  get otp() { return this.otpForm.get('otp')!; }
  get emailChecking() { return this.email.status === 'PENDING'; }

  /* ── Password strength ────────────────────────────── */
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

  /* ── Animations ───────────────────────────────────── */
  private triggerShake(): void {
    this.wrapperShake = true;
    setTimeout(() => this.wrapperShake = false, 500);
  }

  private triggerSuccess(): void {
    this.wrapperSuccess = true;
    setTimeout(() => this.wrapperSuccess = false, 800);
  }

  /* ── Étape 1 : Register ───────────────────────────── */
  register(): void {
    if (this.form.invalid || this.emailChecking) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }

    this.loading = true;
    this.error = '';

    this.userService.register({
      firstName: this.firstName.value,
      lastName: this.lastName.value,
      email: this.email.value,
      password: this.password.value
    }).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.pendingEmail = res.email;
        this.showOtpStep = true; // ✅ Afficher étape OTP
        this.triggerSuccess();
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Registration failed.';
        this.triggerShake();
      }
    });
  }

  /* ── Étape 2 : Vérifier OTP ───────────────────────── */
verifyRegister(): void {
  if (this.otpForm.invalid) {
    this.otpForm.markAllAsTouched();
    this.triggerShake();
    return;
  }

  this.otpLoading = true;
  this.otpError = '';

  this.userService.verifyRegister(this.pendingEmail, this.otp.value).subscribe({
    next: (res: any) => {
      this.otpLoading = false;
      this.triggerSuccess();

      // ✅ Met à jour BehaviorSubject + localStorage
      this.userService.setSession(res.token, res.user);

      this.success = 'Account verified! Redirecting...';
      setTimeout(() => this.router.navigate(['/home']), 1500);
    },
    error: (err) => {
      this.otpLoading = false;
      this.otpError = err?.error?.message || 'Invalid or expired code.';
      this.triggerShake();
    }
  });
}
  /* ── Renvoyer OTP ─────────────────────────────────── */
  resendOtp(): void {
    if (this.resendCooldown > 0) return;
    this.resendLoading = true;

    this.userService.resendOtp(this.pendingEmail).subscribe({
      next: () => {
        this.resendLoading = false;
        this.resendCooldown = 60;
        const interval = setInterval(() => {
          this.resendCooldown--;
          if (this.resendCooldown <= 0) clearInterval(interval);
        }, 1000);
      },
      error: () => { this.resendLoading = false; }
    });
  }
}
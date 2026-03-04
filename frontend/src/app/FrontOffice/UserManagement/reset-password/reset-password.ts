import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule } from '@angular/forms';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('newPassword');
  const confirm  = control.get('confirmPassword');
  if (password && confirm && password.value !== confirm.value) {
    confirm.setErrors({ mismatch: true });
    return { mismatch: true };
  }
  if (confirm?.errors?.['mismatch']) {
    confirm.setErrors(null);
  }
  return null;
}

@Component({
  standalone: true,
  selector: 'app-reset-password',
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  token = '';
  loading = false;
  message = '';
  isSuccess = false;
  showPassword = false;
  showConfirm  = false;

  // Animations
  cardShake   = false;
  cardSuccess = false;

  // Password strength
  passwordStrength = 0;
  passwordStrengthLabel = '';
  passwordStrengthColor = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });

    this.form.get('newPassword')?.valueChanges.subscribe(val => {
      this.checkPasswordStrength(val);
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.message  = 'Invalid reset link.';
      this.isSuccess = false;
    }
  }

  /* ── Getters ──────────────────────────────────── */
  get newPassword()     { return this.form.get('newPassword')!; }
  get confirmPassword() { return this.form.get('confirmPassword')!; }

  /* ── Password strength ────────────────────────── */
  checkPasswordStrength(password: string): void {
    if (!password) {
      this.passwordStrength = 0;
      this.passwordStrengthLabel = '';
      this.passwordStrengthColor = '';
      return;
    }
    let strength = 0;
    if (password.length >= 8)          strength++;
    if (/[A-Z]/.test(password))        strength++;
    if (/[a-z]/.test(password))        strength++;
    if (/\d/.test(password))           strength++;
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

  /* ── Animations ───────────────────────────────── */
  private triggerShake(): void {
    this.cardShake = true;
    setTimeout(() => this.cardShake = false, 500);
  }

  private triggerSuccess(): void {
    this.cardSuccess = true;
    setTimeout(() => this.cardSuccess = false, 800);
  }

  /* ── Submit ───────────────────────────────────── */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }

    if (!this.token) {
      this.message  = 'Invalid reset link.';
      this.isSuccess = false;
      this.triggerShake();
      return;
    }

    this.loading = true;
    this.message = '';

    this.http.post(
      `http://localhost:8081/api/users/reset-password?token=${this.token}&newPassword=${this.newPassword.value}`,
      {},
      { responseType: 'text' }
    ).subscribe({
      next: () => {
        this.loading   = false;
        this.isSuccess = true;
        this.message   = 'Password updated successfully! Redirecting...';
        this.triggerSuccess();
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.loading   = false;
        this.isSuccess = false;
        this.message   = err?.error || 'Invalid or expired token.';
        this.triggerShake();
      }
    });
  }
}
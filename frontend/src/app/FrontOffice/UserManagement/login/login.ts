import { Component } from '@angular/core';
import {
  FormBuilder, FormGroup, Validators, ReactiveFormsModule
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../services/user.service';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class LoginComponent {
  form: FormGroup;
  otpForm: FormGroup;

  loading        = false;
  error          = '';
  isBlocked      = false;
  showPassword   = false;

  // ✅ 2FA
  showOtpStep    = false;
  pendingEmail   = '';
  otpLoading     = false;
  otpError       = '';
  resendLoading  = false;
  resendCooldown = 0;

  // Animations
  cardShake    = false;
  cardSuccess  = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get otp()      { return this.otpForm.get('otp')!; }

  private triggerShake(): void {
    this.cardShake = true;
    setTimeout(() => this.cardShake = false, 500);
  }

  private triggerSuccess(): void {
    this.cardSuccess = true;
    setTimeout(() => this.cardSuccess = false, 800);
  }

  login(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }

    this.loading  = true;
    this.error    = '';
    this.isBlocked = false;

    this.userService.login({
      email:    this.email.value,
      password: this.password.value
    }).subscribe({
      next: (res: any) => {
        this.loading = false;

        if (res.twoFaRequired) {
          this.pendingEmail = res.email;
          this.showOtpStep  = true;
        } else {
          this.triggerSuccess();
          localStorage.setItem('jwt_token', res.token);
          setTimeout(() => {
            this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
          }, 600);
        }
      },
      error: (err) => {
        this.loading = false;

        if (err?.error?.error === 'email_not_verified') {
          this.pendingEmail = err.error.email;
          this.showOtpStep  = true;
          return;
        }

        if (err.status === 403) {
          this.isBlocked = true;
          this.error = err?.error?.message || 'Your account has been blocked.';
        } else {
          this.error = err?.error?.message || 'Invalid credentials';
        }
        this.triggerShake();
      }
    });
  }

  verifyOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      this.triggerShake();
      return;
    }

    this.otpLoading = true;
    this.otpError   = '';

    this.userService.verifyOtp(this.pendingEmail, this.otp.value).subscribe({
      next: (res: any) => {
        this.otpLoading = false;
        this.triggerSuccess();
        localStorage.setItem('jwt_token', res.token);
        setTimeout(() => {
          this.router.navigate(res.role === 'ADMIN' ? ['/admin'] : ['/home']);
        }, 600);
      },
      error: (err) => {
        this.otpLoading = false;
        this.otpError   = err?.error?.message || 'Invalid or expired code.';
        this.triggerShake();
      }
    });
  }

  resendOtp(): void {
    if (this.resendCooldown > 0) return;
    this.resendLoading = true;

    this.userService.resendOtp(this.pendingEmail).subscribe({
      next: () => {
        this.resendLoading  = false;
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
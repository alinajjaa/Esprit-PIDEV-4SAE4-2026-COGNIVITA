import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  FormBuilder, FormGroup, Validators, ReactiveFormsModule
} from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class ForgotPasswordComponent {
  form: FormGroup;
  loading   = false;
  message   = '';
  isSuccess = false;

  // Animations
  cardShake   = false;
  cardSuccess = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  /* ── Getter ───────────────────────────────────── */
  get email() { return this.form.get('email')!; }

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

    this.loading = true;
    this.message = '';

    this.http.post(
      `http://localhost:8081/api/users/forgot-password?email=${this.email.value}`,
      {},
      { responseType: 'text' }
    ).subscribe({
      next: (res) => {
        this.loading   = false;
        this.isSuccess = true;
        this.message   = res;
        this.triggerSuccess();
      },
      error: () => {
        this.loading   = false;
        this.isSuccess = false;
        this.message   = 'Something went wrong. Please try again.';
        this.triggerShake();
      }
    });
  }
}
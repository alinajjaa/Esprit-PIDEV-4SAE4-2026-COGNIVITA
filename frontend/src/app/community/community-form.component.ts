import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Community, CommunityPayload } from '../services/communities.service';

@Component({
  selector: 'app-community-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="form-card">
      <h3>{{ editing ? 'Edit Community' : 'Create Community' }}</h3>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="error-box" *ngIf="submitError">{{ submitError }}</div>

        <label>Name</label>
        <input formControlName="name" type="text" [disabled]="submitting" />
        <small class="error" *ngIf="form.controls.name.touched && form.controls.name.invalid">
          Name is required (max 120 chars)
        </small>

        <label>Description</label>
        <textarea formControlName="description" rows="4" [disabled]="submitting"></textarea>
        <small class="error" *ngIf="form.controls.description.touched && form.controls.description.invalid">
          Description is required (max 255 chars)
        </small>

        <div class="actions">
          <button type="submit" [disabled]="form.invalid || submitting">
            {{ submitting ? (editing ? 'Updating...' : 'Creating...') : (editing ? 'Update' : 'Create') }}
          </button>
          <button type="button" class="secondary" [disabled]="submitting" (click)="cancel.emit()">
            Cancel
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .form-card { background: rgba(255,255,255,0.05); border: 1px solid rgba(0,255,255,0.25); border-radius: 12px; padding: 16px; }
    h3 { margin: 0 0 12px; color: #fff; }
    form { display: flex; flex-direction: column; gap: 8px; }
    label { color: #b0e0ff; font-size: 0.9rem; }
    input, textarea {
      background: rgba(0,0,0,0.25); border: 1px solid rgba(0,255,255,0.25);
      border-radius: 8px; padding: 10px; color: #fff; outline: none;
    }
    .actions { display: flex; gap: 8px; margin-top: 8px; }
    button { background: #00bcd4; color: #041a20; border: none; border-radius: 8px; padding: 10px 12px; font-weight: 700; cursor: pointer; }
    button.secondary { background: #29323c; color: #dbeafe; }
    button:disabled { opacity: 0.6; cursor: not-allowed; }
    .error { color: #fca5a5; font-size: 0.8rem; }
    .error-box {
      background: rgba(185, 28, 28, 0.2);
      border: 1px solid rgba(248, 113, 113, 0.5);
      color: #fecaca;
      border-radius: 8px;
      padding: 8px 10px;
      font-size: 0.85rem;
      margin-bottom: 4px;
    }
  `]
})
export class CommunityFormComponent implements OnChanges {
  @Input() community: Community | null = null;
  @Input() submitting = false;
  @Input() submitError = '';
  @Output() save = new EventEmitter<CommunityPayload>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.maxLength(255)]]
  });

  get editing(): boolean {
    return !!this.community;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['community']) {
      if (this.community) {
        this.form.patchValue({
          name: this.community.name,
          description: this.community.description
        });
      } else {
        this.form.reset({ name: '', description: '' });
      }
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.save.emit({
      name: value.name ?? '',
      description: value.description ?? ''
    });
  }
}

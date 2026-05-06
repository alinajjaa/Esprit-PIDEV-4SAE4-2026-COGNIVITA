import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="confirm-backdrop" (click)="onCancel()">
      <div class="confirm-card" (click)="$event.stopPropagation()">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="actions">
          <button type="button" class="secondary" [disabled]="submitting" (click)="onCancel()">
            {{ cancelLabel }}
          </button>
          <button type="button" class="danger" [disabled]="submitting" (click)="onConfirm()">
            {{ submitting ? 'Working...' : confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .confirm-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(2, 6, 23, 0.72);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 2300;
      padding: 16px;
    }
    .confirm-card {
      width: min(480px, 100%);
      background: #0f172a;
      border: 1px solid rgba(0, 255, 255, 0.25);
      border-radius: 12px;
      padding: 16px;
      color: #e2e8f0;
    }
    h3 {
      margin: 0 0 8px;
      color: #fff;
    }
    p {
      margin: 0 0 14px;
      color: #cbd5e1;
    }
    .actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
    }
    button {
      border: none;
      border-radius: 8px;
      padding: 8px 12px;
      font-weight: 700;
      cursor: pointer;
    }
    .secondary {
      background: #334155;
      color: #e2e8f0;
    }
    .danger {
      background: #b91c1c;
      color: #fff;
    }
  `]
})
export class ConfirmDialogComponent {
  @Input() title = 'Confirm';
  @Input() message = 'Are you sure?';
  @Input() confirmLabel = 'Confirm';
  @Input() cancelLabel = 'Cancel';
  @Input() submitting = false;

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm(): void {
    if (!this.submitting) {
      this.confirm.emit();
    }
  }

  onCancel(): void {
    if (!this.submitting) {
      this.cancel.emit();
    }
  }
}

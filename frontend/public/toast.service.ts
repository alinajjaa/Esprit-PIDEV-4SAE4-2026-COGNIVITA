import { Injectable } from '@angular/core';

export interface Toast {
  id: number;
  type: 'success' | 'error' | 'warn' | 'info';
  title: string;
  message: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {

  toasts: Toast[] = [];
  private counter = 0;

  show(type: Toast['type'], title: string, message: string, duration = 3000) {
    const id = ++this.counter;

    const toast: Toast = { id, type, title, message, duration };
    this.toasts.push(toast);

    setTimeout(() => this.remove(id), duration);
  }

  remove(id: number) {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  success(title: string, message: string) {
    this.show('success', title, message);
  }

  error(title: string, message: string) {
    this.show('error', title, message);
  }

  warn(title: string, message: string) {
    this.show('warn', title, message);
  }

  info(title: string, message: string) {
    this.show('info', title, message);
  }
}

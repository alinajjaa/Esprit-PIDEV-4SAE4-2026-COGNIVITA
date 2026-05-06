import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Community, CommunitiesService, CommunityPayload } from '../services/communities.service';
import { CommunityFormComponent } from './community-form.component';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';

@Component({
  selector: 'app-communities-page',
  standalone: true,
  imports: [CommonModule, RouterModule, CommunityFormComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <div class="header">
        <h2>Community Groups</h2>
        <p>Create and manage discussion communities</p>
        <button class="create-btn" (click)="openCreate()">Create Community</button>
      </div>

      <div class="layout">
        <div class="list-card">
          <div class="status" *ngIf="loading && communities.length === 0">Loading communities...</div>
          <div class="status error" *ngIf="listError">{{ listError }}</div>
          <div class="status" *ngIf="!loading && !listError && communities.length === 0">
            No communities yet.
          </div>

          <div class="item" *ngFor="let c of communities">
            <div class="meta">
              <h4>{{ c.name }}</h4>
              <p>{{ c.description }}</p>
            </div>
            <div class="actions">
              <button [routerLink]="['/community', c.id]">Open</button>
              <button (click)="edit(c)">Edit</button>
              <button class="danger" (click)="remove(c.id)">Delete</button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-backdrop" *ngIf="showDialog" (click)="closeDialog()">
        <div class="modal" (click)="$event.stopPropagation()">
          <app-community-form
            [community]="editingCommunity"
            [submitting]="dialogSubmitting"
            [submitError]="dialogError"
            (save)="onSave($event)"
            (cancel)="closeDialog()"
          />
        </div>
      </div>

      <app-confirm-dialog
        *ngIf="pendingDeleteCommunityId !== null"
        title="Delete Community"
        message="This community will be permanently removed. Do you want to continue?"
        confirmLabel="Delete"
        [submitting]="deleteSubmitting"
        (confirm)="confirmDeleteCommunity()"
        (cancel)="cancelDeleteCommunity()"
      />
    </div>
  `,
  styles: [`
    .page { padding: 24px; max-width: 1200px; margin: 0 auto; color: #e2e8f0; }
    .header { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .header h2 { margin: 0; color: #fff; }
    .header p { margin: 0; color: #93c5fd; flex: 1 1 280px; }
    .create-btn { background: #00bcd4; color: #041a20; border: none; border-radius: 10px; padding: 10px 14px; font-weight: 700; cursor: pointer; }
    .layout { display: block; }
    .list-card { background: rgba(255,255,255,0.04); border: 1px solid rgba(0,255,255,0.2); border-radius: 12px; padding: 12px; }
    .item { border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; padding: 12px; margin-bottom: 10px; display: flex; justify-content: space-between; gap: 12px; }
    .meta h4 { margin: 0 0 4px; color: #fff; }
    .meta p { margin: 0; color: #cbd5e1; font-size: 0.92rem; }
    .actions { display: flex; gap: 8px; align-items: center; }
    button { background: #00bcd4; color: #041a20; border: none; border-radius: 8px; padding: 8px 10px; font-weight: 700; cursor: pointer; }
    button.danger { background: #b91c1c; color: #fff; }
    .status { padding: 8px; color: #93c5fd; }
    .status.error { color: #fca5a5; }
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(2, 6, 23, 0.72);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 2000;
      padding: 16px;
    }
    .modal {
      width: min(560px, 100%);
    }
  `]
})
export class CommunitiesPageComponent implements OnInit {
  private static readonly CACHE_KEY = 'community_cache_v1';

  communities: Community[] = [];
  editingCommunity: Community | null = null;
  showDialog = false;

  loading = false;
  listError = '';

  dialogSubmitting = false;
  dialogError = '';
  pendingDeleteCommunityId: number | null = null;
  deleteSubmitting = false;

  constructor(
    private readonly communitiesService: CommunitiesService,
    private readonly ngZone: NgZone,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.hydrateFromCache();
    this.load();
  }

  load(): void {
    this.loading = true;
    this.listError = '';
    this.communitiesService.getAll().subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.communities = data;
          this.persistCache(data);
          this.loading = false;
          this.refreshView();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.listError = this.getError(err);
          this.loading = false;
          this.refreshView();
        });
      }
    });
  }

  edit(community: Community): void {
    this.editingCommunity = community;
    this.dialogError = '';
    this.showDialog = true;
  }

  openCreate(): void {
    this.editingCommunity = null;
    this.dialogError = '';
    this.showDialog = true;
  }

  closeDialog(): void {
    if (this.dialogSubmitting) return;
    this.editingCommunity = null;
    this.showDialog = false;
    this.dialogError = '';
  }

  onSave(payload: CommunityPayload): void {
    this.dialogSubmitting = true;
    this.dialogError = '';

    if (this.editingCommunity) {
      this.communitiesService.update(this.editingCommunity.id, payload).subscribe({
        next: (updated) => {
          this.ngZone.run(() => {
            this.communities = this.communities.map((c) => (c.id === updated.id ? updated : c));
            this.persistCache(this.communities);
            this.dialogSubmitting = false;
            this.closeDialog();
            this.refreshView();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.dialogError = this.getError(err);
            this.dialogSubmitting = false;
            this.refreshView();
          });
        }
      });
      return;
    }

    this.communitiesService.create(payload).subscribe({
      next: (created) => {
        this.ngZone.run(() => {
          this.communities = [created, ...this.communities];
          this.persistCache(this.communities);
          this.dialogSubmitting = false;
          this.closeDialog();
          this.refreshView();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.dialogError = this.getError(err);
          this.dialogSubmitting = false;
          this.refreshView();
        });
      }
    });
  }

  remove(id: number): void {
    this.pendingDeleteCommunityId = id;
  }

  confirmDeleteCommunity(): void {
    if (this.pendingDeleteCommunityId === null) return;
    this.deleteSubmitting = true;
    this.listError = '';
    this.communitiesService.delete(this.pendingDeleteCommunityId).subscribe({
      next: () => {
        this.ngZone.run(() => {
          const deletedId = this.pendingDeleteCommunityId;
          this.communities = this.communities.filter((c) => c.id !== deletedId);
          this.persistCache(this.communities);
          if (this.editingCommunity?.id === deletedId) {
            this.closeDialog();
          }
          this.pendingDeleteCommunityId = null;
          this.deleteSubmitting = false;
          this.refreshView();
        });
      },
      error: (err) => this.ngZone.run(() => {
        this.listError = this.getError(err);
        this.deleteSubmitting = false;
        this.refreshView();
      })
    });
  }

  cancelDeleteCommunity(): void {
    if (this.deleteSubmitting) return;
    this.pendingDeleteCommunityId = null;
  }

  private getError(err: any): string {
    if (typeof err?.error?.error === 'string') return err.error.error;
    return 'Request failed. Verify gateway/community-service are running.';
  }

  private hydrateFromCache(): void {
    try {
      const raw = localStorage.getItem(CommunitiesPageComponent.CACHE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as Community[];
      if (Array.isArray(parsed)) {
        this.communities = parsed;
      }
    } catch {
      // ignore bad cache
    }
  }

  private persistCache(data: Community[]): void {
    try {
      localStorage.setItem(CommunitiesPageComponent.CACHE_KEY, JSON.stringify(data));
    } catch {
      // ignore storage limits
    }
  }

  private refreshView(): void {
    this.cdr.detectChanges();
  }
}

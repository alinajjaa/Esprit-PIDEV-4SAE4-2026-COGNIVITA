import { ChangeDetectorRef, Component, NgZone, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommunitiesService, Community } from '../services/communities.service';
import { CommentsService, Comment } from '../services/comments.service';
import { Post, PostsService } from '../services/posts.service';
import { VotesService, Vote } from '../services/votes.service';
import { FORUM_TEST_USERS, ForumTestUser } from '../services/forum-users';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';

@Component({
  selector: 'app-community-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, ConfirmDialogComponent],
  template: `
    <div class="page">
      <a routerLink="/community" class="back-link">Back to Communities</a>

      <div class="header-card" *ngIf="community">
        <h2>{{ community.name }}</h2>
        <p>{{ community.description }}</p>
      </div>

      <div class="status" *ngIf="loading">Loading community...</div>
      <div class="status error" *ngIf="errorMessage">{{ errorMessage }}</div>

      <div class="actions-row" *ngIf="community">
        <label class="user-select">
          Active User
          <select [ngModel]="activeUser" (ngModelChange)="onUserChange($event)">
            <option *ngFor="let u of users" [value]="u">{{ u }}</option>
          </select>
        </label>
        <button (click)="openPostDialog()">Create Post</button>
      </div>

      <div class="post-list" *ngIf="!loading && posts.length > 0">
        <article class="post-card" *ngFor="let p of posts">
          <div class="post-top">
            <h3>{{ p.title }}</h3>
            <div class="post-actions" *ngIf="canManagePost(p)">
              <button (click)="startEditPost(p)">Edit</button>
              <button class="danger" (click)="deletePost(p.id)">Delete</button>
            </div>
          </div>
          <p class="post-body">{{ p.content }}</p>
          <div class="post-meta">
            <span>by {{ p.author }}</span>
            <span>Score: {{ getPostScore(p.id) }}</span>
          </div>

          <div class="vote-actions">
            <button (click)="vote(p.id, 1)">+1</button>
            <button (click)="vote(p.id, -1)">-1</button>
            <button class="secondary" (click)="clearVote(p.id)">Clear Vote</button>
          </div>

          <div class="comments">
            <h4>Comments</h4>
            <div class="comment" *ngFor="let c of commentsByPost[p.id] || []">
              <div *ngIf="editingCommentId !== c.id">{{ c.content }}</div>
              <small *ngIf="editingCommentId !== c.id">by {{ c.author }}</small>
              <form *ngIf="editingCommentId === c.id" [formGroup]="editCommentForm" (ngSubmit)="saveEditedComment(p.id, c.id)">
                <input formControlName="author" placeholder="Author" />
                <input formControlName="content" placeholder="Comment content" />
                <div class="dialog-actions">
                  <button type="submit" [disabled]="editCommentForm.invalid">Save</button>
                  <button type="button" class="secondary" (click)="cancelEditComment()">Cancel</button>
                </div>
              </form>
              <div class="comment-actions" *ngIf="editingCommentId !== c.id && canManageComment(c, p)">
                <button class="secondary" (click)="startEditComment(c)">Edit</button>
                <button class="danger" (click)="deleteComment(p.id, c.id)">Delete</button>
              </div>
            </div>

            <form [formGroup]="commentForms[p.id]" (ngSubmit)="addComment(p.id)">
              <input formControlName="content" placeholder="Add a comment" />
              <button type="submit" [disabled]="commentForms[p.id].invalid">Comment</button>
            </form>
          </div>
        </article>
      </div>

      <div class="status" *ngIf="!loading && !errorMessage && posts.length === 0">
        No posts yet for this community.
      </div>

      <div class="modal-backdrop" *ngIf="showPostDialog" (click)="closePostDialog()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h3>{{ editingPost ? 'Edit Post' : 'Create Post' }}</h3>
          <div class="error" *ngIf="dialogError">{{ dialogError }}</div>
          <form [formGroup]="postForm" (ngSubmit)="savePost()">
            <label>Title</label>
            <input formControlName="title" />
            <label>Content</label>
            <textarea rows="5" formControlName="content"></textarea>
            <div class="dialog-actions">
              <button type="submit" [disabled]="postForm.invalid || dialogSubmitting">
                {{ dialogSubmitting ? (editingPost ? 'Updating...' : 'Creating...') : (editingPost ? 'Update' : 'Create') }}
              </button>
              <button type="button" class="secondary" [disabled]="dialogSubmitting" (click)="closePostDialog()">Cancel</button>
            </div>
          </form>
        </div>
      </div>

      <app-confirm-dialog
        *ngIf="pendingDelete.type !== null"
        [title]="pendingDelete.type === 'post' ? 'Delete Post' : 'Delete Comment'"
        [message]="pendingDelete.type === 'post'
          ? 'This post will be removed. Do you want to continue?'
          : 'This comment will be removed. Do you want to continue?'"
        [confirmLabel]="'Delete'"
        [submitting]="deleteSubmitting"
        (confirm)="confirmDelete()"
        (cancel)="cancelDelete()"
      />
    </div>
  `,
  styles: [`
    .page { padding: 24px; max-width: 1100px; margin: 0 auto; color: #e2e8f0; }
    .back-link { color: #93c5fd; text-decoration: none; display: inline-block; margin-bottom: 12px; }
    .header-card { background: rgba(255,255,255,0.05); border: 1px solid rgba(0,255,255,0.2); border-radius: 12px; padding: 14px; margin-bottom: 12px; }
    .header-card h2 { margin: 0 0 4px; color: #fff; }
    .header-card p { margin: 0; color: #cbd5e1; }
    .actions-row { margin-bottom: 12px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .user-select { display: flex; align-items: center; gap: 8px; color: #93c5fd; font-size: 0.9rem; }
    .user-select select { background: rgba(0,0,0,0.3); border: 1px solid rgba(0,255,255,0.2); color: #fff; border-radius: 8px; padding: 6px 8px; }
    .post-list { display: grid; gap: 12px; }
    .post-card { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 12px; }
    .post-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .post-top h3 { margin: 0; color: #fff; }
    .post-actions, .vote-actions, .dialog-actions { display: flex; gap: 8px; }
    .post-body { color: #d1d5db; margin: 10px 0; white-space: pre-wrap; }
    .post-meta { display: flex; gap: 12px; color: #93c5fd; font-size: 0.9rem; margin-bottom: 8px; }
    .comments { margin-top: 8px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 8px; }
    .comments h4 { margin: 0 0 8px; color: #fff; font-size: 0.95rem; }
    .comment { border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px; margin-bottom: 8px; }
    .comment small { color: #93c5fd; }
    .comment-actions { margin-top: 8px; display: flex; gap: 8px; }
    form { display: grid; gap: 8px; }
    input, textarea { background: rgba(0,0,0,0.3); border: 1px solid rgba(0,255,255,0.2); border-radius: 8px; color: #fff; padding: 10px; }
    button { background: #00bcd4; color: #041a20; border: none; border-radius: 8px; padding: 8px 10px; font-weight: 700; cursor: pointer; }
    button.secondary { background: #334155; color: #e2e8f0; }
    button.danger { background: #b91c1c; color: #fff; }
    .status { color: #93c5fd; padding: 8px 0; }
    .status.error, .error { color: #fca5a5; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(2,6,23,0.72); display: flex; align-items: center; justify-content: center; z-index: 2200; padding: 16px; }
    .modal { width: min(640px, 100%); background: #0f172a; border: 1px solid rgba(0,255,255,0.25); border-radius: 12px; padding: 14px; }
    .modal h3 { margin: 0 0 10px; color: #fff; }
  `]
})
export class CommunityDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly communitiesService = inject(CommunitiesService);
  private readonly postsService = inject(PostsService);
  private readonly commentsService = inject(CommentsService);
  private readonly votesService = inject(VotesService);
  private readonly fb = inject(FormBuilder);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  community: Community | null = null;
  posts: Post[] = [];
  commentsByPost: Record<number, Comment[]> = {};
  votesByPost: Record<number, Vote[]> = {};
  commentForms: Record<number, ReturnType<FormBuilder['group']>> = {};

  loading = false;
  errorMessage = '';

  showPostDialog = false;
  dialogSubmitting = false;
  dialogError = '';
  editingPost: Post | null = null;
  communityId = 0;
  readonly users = FORUM_TEST_USERS;
  activeUser: ForumTestUser = 'alice';
  editingCommentId: number | null = null;
  pendingDelete: { type: 'post' | 'comment' | null; postId?: number; commentId?: number } = { type: null };
  deleteSubmitting = false;

  postForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    content: ['', [Validators.required]]
  });

  editCommentForm = this.fb.group({
    content: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  loadAll(): void {
    if (!this.communityId) {
      this.errorMessage = 'Invalid community id.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    this.communitiesService.getById(this.communityId).subscribe({
      next: (community) => this.ngZone.run(() => {
        this.community = community;
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.loading = false;
        this.cdr.detectChanges();
      })
    });

    this.postsService.getAll(this.communityId).subscribe({
      next: (posts) => this.ngZone.run(() => {
        this.posts = posts;
        this.initializePerPostState(posts);
        this.loading = false;
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.loading = false;
        this.cdr.detectChanges();
      })
    });
  }

  initializePerPostState(posts: Post[]): void {
    for (const post of posts) {
      this.commentForms[post.id] = this.fb.group({
        content: ['', [Validators.required]]
      });
      this.loadComments(post.id);
      this.loadVotes(post.id);
    }
  }

  onUserChange(user: string): void {
    if ((this.users as readonly string[]).includes(user)) {
      this.activeUser = user as ForumTestUser;
      if (this.editingPost && !this.canManagePost(this.editingPost)) {
        this.closePostDialog();
      }
      if (this.editingCommentId != null) {
        const comment = Object.values(this.commentsByPost).flat().find(c => c.id === this.editingCommentId);
        if (comment) {
          const post = this.posts.find(p => p.id === comment.postId || p.id === (comment.post?.id ?? -1));
          if (post && !this.canManageComment(comment, post)) {
            this.cancelEditComment();
          }
        }
      }
    }
  }

  canManagePost(post: Post): boolean {
    return post.author === this.activeUser;
  }

  canManageComment(comment: Comment, post: Post): boolean {
    return comment.author === this.activeUser || post.author === this.activeUser;
  }

  loadComments(postId: number): void {
    this.commentsService.getAll(postId).subscribe({
      next: (comments) => this.ngZone.run(() => {
        this.commentsByPost[postId] = comments;
        this.cdr.detectChanges();
      })
    });
  }

  loadVotes(postId: number): void {
    this.votesService.getAll(postId).subscribe({
      next: (votes) => this.ngZone.run(() => {
        this.votesByPost[postId] = votes;
        this.cdr.detectChanges();
      })
    });
  }

  getPostScore(postId: number): number {
    return (this.votesByPost[postId] || []).reduce((sum, v) => sum + (v.value || 0), 0);
  }

  openPostDialog(): void {
    this.editingPost = null;
    this.dialogError = '';
    this.postForm.reset({ title: '', content: '' });
    this.showPostDialog = true;
  }

  startEditPost(post: Post): void {
    if (!this.canManagePost(post)) {
      return;
    }
    this.editingPost = post;
    this.dialogError = '';
    this.postForm.patchValue({
      title: post.title,
      content: post.content
    });
    this.showPostDialog = true;
  }

  closePostDialog(): void {
    if (this.dialogSubmitting) return;
    this.showPostDialog = false;
    this.editingPost = null;
    this.dialogError = '';
  }

  savePost(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }
    this.dialogSubmitting = true;
    const form = this.postForm.getRawValue();

    if (this.editingPost) {
      this.postsService.update(this.editingPost.id, {
        title: form.title || '',
        content: form.content || '',
        communityId: this.communityId
      }, this.activeUser).subscribe({
        next: (updated) => this.ngZone.run(() => {
          this.posts = this.posts.map(p => p.id === updated.id ? updated : p);
          this.dialogSubmitting = false;
          this.closePostDialog();
          this.cdr.detectChanges();
        }),
        error: (err) => this.ngZone.run(() => {
          this.dialogError = this.getError(err);
          this.dialogSubmitting = false;
          this.cdr.detectChanges();
        })
      });
      return;
    }

    this.postsService.create({
      communityId: this.communityId,
      title: form.title || '',
      content: form.content || '',
      author: this.activeUser
    }).subscribe({
      next: (created) => this.ngZone.run(() => {
        this.posts = [created, ...this.posts];
        this.initializePerPostState([created]);
        this.dialogSubmitting = false;
        this.closePostDialog();
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.dialogError = this.getError(err);
        this.dialogSubmitting = false;
        this.cdr.detectChanges();
      })
    });
  }

  deletePost(id: number): void {
    const post = this.posts.find(p => p.id === id);
    if (!post || !this.canManagePost(post)) {
      return;
    }
    this.pendingDelete = { type: 'post', postId: id };
  }

  deleteComment(postId: number, commentId: number): void {
    const post = this.posts.find(p => p.id === postId);
    const comment = (this.commentsByPost[postId] || []).find(c => c.id === commentId);
    if (!post || !comment || !this.canManageComment(comment, post)) {
      return;
    }
    this.pendingDelete = { type: 'comment', postId, commentId };
  }

  confirmDelete(): void {
    if (!this.pendingDelete.type) return;
    this.deleteSubmitting = true;
    if (this.pendingDelete.type === 'post' && this.pendingDelete.postId != null) {
      this.executeDeletePost(this.pendingDelete.postId);
      return;
    }
    if (
      this.pendingDelete.type === 'comment' &&
      this.pendingDelete.postId != null &&
      this.pendingDelete.commentId != null
    ) {
      this.executeDeleteComment(this.pendingDelete.postId, this.pendingDelete.commentId);
      return;
    }
    this.deleteSubmitting = false;
    this.pendingDelete = { type: null };
  }

  cancelDelete(): void {
    if (this.deleteSubmitting) return;
    this.pendingDelete = { type: null };
  }

  private executeDeletePost(id: number): void {
    this.postsService.delete(id, this.activeUser).subscribe({
      next: () => this.ngZone.run(() => {
        this.posts = this.posts.filter(p => p.id !== id);
        delete this.commentsByPost[id];
        delete this.votesByPost[id];
        delete this.commentForms[id];
        this.deleteSubmitting = false;
        this.pendingDelete = { type: null };
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.deleteSubmitting = false;
        this.cdr.detectChanges();
      })
    });
  }

  addComment(postId: number): void {
    const form = this.commentForms[postId];
    if (!form || form.invalid) {
      form?.markAllAsTouched();
      return;
    }
    const value = form.getRawValue();
    this.commentsService.create({
      post: { id: postId },
      author: this.activeUser,
      content: value.content || ''
    }).subscribe({
      next: (created) => this.ngZone.run(() => {
        this.commentsByPost[postId] = [created, ...(this.commentsByPost[postId] || [])];
        form.reset({ content: '' });
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.cdr.detectChanges();
      })
    });
  }

  startEditComment(comment: Comment): void {
    const post = this.posts.find(p => p.id === comment.postId || p.id === (comment.post?.id ?? -1));
    if (!post || !this.canManageComment(comment, post)) {
      return;
    }
    this.editingCommentId = comment.id;
    this.editCommentForm.patchValue({
      content: comment.content
    });
  }

  cancelEditComment(): void {
    this.editingCommentId = null;
    this.editCommentForm.reset({ content: '' });
  }

  saveEditedComment(postId: number, commentId: number): void {
    if (this.editCommentForm.invalid) {
      this.editCommentForm.markAllAsTouched();
      return;
    }

    const value = this.editCommentForm.getRawValue();
    this.commentsService.update(commentId, {
      post: { id: postId },
      content: value.content || ''
    }, this.activeUser).subscribe({
      next: (updated) => this.ngZone.run(() => {
        this.commentsByPost[postId] = (this.commentsByPost[postId] || []).map((comment) =>
          comment.id === updated.id ? updated : comment
        );
        this.cancelEditComment();
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.cdr.detectChanges();
      })
    });
  }

  private executeDeleteComment(postId: number, commentId: number): void {
    this.commentsService.delete(commentId, this.activeUser).subscribe({
      next: () => this.ngZone.run(() => {
        this.commentsByPost[postId] = (this.commentsByPost[postId] || []).filter((comment) => comment.id !== commentId);
        if (this.editingCommentId === commentId) {
          this.cancelEditComment();
        }
        this.deleteSubmitting = false;
        this.pendingDelete = { type: null };
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.deleteSubmitting = false;
        this.cdr.detectChanges();
      })
    });
  }

  vote(postId: number, value: 1 | -1): void {
    this.votesService.createOrUpdate({
      post: { id: postId },
      username: this.activeUser,
      value
    }).subscribe({
      next: () => this.loadVotes(postId),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.cdr.detectChanges();
      })
    });
  }

  clearVote(postId: number): void {
    const existing = (this.votesByPost[postId] || []).find((v) => v.username === this.activeUser);
    if (!existing?.id) return;
    this.votesService.delete(existing.id).subscribe({
      next: () => this.loadVotes(postId),
      error: (err) => this.ngZone.run(() => {
        this.errorMessage = this.getError(err);
        this.cdr.detectChanges();
      })
    });
  }

  private getError(err: any): string {
    if (typeof err?.error?.error === 'string') return err.error.error;
    return 'Request failed. Verify gateway/content-service/community-service are running.';
  }
}

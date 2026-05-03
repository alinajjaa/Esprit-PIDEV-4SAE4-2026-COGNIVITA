import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { UserService, User } from '../../../services/user.service';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-user-profile',
  templateUrl: './user-profile.html',
  styleUrls: ['./user-profile.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, DatePipe]
})
export class UserProfileComponent implements OnInit {

  currentUser!: User;
  originalUser!: User;
  saving        = false;
  statusMessage = '';
  isSuccess     = false;
  hasChanges    = false;
  photoPreview  = '';
  selectedFile: File | null = null;
  zoomOpen      = false;

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  private mark(): void {
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    const local = this.userService.getCurrentUser();
    if (local) this.initUser(local);

    this.userService.getSession().subscribe({
      next: (fresh) => { this.initUser(fresh); this.mark(); },
      error: () => {}
    });
  }

  private initUser(user: User): void {
    this.currentUser  = { ...user };
    this.originalUser = { ...user };
    this.photoPreview = user.photoUrl || '';
    this.hasChanges   = false;
  }

  onFieldChange(): void {
    this.hasChanges =
      this.currentUser.fullName !== this.originalUser.fullName ||
      this.currentUser.email    !== this.originalUser.email    ||
      this.currentUser.photoUrl !== this.originalUser.photoUrl;
    this.mark();
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (!file) return;
    this.selectedFile = file;
    this.hasChanges   = true;
    const reader      = new FileReader();
    reader.onload     = (e: any) => {
      this.photoPreview = e.target.result;
      this.mark();
    };
    reader.readAsDataURL(file);
  }

  removePhoto(): void {
    this.photoPreview              = '';
    this.selectedFile              = null;
    this.currentUser.photoUrl      = '';
    this.hasChanges                = true;
    const input = document.getElementById('photoUpload') as HTMLInputElement;
    if (input) input.value = '';
    this.mark();
  }

  triggerUpload(): void {
    const input = document.getElementById('photoUpload') as HTMLInputElement;
    if (input) input.click();
  }

  saveProfile(): void {
    if (this.selectedFile) {
      this.uploadImage();
    } else if (this.hasChanges) {
      this.doSaveProfile();
    }
  }

  uploadImage(): void {
    if (!this.selectedFile) return;
    this.saving        = true;
    this.statusMessage = '';
    this.mark();

    this.userService.uploadProfileImage(this.currentUser.id, this.selectedFile).subscribe({
      next: (imageUrl: string) => {
        this.currentUser.photoUrl = imageUrl;
        this.photoPreview         = imageUrl;
        this.selectedFile         = null;
        this.hasChanges           = true;
        this.doSaveProfile();
      },
      error: () => {
        this.saving        = false;
        this.statusMessage = 'Image upload failed.';
        this.isSuccess     = false;
        this.mark();
      }
    });
  }

  private doSaveProfile(): void {
    this.saving        = true;
    this.statusMessage = '';
    this.mark();

    this.userService.updateProfile(this.currentUser).subscribe({
      next: (updated) => {
        this.saving        = false;
        this.isSuccess     = true;
        this.statusMessage = 'Profile updated successfully!';
        this.originalUser  = { ...updated };
        this.currentUser   = { ...updated };
        this.photoPreview  = updated.photoUrl || '';
        this.hasChanges    = false;
        this.mark();
        setTimeout(() => { this.statusMessage = ''; this.mark(); }, 4000);
      },
      error: (err) => {
        this.saving        = false;
        this.isSuccess     = false;
        this.statusMessage = err?.error?.message || 'Error updating profile.';
        this.mark();
        setTimeout(() => { this.statusMessage = ''; this.mark(); }, 4000);
      }
    });
  }

  resetChanges(): void {
    this.currentUser  = { ...this.originalUser };
    this.photoPreview = this.originalUser.photoUrl || '';
    this.selectedFile = null;
    this.hasChanges   = false;
    this.statusMessage = '';
    const input = document.getElementById('photoUpload') as HTMLInputElement;
    if (input) input.value = '';
    this.mark();
  }

  openZoom(): void {
    if (this.photoPreview) {
      this.zoomOpen = true;
      document.body.style.overflow = 'hidden';
      this.mark();
    }
  }

  closeZoom(): void {
    this.zoomOpen = false;
    document.body.style.overflow = '';
    this.mark();
  }

  changePhoto(): void {
    this.closeZoom();
    setTimeout(() => {
      const input = document.getElementById('photoUpload') as HTMLInputElement;
      if (input) input.click();
    }, 300);
  }

  downloadPhoto(): void {
    if (!this.photoPreview) return;
    const fileName = `cognivita-profile-${this.currentUser?.fullName ?? 'photo'}.jpg`;

    if (this.photoPreview.startsWith('data:')) {
      const link = document.createElement('a');
      link.href  = this.photoPreview;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      return;
    }

    fetch(this.photoPreview)
      .then(res => res.blob())
      .then(blob => {
        const blobUrl = URL.createObjectURL(blob);
        const link    = document.createElement('a');
        link.href     = blobUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(blobUrl);
      })
      .catch(() => window.open(this.photoPreview, '_blank'));
  }
}
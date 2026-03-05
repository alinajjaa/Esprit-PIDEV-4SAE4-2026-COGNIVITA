import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { UserService, User } from '../../../services/user.service';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-user-profile',
  templateUrl: './user-profile.html',
  styleUrls: ['./user-profile.css'],
  imports: [CommonModule, FormsModule, DatePipe]
})
export class UserProfileComponent implements OnInit {

  currentUser!: User;
  originalUser!: User;
  saving = false;
  statusMessage = '';
  isSuccess = false;
  hasChanges = false;
  photoPreview = '';
  selectedFile: File | null = null;

  // URL de base pour afficher les images depuis le backend

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const local = this.userService.getCurrentUser();
    if (local) {
      this.initUser(local);
    }

    this.userService.getSession().subscribe({
      next: (fresh) => {
        this.initUser(fresh);
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  private initUser(user: User): void {
    this.currentUser = { ...user };
    this.originalUser = { ...user };
    // Charger la photo existante depuis la BDD
    this.photoPreview = this.getPhotoUrl(user.photoUrl);
    this.hasChanges = false;
  }

  // Construire l'URL complète de la photo
getPhotoUrl(photoUrl: string): string {
  if (!photoUrl) return '';
  return photoUrl;
}
  onFieldChange(): void {
    this.hasChanges =
      this.currentUser.fullName !== this.originalUser.fullName ||
      this.currentUser.email !== this.originalUser.email ||
      this.currentUser.photoUrl !== this.originalUser.photoUrl;
    this.cdr.detectChanges();
  }

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0];
    if (this.selectedFile) {
      this.hasChanges = true;
      // Aperçu immédiat en base64 avant upload
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.photoPreview = e.target.result;
        this.cdr.detectChanges();
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  saveProfile(): void {
    if (this.selectedFile) {
      this.uploadImage(); // Upload image d'abord
    } else if (this.hasChanges) {
      this.doSaveProfile(); // Sauvegarde directe si pas de fichier
    }
  }
uploadImage(): void {
  if (!this.selectedFile) return;

  this.saving = true;
  this.statusMessage = '';
  this.cdr.detectChanges();

  // ✅ Utiliser userService directement
  this.userService.uploadProfileImage(this.currentUser.id, this.selectedFile).subscribe({
    next: (imageUrl: string) => {
      this.currentUser.photoUrl = imageUrl;
      this.photoPreview = imageUrl;
      this.selectedFile = null;
      this.hasChanges = true;
      this.doSaveProfile();
    },
    error: () => {
      this.saving = false;
      this.statusMessage = 'Image upload failed.';
      this.isSuccess = false;
      this.cdr.detectChanges();
    }
  });
}

  private doSaveProfile(): void {
    this.saving = true;
    this.statusMessage = '';
    this.cdr.detectChanges();

    this.userService.updateProfile(this.currentUser).subscribe({
      next: (updated) => {
        this.saving = false;
        this.isSuccess = true;
        this.statusMessage = 'Profile updated successfully!';
        this.originalUser = { ...updated };
        this.currentUser = { ...updated };
        // Mettre à jour la photo affichée
        this.photoPreview = this.getPhotoUrl(updated.photoUrl);
        this.hasChanges = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.statusMessage = '';
          this.cdr.detectChanges();
        }, 4000);
      },
      error: (err) => {
        this.saving = false;
        this.isSuccess = false;
        this.statusMessage = err?.error?.message || 'Error updating profile.';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.statusMessage = '';
          this.cdr.detectChanges();
        }, 4000);
      }
    });
  }

  resetChanges(): void {
    this.currentUser = { ...this.originalUser };
    this.photoPreview = this.getPhotoUrl(this.originalUser.photoUrl);
    this.selectedFile = null;
    this.hasChanges = false;
    this.statusMessage = '';
    this.cdr.detectChanges();
  }


  // ✅ Ajouter ces propriétés
zoomOpen = false;

// ✅ Ouvrir zoom
openZoom(): void {
  if (this.photoPreview) {
    this.zoomOpen = true;
    document.body.style.overflow = 'hidden'; // bloquer scroll
  }
}

// ✅ Fermer zoom
closeZoom(): void {
  this.zoomOpen = false;
  document.body.style.overflow = '';
}

// ✅ Télécharger la photo
downloadPhoto(): void {
  if (!this.photoPreview) return;

  const fileName = `cognivita-profile-${this.currentUser?.fullName ?? 'photo'}.jpg`;

  if (this.photoPreview.startsWith('data:')) {
    const link = document.createElement('a');
    link.href = this.photoPreview;
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
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(blobUrl);
    })
    .catch(() => {
      window.open(this.photoPreview, '_blank');
    });
}
// ✅ Ouvrir input directement depuis le bouton sous l'avatar
triggerUpload(): void {
  const input = document.getElementById('photoUpload') as HTMLInputElement;
  if (input) input.click();
}

// ✅ Fermer modal puis ouvrir input
changePhoto(): void {
  this.closeZoom();
  setTimeout(() => {
    const input = document.getElementById('photoUpload') as HTMLInputElement;
    if (input) input.click();
  }, 300);
}
}
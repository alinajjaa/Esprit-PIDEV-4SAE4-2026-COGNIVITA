import { Component, Input, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, User } from '../../services/user.service';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users-list.html',
  styleUrls: ['./users-list.css']
})
export class UsersListComponent implements OnChanges {

  @Input() users: User[] = [];

  filteredUsers: User[] = [];
  searchQuery = '';
  roleFilter = 'ALL';
  statusFilter = 'ALL';
  error = '';

  // Modals
  userToDelete: User | null = null;
  userToBlock: User | null = null;

  // Loading states
  promotingId: number | null = null;
  blockingId: number | null = null;

  // Pagination
  currentPage = 1;
  pageSize = 5;

  // Toast
  toast: { message: string; type: 'success' | 'error' | 'warning' } | null = null;
  private toastTimeout: any;

  // Inline role edit
  editingRoleId: number | null = null;

  private avatarColors = [
    '#00ffff', '#ff00ff', '#00ff88', '#ffd700',
    '#ff6b6b', '#4ecdc4', '#a855f7', '#f97316'
  ];

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['users']) {
      this.applyFilters();
    }
  }

  // ── STATS ──────────────────────────────────────
  get totalUsers(): number { return this.users.length; }
  get totalAdmins(): number { return this.users.filter(u => u.role === 'ADMIN').length; }
  get totalDoctors(): number { return this.users.filter(u => u.role === 'DOCTOR').length; }
  get totalBlocked(): number { return this.users.filter(u => u.blocked).length; }

  // ── FILTERS ────────────────────────────────────
  applyFilters(): void {
    let result = [...this.users];
    const q = this.searchQuery.toLowerCase().trim();

    if (q) {
      result = result.filter(u =>
        u.fullName?.toLowerCase().includes(q) ||
        u.email?.toLowerCase().includes(q) ||
        u.role?.toLowerCase().includes(q)
      );
    }

    if (this.roleFilter !== 'ALL') {
      result = result.filter(u => u.role === this.roleFilter);
    }

    if (this.statusFilter === 'ACTIVE') {
      result = result.filter(u => !u.blocked);
    } else if (this.statusFilter === 'BLOCKED') {
      result = result.filter(u => u.blocked);
    }

    this.filteredUsers = result;
    this.currentPage = 1;
    this.cdr.detectChanges();
  }

  filterUsers(): void { this.applyFilters(); }

  // ── PAGINATION ─────────────────────────────────
  get totalPages(): number { return Math.ceil(this.filteredUsers.length / this.pageSize); }
  get paginatedUsers(): User[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredUsers.slice(start, start + this.pageSize);
  }
  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.cdr.detectChanges();
    }
  }

  // ── TOAST ──────────────────────────────────────
  showToast(message: string, type: 'success' | 'error' | 'warning'): void {
    clearTimeout(this.toastTimeout);
    this.toast = { message, type };
    this.cdr.detectChanges();
    this.toastTimeout = setTimeout(() => {
      this.toast = null;
      this.cdr.detectChanges();
    }, 3500);
  }

  // ── INLINE ROLE EDIT ───────────────────────────
  startEditRole(user: User): void { this.editingRoleId = user.id; }
  cancelEditRole(): void { this.editingRoleId = null; }

  changeRole(user: User, newRole: string): void {
    if (newRole === user.role) { this.editingRoleId = null; return; }
    this.promotingId = user.id;
    this.editingRoleId = null;

    this.userService.update(user.id, { ...user, role: newRole }).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.promotingId = null;
        this.showToast(`${updated.fullName} is now ${newRole}`, 'success');
        this.applyFilters();
      },
      error: (err) => {
        this.promotingId = null;
        this.showToast(err?.error?.message || 'Failed to change role', 'error');
      }
    });
  }

  // ── BLOCK / UNBLOCK ────────────────────────────
  confirmBlock(user: User): void { this.userToBlock = user; }
  cancelBlock(): void { this.userToBlock = null; }

  blockUser(user: User): void {
    this.blockingId = user.id;
    this.userToBlock = null;

    this.userService.blockUser(user.id).subscribe({
      next: (updated) => {
        this.replaceUser({ ...updated, blocked: true });
        this.blockingId = null;
        this.showToast(`${user.fullName} has been blocked`, 'warning');
        this.applyFilters();
      },
      error: (err) => {
        this.blockingId = null;
        this.showToast(err?.error?.message || 'Failed to block user', 'error');
      }
    });
  }

  unblockUser(user: User): void {
    this.blockingId = user.id;

    this.userService.unblockUser(user.id).subscribe({
      next: (updated) => {
        this.replaceUser({ ...updated, blocked: false });
        this.blockingId = null;
        this.showToast(`${user.fullName} has been unblocked`, 'success');
        this.applyFilters();
      },
      error: (err) => {
        this.blockingId = null;
        this.showToast(err?.error?.message || 'Failed to unblock user', 'error');
      }
    });
  }

  // ── DELETE ─────────────────────────────────────
  confirmDelete(user: User): void { this.userToDelete = user; }
  cancelDelete(): void { this.userToDelete = null; }

  deleteUser(userId: number): void {
    const name = this.userToDelete?.fullName;
    this.userService.delete(userId).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== userId);
        this.userToDelete = null;
        this.showToast(`${name} has been deleted`, 'warning');
        this.applyFilters();
      },
      error: (err) => {
        this.userToDelete = null;
        this.showToast(err?.error?.message || 'Failed to delete user', 'error');
      }
    });
  }

  // ── EXPORT PDF ─────────────────────────────────
  async exportPdf(): Promise<void> {
    const { jsPDF } = await import('jspdf');
    const autoTable = (await import('jspdf-autotable')).default;

    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const pageH = doc.internal.pageSize.getHeight();
    const now = new Date();

    // ── Background ──
    doc.setFillColor(10, 10, 26);
    doc.rect(0, 0, pageW, pageH, 'F');

    // ── Header bar ──
    doc.setFillColor(0, 30, 40);
    doc.rect(0, 0, pageW, 28, 'F');

    // ── Accent line ──
    doc.setDrawColor(0, 255, 255);
    doc.setLineWidth(0.8);
    doc.line(0, 28, pageW, 28);

    // ── Title ──
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.setTextColor(0, 255, 255);
    doc.text('COGNIVITA', 14, 12);

    doc.setFontSize(10);
    doc.setTextColor(180, 220, 255);
    doc.text('Users Management Report', 14, 20);

    // ── Date ──
    doc.setFontSize(8);
    doc.setTextColor(100, 160, 200);
    doc.text(`Generated: ${now.toLocaleDateString()} ${now.toLocaleTimeString()}`, pageW - 14, 12, { align: 'right' });
    doc.text(`Total: ${this.filteredUsers.length} users`, pageW - 14, 20, { align: 'right' });

    // ── Stats row ──
    const stats = [
      { label: 'Total Users', value: this.totalUsers, color: [0, 255, 255] },
      { label: 'Admins', value: this.totalAdmins, color: [255, 215, 0] },
      { label: 'Blocked', value: this.totalBlocked, color: [255, 50, 80] },
    ];

    const boxW = 50, boxH = 16, startX = 14, startY = 34, gap = 56;
    stats.forEach((s, i) => {
      const x = startX + i * gap;
      doc.setFillColor(0, 20, 30);
      doc.setDrawColor(s.color[0], s.color[1], s.color[2]);
      doc.setLineWidth(0.4);
      doc.roundedRect(x, startY, boxW, boxH, 3, 3, 'FD');

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(14);
      doc.setTextColor(s.color[0], s.color[1], s.color[2]);
      doc.text(String(s.value), x + boxW / 2, startY + 9, { align: 'center' });

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7);
      doc.setTextColor(100, 160, 200);
      doc.text(s.label, x + boxW / 2, startY + 14, { align: 'center' });
    });

    // ── Table ──
    const tableData = this.filteredUsers.map(u => [

      u.fullName || '-',
      u.email,
      u.role,
      u.blocked ? 'Blocked' : 'Active',
      u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'
    ]);

    autoTable(doc, {
      startY: 56,
      head: [[ 'Full Name', 'Email', 'Role', 'Status', 'Created At']],
      body: tableData,
      theme: 'plain',
      styles: {
        font: 'helvetica',
        fontSize: 9,
        textColor: [176, 224, 255],
        cellPadding: { top: 4, bottom: 4, left: 6, right: 6 },
        lineColor: [0, 50, 70],
        lineWidth: 0.3,
        fillColor: [10, 10, 26],
      },
      headStyles: {
        fillColor: [0, 30, 50],
        textColor: [0, 255, 255],
        fontStyle: 'bold',
        fontSize: 9,
        lineColor: [0, 255, 255],
        lineWidth: 0.4,
      },
      alternateRowStyles: {
        fillColor: [0, 15, 25],
      },
didParseCell: (data: any) => {
  // Status — colonne index 3 (was 4)
  if (data.column.index === 3 && data.section === 'body') {
    if (data.cell.raw === 'Blocked') {
      data.cell.styles.textColor = [255, 50, 80];
    } else {
      data.cell.styles.textColor = [0, 255, 136];
    }
  }
  // Role — colonne index 2 (was 3)
  if (data.column.index === 2 && data.section === 'body') {
    if (data.cell.raw === 'ADMIN') data.cell.styles.textColor = [255, 215, 0];
    else data.cell.styles.textColor = [0, 200, 255];
  }
},
    });

    // ── Footer ──
    const finalY = (doc as any).lastAutoTable?.finalY || pageH - 20;
    doc.setDrawColor(0, 255, 255);
    doc.setLineWidth(0.3);
    doc.line(14, pageH - 10, pageW - 14, pageH - 10);
    doc.setFontSize(7);
    doc.setTextColor(60, 120, 160);
    doc.text('COGNIVITA — Confidential Report', 14, pageH - 6);
    doc.text(`Page 1`, pageW - 14, pageH - 6, { align: 'right' });

    doc.save(`cognivita-users-${now.toISOString().slice(0, 10)}.pdf`);
    this.showToast('PDF exported successfully!', 'success');
  }

  // ── HELPERS ────────────────────────────────────
  private replaceUser(updated: User): void {
    this.users = this.users.map(u => u.id === updated.id ? { ...updated } : u);
  }

  onImgError(event: Event): void {
    (event.target as HTMLElement).style.display = 'none';
  }

  getAvatarColor(name: string | undefined): string {
    if (!name) return this.avatarColors[0];
    return this.avatarColors[name.charCodeAt(0) % this.avatarColors.length];
  }


  zoomOpen = false;
zoomedPhotoUrl = '';
zoomedUserName = '';

openZoom(user: any): void {
  this.zoomedPhotoUrl = user.photoUrl;
  this.zoomedUserName = user.fullName;
  this.zoomOpen = true;
  document.body.style.overflow = 'hidden';
}

closeZoom(): void {
  this.zoomOpen = false;
  this.zoomedPhotoUrl = '';
  document.body.style.overflow = '';
}

downloadPhoto(): void {
  if (!this.zoomedPhotoUrl) return;

  const fileName = `cognivita-profile-${this.zoomedUserName ?? 'photo'}.jpg`;

  fetch(this.zoomedPhotoUrl)
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
      window.open(this.zoomedPhotoUrl, '_blank');
    });
}
}
//1497995858558203
//d3e6322456486e9a4f6bbb0551f2da2c
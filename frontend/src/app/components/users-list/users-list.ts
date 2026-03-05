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
  searchQuery    = '';
  roleFilter     = 'ALL';
  statusFilter   = 'ALL';
  searchFocused  = false;   // ← new: drives search-box focused class
  error          = '';

  // Modals
  userToDelete: User | null = null;
  userToBlock:  User | null = null;

  // Loading states
  promotingId: number | null = null;
  blockingId:  number | null = null;

  // Pagination
  currentPage = 1;
  pageSize    = 8;

  // Toast
  toast: { message: string; type: 'success' | 'error' | 'warning' } | null = null;
  private toastTimeout: any;

  // Inline role edit
  editingRoleId: number | null = null;

  // Zoom modal
  zoomOpen       = false;
  zoomedPhotoUrl = '';
  zoomedUserName = '';

  private readonly avatarColors = [
    '#22d3ee', '#f59e0b', '#10b981', '#818cf8',
    '#f43f5e', '#fb923c', '#a78bfa', '#34d399'
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
  get totalUsers():   number { return this.users.length; }
  get totalAdmins():  number { return this.users.filter(u => u.role === 'ADMIN').length; }
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
    this.currentPage   = 1;
    this.cdr.detectChanges();
  }

  filterUsers(): void { this.applyFilters(); }

  // ── PAGINATION ─────────────────────────────────
  get totalPages(): number {
    return Math.ceil(this.filteredUsers.length / this.pageSize);
  }
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
  startEditRole(user: User): void  { this.editingRoleId = user.id; }
  cancelEditRole(): void           { this.editingRoleId = null; }

  changeRole(user: User, newRole: string): void {
    if (newRole === user.role) { this.editingRoleId = null; return; }
    this.promotingId  = user.id;
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
  cancelBlock():  void           { this.userToBlock = null; }

  blockUser(user: User): void {
    this.blockingId  = user.id;
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
  cancelDelete():  void           { this.userToDelete = null; }

  deleteUser(userId: number): void {
    const name = this.userToDelete?.fullName;
    this.userService.delete(userId).subscribe({
      next: () => {
        this.users        = this.users.filter(u => u.id !== userId);
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
    const { jsPDF }   = await import('jspdf');
    const autoTable   = (await import('jspdf-autotable')).default;

    const doc   = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const pageH = doc.internal.pageSize.getHeight();
    const now   = new Date();

    // Background
    doc.setFillColor(8, 12, 20);
    doc.rect(0, 0, pageW, pageH, 'F');

    // Header bar
    doc.setFillColor(13, 20, 32);
    doc.rect(0, 0, pageW, 28, 'F');

    // Accent line
    doc.setDrawColor(34, 211, 238);
    doc.setLineWidth(0.6);
    doc.line(0, 28, pageW, 28);

    // Brand
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.setTextColor(34, 211, 238);
    doc.text('COGNIVITA', 14, 11);

    doc.setFontSize(9);
    doc.setTextColor(140, 180, 220);
    doc.text('Users Management Report', 14, 20);

    // Date
    doc.setFontSize(8);
    doc.setTextColor(80, 130, 170);
    doc.text(`Generated: ${now.toLocaleDateString()} ${now.toLocaleTimeString()}`, pageW - 14, 11, { align: 'right' });
    doc.text(`${this.filteredUsers.length} users exported`, pageW - 14, 20, { align: 'right' });

    // Stats cards
    const stats = [
      { label: 'Total Users', value: this.totalUsers,   color: [34, 211, 238] as [number,number,number] },
      { label: 'Admins',      value: this.totalAdmins,  color: [245, 158, 11] as [number,number,number] },
      { label: 'Blocked',     value: this.totalBlocked, color: [244, 63, 94]  as [number,number,number] },
    ];
    const bW = 48, bH = 15, sX = 14, sY = 34, gap = 54;
    stats.forEach((s, i) => {
      const x = sX + i * gap;
      doc.setFillColor(0, 15, 25);
      doc.setDrawColor(...s.color);
      doc.setLineWidth(0.3);
      doc.roundedRect(x, sY, bW, bH, 2.5, 2.5, 'FD');

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(13);
      doc.setTextColor(...s.color);
      doc.text(String(s.value), x + bW / 2, sY + 8.5, { align: 'center' });

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(6.5);
      doc.setTextColor(80, 130, 170);
      doc.text(s.label, x + bW / 2, sY + 13, { align: 'center' });
    });

    // Table
    const rows = this.filteredUsers.map(u => [
      u.fullName || '-',
      u.email    || '-',
      u.role     || '-',
      u.blocked ? 'Blocked' : 'Active',
      u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'
    ]);

    autoTable(doc, {
      startY: 55,
      head:   [['Full Name', 'Email', 'Role', 'Status', 'Created At']],
      body:   rows,
      theme:  'plain',
      styles: {
        font: 'helvetica', fontSize: 9,
        textColor:   [176, 224, 255],
        cellPadding: { top: 4, bottom: 4, left: 6, right: 6 },
        lineColor:   [0, 40, 60],
        lineWidth:   0.25,
        fillColor:   [8, 12, 20],
      },
      headStyles: {
        fillColor:  [13, 24, 40],
        textColor:  [34, 211, 238],
        fontStyle:  'bold',
        fontSize:   8.5,
        lineColor:  [34, 211, 238],
        lineWidth:  0.4,
      },
      alternateRowStyles: { fillColor: [0, 10, 18] },
      didParseCell: (data: any) => {
        if (data.section !== 'body') return;
        if (data.column.index === 3) {
          data.cell.styles.textColor = data.cell.raw === 'Blocked'
            ? [244, 63, 94] : [16, 185, 129];
        }
        if (data.column.index === 2) {
          data.cell.styles.textColor = data.cell.raw === 'ADMIN'
            ? [245, 158, 11] : [34, 211, 238];
        }
      },
    });

    // Footer
    doc.setDrawColor(34, 211, 238);
    doc.setLineWidth(0.25);
    doc.line(14, pageH - 9, pageW - 14, pageH - 9);
    doc.setFontSize(7);
    doc.setTextColor(50, 100, 140);
    doc.text('COGNIVITA — Confidential Report', 14, pageH - 5);
    doc.text('Page 1', pageW - 14, pageH - 5, { align: 'right' });

    doc.save(`cognivita-users-${now.toISOString().slice(0, 10)}.pdf`);
    this.showToast('PDF exported successfully!', 'success');
  }

  // ── ZOOM MODAL ─────────────────────────────────
  openZoom(user: any): void {
    this.zoomedPhotoUrl = user.photoUrl;
    this.zoomedUserName = user.fullName;
    this.zoomOpen       = true;
    document.body.style.overflow = 'hidden';
  }

  closeZoom(): void {
    this.zoomOpen       = false;
    this.zoomedPhotoUrl = '';
    document.body.style.overflow = '';
  }

  downloadPhoto(): void {
    if (!this.zoomedPhotoUrl) return;
    const fileName = `cognivita-profile-${this.zoomedUserName ?? 'photo'}.jpg`;

    fetch(this.zoomedPhotoUrl)
      .then(res => res.blob())
      .then(blob => {
        const url  = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href  = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      })
      .catch(() => window.open(this.zoomedPhotoUrl, '_blank'));
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
}
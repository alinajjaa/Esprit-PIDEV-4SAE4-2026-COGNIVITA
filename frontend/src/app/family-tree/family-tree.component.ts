import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  FamilyTreeService, FamilyMember, FamilyTreeNode,
  HereditaryRiskAnalysis, Relationship, FamilyGender
} from './family-tree.service';

@Component({
  selector: 'app-family-tree',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './family-tree.component.html',
  styleUrls: ['./family-tree.component.css']
})
export class FamilyTreeComponent implements OnInit {

  userId = 1;
  userName = '';
  activeTab: 'tree' | 'members' | 'analysis' = 'tree';

  members: FamilyMember[] = [];
  treeNodes: FamilyTreeNode[] = [];
  riskAnalysis: HereditaryRiskAnalysis | null = null;
  stats: any = null;

  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showForm = false;
  isEditing = false;
  editingId: number | null = null;
  generatingPdf = false;

  showPdfDialog = false; // kept for template compatibility (PDF modal removed)

  relationships: Relationship[] = [
    'FATHER', 'MOTHER',
    'PATERNAL_GRANDFATHER', 'PATERNAL_GRANDMOTHER',
    'MATERNAL_GRANDFATHER', 'MATERNAL_GRANDMOTHER',
    'BROTHER', 'SISTER', 'SON', 'DAUGHTER',
    'UNCLE', 'AUNT', 'COUSIN', 'OTHER'
  ];
  genders: FamilyGender[] = ['MALE', 'FEMALE', 'OTHER'];
  form: FamilyMember = this.emptyForm();

  constructor(
    private service: FamilyTreeService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const id = params['userId'];
      if (id) {
        this.userId = +id;
        this.userName = params['userName'] || '';
      }
      this.loadAll();
    });
  }

  goBackToMedicalRecords(): void {
    this.router.navigate(['/medical-records']);
  }

  loadAll(): void {
    this.isLoading = true;
    this.errorMessage = '';
    // Fire all 4 requests in parallel — not chained sequentially
    forkJoin({
      members:  this.service.getMembers(this.userId),
      tree:     this.service.getTree(this.userId),
      analysis: this.service.getRiskAnalysis(this.userId),
      stats:    this.service.getStats(this.userId),
    }).subscribe({
      next: ({ members, tree, analysis, stats }) => {
        if (members.success)  this.members      = members.data;
        if (tree.success)     this.treeNodes    = tree.data;
        if (analysis.success) this.riskAnalysis = analysis.data;
        if (stats.success)    this.stats        = stats.data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Failed to load family data.';
      }
    });
  }

  // ── Generation grouping ──────────────────────────────────────────────────
  getGeneration(gen: string): FamilyMember[] {
    switch (gen) {
      case 'grandparents':
        return this.members.filter(m =>
          ['PATERNAL_GRANDFATHER','PATERNAL_GRANDMOTHER',
           'MATERNAL_GRANDFATHER','MATERNAL_GRANDMOTHER'].includes(m.relationship));
      case 'parents':
        return this.members.filter(m =>
          ['FATHER','MOTHER','UNCLE','AUNT'].includes(m.relationship));
      case 'siblings':
        return this.members.filter(m =>
          ['BROTHER','SISTER'].includes(m.relationship));
      case 'children':
        return this.members.filter(m =>
          ['SON','DAUGHTER'].includes(m.relationship));
      case 'other':
        return this.members.filter(m =>
          ['COUSIN','OTHER'].includes(m.relationship));
      default:
        return [];
    }
  }

  getGenderEmoji(m: FamilyMember): string {
    if (m.gender === 'FEMALE') return '👩';
    if (m.gender === 'MALE') return '👨';
    return '🧑';
  }

  // ── PDF — backend finds the medical record automatically by userId ───────
  openPdfDialog(): void { this.downloadPdf(); } // kept for HTML compatibility

  confirmDownloadPdf(): void { this.downloadPdf(); } // kept for HTML compatibility

  downloadPdf(): void {
    this.generatingPdf = true;
    this.showPdfDialog = false;
    this.errorMessage  = '';

    // NEW endpoint: /user/{userId}/pdf — no manual record ID needed
    this.http.get(
      `/api/reports/user/${this.userId}/pdf`,
      { responseType: 'blob', observe: 'response' }
    ).subscribe({
      next: response => {
        this.generatingPdf = false;
        const blob = response.body!;
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href     = url;
        a.download = `Medical_Report_${new Date().toISOString().slice(0, 10)}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.successMessage = 'PDF downloaded! If your risk score exceeds 75%, an alert email was sent.';
        setTimeout(() => this.successMessage = '', 5000);
      },
      error: err => {
        this.generatingPdf = false;
        if (err.status === 404) {
          this.errorMessage = 'No medical record found for your account. Please go to Medical Records and create one first, then come back to generate the PDF.';
        } else if (err.status === 500) {
          this.errorMessage = 'Server error generating PDF. Please check the backend logs.';
        } else {
          this.errorMessage = 'Failed to generate PDF: ' + (err.message || 'Unknown error');
        }
      }
    });
  }

  // ── Form actions ─────────────────────────────────────────────────────────
  openAddForm(): void {
    this.form = this.emptyForm();
    this.isEditing = false;
    this.editingId = null;
    this.showForm = true;
  }

  openEditForm(m: FamilyMember): void {
    this.form = { ...m };
    this.isEditing = true;
    this.editingId = m.id!;
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.form = this.emptyForm();
  }

  saveMember(): void {
    this.form.userId = this.userId;
    this.isLoading = true;
    const action = this.isEditing && this.editingId
      ? this.service.updateMember(this.editingId, this.form)
      : this.service.addMember(this.form);
    action.subscribe({
      next: res => {
        this.isLoading = false;
        if (res.success) {
          this.showForm = false;
          this.successMessage = this.isEditing ? 'Member updated!' : 'Member added!';
          setTimeout(() => this.successMessage = '', 3000);
          this.loadAll();
        } else {
          this.errorMessage = res.message;
        }
      },
      error: () => { this.isLoading = false; this.errorMessage = 'Failed to save member.'; }
    });
  }

  deleteMember(id: number): void {
    if (!confirm('Delete this family member?')) return;
    this.service.deleteMember(id).subscribe({
      next: res => {
        if (res.success) {
          this.successMessage = 'Member deleted.';
          setTimeout(() => this.successMessage = '', 3000);
          this.loadAll();
        }
      }
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────
  emptyForm(): FamilyMember {
    return { userId: this.userId, fullName: '', relationship: 'FATHER', age: undefined, isAlive: true, hasAlzheimers: false, hasDementia: false, gender: 'MALE', otherConditions: '', notes: '' };
  }

  formatRel(rel: string): string {
    return rel.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  getRiskColor(score: number): string {
    if (score >= 60) return '#dc2626'; if (score >= 40) return '#d97706';
    if (score >= 20) return '#2563eb'; return '#16a34a';
  }

  getRiskGradient(score: number): string {
    if (score >= 60) return 'linear-gradient(135deg,#7f1d1d,#dc2626)';
    if (score >= 40) return 'linear-gradient(135deg,#78350f,#d97706)';
    if (score >= 20) return 'linear-gradient(135deg,#1e3a8a,#2563eb)';
    return 'linear-gradient(135deg,#14532d,#16a34a)';
  }

  getNodeColor(node: FamilyTreeNode): string {
    if (node.hasAlzheimers) return '#ef4444';
    if (node.hasDementia) return '#f59e0b';
    return '#00ffff';
  }

  get affectedByRelationshipEntries(): [string, number][] {
    if (!this.riskAnalysis?.affectedByRelationship) return [];
    return Object.entries(this.riskAnalysis.affectedByRelationship);
  }

  setTab(t: typeof this.activeTab): void { this.activeTab = t; }
}

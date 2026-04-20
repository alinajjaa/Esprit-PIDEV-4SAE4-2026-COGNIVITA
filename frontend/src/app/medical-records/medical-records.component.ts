import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink, Router } from '@angular/router';
import { Brain3dComponent } from '../brain3d/brain3d.component';
import { RiskFactorsComponent } from './components/risk-factors/risk-factors.component';
import { MedicalChartsComponent } from './components/charts/medical-charts.component';

interface MedicalRecord {
  id: number; userId: number; userName: string; userEmail: string;
  age: number; gender: string; educationLevel: string; familyHistory: string;
  riskFactors: string; currentSymptoms: string; diagnosisNotes: string;
  riskScore: number; riskLevel: string; lastRiskCalculation: string;
  hereditaryRiskContribution?: number;
  wellnessRiskContribution?: number;
  recommendations: string[]; createdAt: string; updatedAt: string;
  // Session 1 new fields
  apoeStatus?: string;
  apoeStatusDisplay?: string;
  diagnosisStage?: string;
  diagnosisStageDisplay?: string;
  diagnosisStageDescription?: string;
  stageManuallyOverridden?: boolean;
}
interface RiskScoreHistory {
  id: number; recordId: number; score: number; riskLevel: string;
  hereditaryContribution: number; wellnessContribution: number;
  calculatedAt: string; triggerReason: string;
}
interface UserGroup { userId: number; userName: string; userEmail: string; records: MedicalRecord[]; expanded: boolean; }
interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; size: number; number: number; }
interface ApiResponse<T> { success: boolean; message: string; data: T; }
interface TimelineEvent { id: number; eventDate: string; eventType: string; description: string; }
interface PreventionAction { id: number; actionType: string; description: string; actionDate: string; status: string; frequency: string; }

@Component({
  selector: 'app-medical-records',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Brain3dComponent, RiskFactorsComponent, MedicalChartsComponent],
  templateUrl: './medical-records.component.html',
  styleUrls: ['./medical-records.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MedicalRecordsComponent implements OnInit {
  // State
  allRecords: MedicalRecord[] = [];
  userGroups: UserGroup[] = [];
  // Cached computed values — updated only when data changes, never recalculated per render cycle
  cachedFilteredGroups: UserGroup[] = [];
  cachedPageNumbers: number[] = [];
  cachedHighRiskCount = 0;
  cachedAverageScore = 0;
  cachedCompletedCount = 0;
  selectedRecord: MedicalRecord | null = null;
  loading = true; error: string | null = null;
  showForm = false; isEditing = false;
  activeTab: 'overview' | 'risk' | 'prevention' | 'timeline' | 'stats' | 'wellness' | 'riskHistory' | 'medications' | 'careNotes' | 'appointments' | 'interactions' | 'trend' = 'overview';
  viewMode: 'grouped' | 'list' = 'grouped';

  // Pagination
  currentPage = 0; pageSize = 9; totalPages = 0; totalElements = 0;

  // Filters
  filterRiskLevel = ''; filterGender = ''; filterFamilyHistory = '';
  sortBy = 'createdAt'; sortDirection = 'DESC';
  searchName = '';

  // Patient data
  patientTimeline: TimelineEvent[] = [];
  patientActions: PreventionAction[] = [];
  patientActionPage = 0; patientActionTotalPages = 0;
  timelineFilter = ''; showActionForm = false;
  actionFormData = { actionType: '', description: '', actionDate: '', frequency: '', status: 'PENDING' };

  // Medications
  medications: any[] = [];
  medicationsLoading = false;
  showMedicationForm = false;
  editingMedication: any = null;
  medFormData = { name: '', dosage: '', frequency: '', prescribedBy: '', startDate: '', endDate: '', notes: '', isActive: true };

  // Care Notes
  careNotes: any[] = [];
  careNotesLoading = false;
  showCareNoteForm = false;
  editingCareNote: any = null;
  noteFormData = { authorName: '', authorRole: '', content: '', noteType: 'GENERAL', isPinned: false };


  // Notification status (shown in overview)
  lastNotificationInfo = '';

  // Check-user API state (fixes the add/edit bug)
  checkingUser = false;
  availableUsers: { id: number; firstName: string; lastName: string; email: string }[] = [];

  // Form
  formData = {
    userId: null as number | null, age: null as number | null, gender: '',
    educationLevel: '', familyHistory: 'No', riskFactors: '',
    currentSymptoms: '', diagnosisNotes: '',
    // Session 1 new fields
    apoeStatus: 'NOT_TESTED',
    diagnosisStage: ''
  };

  // APOE options for dropdown
  apoeOptions = [
    { value: 'NOT_TESTED',  label: '❓ Not Tested / Unknown',         risk: 0,   color: '#64748b' },
    { value: 'E2_E2',       label: '🛡️ E2/E2 — Strongly protective',  risk: -8,  color: '#10b981' },
    { value: 'E2_E3',       label: '🛡️ E2/E3 — Mildly protective',    risk: -4,  color: '#34d399' },
    { value: 'E3_E3',       label: '⚖️ E3/E3 — Average risk',         risk: 0,   color: '#94a3b8' },
    { value: 'E2_E4',       label: '⚠️ E2/E4 — Slightly elevated',    risk: 5,   color: '#fbbf24' },
    { value: 'E3_E4',       label: '⚠️ E3/E4 — Elevated (1×E4)',      risk: 15,  color: '#f97316' },
    { value: 'E4_E4',       label: '🔴 E4/E4 — High risk (2×E4)',     risk: 25,  color: '#ef4444' },
  ];

  // Diagnosis stage options
  stageOptions = [
    { value: '',            label: '— Auto (from risk score)' },
    { value: 'PRECLINICAL', label: '🟢 Preclinical — No symptoms' },
    { value: 'MCI',         label: '🟡 MCI — Mild Cognitive Impairment' },
    { value: 'MILD',        label: '🟠 Mild — Early dementia' },
    { value: 'MODERATE',    label: '🔴 Moderate — Needs assistance' },
    { value: 'SEVERE',      label: '⛔ Severe — Full care required' },
  ];

  // Duplicate-user guard: true when a non-editing form is shown for a userId that already has a record
  get userAlreadyHasRecord(): boolean {
    if (this.isEditing || !this.formData.userId) return false;
    return this.allRecords.some(r => r.userId === this.formData.userId);
  }
  get existingRecordForUser(): MedicalRecord | null {
    if (this.isEditing || !this.formData.userId) return null;
    return this.allRecords.find(r => r.userId === this.formData.userId) || null;
  }

  private apiUrl = '/api/medical-records';
  private reportApiUrl = '/api/reports';
  private healthPreventionApiUrl = '/api/health-prevention/profiles';

  // Health Prevention integration
  healthProfile: any = null;
  healthProfileLoading = false;
  healthProfileError: string | null = null;
  wellnessBars: { label: string; val: string; pct: number; color: string }[] = [];
  generatingPdf = false;

  // Risk Score History
  riskScoreHistory: RiskScoreHistory[] = [];
  riskHistoryLoading = false;

  private timelineApiUrl     = '/api/timeline';
  private preventionApiUrl   = '/api/prevention-actions';
  private medicationApiUrl   = '/api/medications';
  private careNoteApiUrl     = '/api/care-notes';

  constructor(private http: HttpClient, private router: Router, private cdr: ChangeDetectorRef) {}
  ngOnInit(): void { this.loadRecords(); }

  loadRecords(): void {
    this.loading = true;
    let params = new HttpParams()
      .set('page', this.currentPage.toString())
      .set('size', this.pageSize.toString())
      .set('sortBy', this.sortBy)
      .set('sortDirection', this.sortDirection);
    if (this.filterRiskLevel) params = params.set('riskLevel', this.filterRiskLevel);
    if (this.filterGender) params = params.set('gender', this.filterGender);
    if (this.filterFamilyHistory) params = params.set('familyHistory', this.filterFamilyHistory);

    this.http.get<ApiResponse<PageResponse<MedicalRecord>>>(this.apiUrl, { params }).subscribe({
      next: (response) => {
        const page = response.data;
        this.allRecords = page.content || [];
        this.totalPages = page.totalPages || 0;
        this.totalElements = page.totalElements || 0;
        this.buildUserGroups();
        this.updateRecordStats();
        this.updatePageNumbers();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => { this.error = 'Failed to load medical records: ' + (err?.error?.message || err?.error?.error || err?.status || err?.message || 'Unknown error'); this.loading = false; this.cdr.markForCheck(); }
    });
  }

  buildUserGroups(): void {
    const map = new Map<number, UserGroup>();
    for (const record of this.allRecords) {
      if (!map.has(record.userId)) {
        map.set(record.userId, {
          userId: record.userId,
          userName: record.userName,
          userEmail: record.userEmail,
          records: [],
          expanded: true
        });
      }
      map.get(record.userId)!.records.push(record);
    }
    this.userGroups = Array.from(map.values());
    this.updateFilteredGroups();
  }

  get filteredGroups(): UserGroup[] { return this.cachedFilteredGroups; }

  private updateFilteredGroups(): void {
    if (!this.searchName) {
      this.cachedFilteredGroups = this.userGroups;
    } else {
      const q = this.searchName.toLowerCase();
      this.cachedFilteredGroups = this.userGroups
        .map(g => ({ ...g, records: g.records.filter(r => r.userName.toLowerCase().includes(q) || r.userEmail.toLowerCase().includes(q)) }))
        .filter(g => g.records.length > 0);
    }
  }

  toggleGroup(group: UserGroup): void { group.expanded = !group.expanded; this.cdr.markForCheck(); }

  // trackBy functions — prevent Angular from destroying/recreating DOM nodes unnecessarily
  trackByUserId(_: number, g: UserGroup): number   { return g.userId; }
  trackByRecordId(_: number, r: any): number        { return r.id; }
  trackByIndex(i: number): number                   { return i; }


  // Filters & Pagination
  applyFilters(): void { this.currentPage = 0; this.loadRecords(); }
  onSearchInput(): void { this.updateFilteredGroups(); this.cdr.markForCheck(); }
  clearFilters(): void { this.filterRiskLevel = ''; this.filterGender = ''; this.filterFamilyHistory = ''; this.sortBy = 'createdAt'; this.sortDirection = 'DESC'; this.searchName = ''; this.applyFilters(); }
  changePage(page: number): void { if (page >= 0 && page < this.totalPages) { this.currentPage = page; this.loadRecords(); } }
  get pageNumbers(): number[] { return this.cachedPageNumbers; }

  private updatePageNumbers(): void {
    const max = 7; const half = Math.floor(max / 2);
    let start = Math.max(0, this.currentPage - half);
    let end = Math.min(this.totalPages, start + max);
    start = Math.max(0, end - max);
    this.cachedPageNumbers = Array.from({ length: end - start }, (_, i) => start + i);
  }

  // View record
  viewRecord(record: MedicalRecord): void {
    this.selectedRecord = record;
    this.activeTab = 'overview';
    // Reset stale data from previous record
    this.riskScoreHistory = [];
    this.patientTimeline = [];
    this.patientActions = [];
    this.healthProfile = null;
    this.healthProfileError = null;
    this.cdr.markForCheck();
    // Load timeline for overview tab — other tabs load lazily on click
    this.loadPatientTimeline(record.id);
  }

  loadRiskScoreHistory(recordId: number): void {
    this.riskHistoryLoading = true;
    this.http.get<ApiResponse<RiskScoreHistory[]>>(`${this.apiUrl}/${recordId}/risk-score-history`).subscribe({
      next: (r) => { this.riskScoreHistory = r.data || []; this.riskHistoryLoading = false; this.cdr.markForCheck(); },
      error: () => { this.riskScoreHistory = []; this.riskHistoryLoading = false; this.cdr.markForCheck(); }
    });
  }

  navigateToFamilyTree(): void {
    if (this.selectedRecord) {
      this.router.navigate(['/family-tree'], {
        queryParams: { userId: this.selectedRecord.userId, userName: this.selectedRecord.userName }
      });
    }
  }

  navigateToHealthPrevention(): void {
    if (this.selectedRecord) {
      this.router.navigate(['/health-prevention'], {
        queryParams: { userId: this.selectedRecord.userId, userName: this.selectedRecord.userName }
      });
    }
  }

  loadPatientTimeline(recordId: number): void {
    let url = `${this.timelineApiUrl}/medical-record/${recordId}`;
    if (this.timelineFilter) url += `/type/${this.timelineFilter}`;
    this.http.get<ApiResponse<TimelineEvent[]>>(url).subscribe({
      next: (r) => { this.patientTimeline = r.data || []; this.cdr.markForCheck(); },
      error: () => { this.patientTimeline = []; this.cdr.markForCheck(); }
    });
  }

  loadPatientActions(recordId: number, page = 0): void {
    const params = new HttpParams().set('page', page.toString()).set('size', '5').set('sortBy', 'actionDate').set('sortDirection', 'DESC');
    this.http.get<ApiResponse<PageResponse<PreventionAction>>>(`${this.preventionApiUrl}/medical-record/${recordId}`, { params }).subscribe({
      next: (r) => { this.patientActions = r.data.content || []; this.patientActionTotalPages = r.data.totalPages || 0; this.patientActionPage = page; this.updateActionStats(); this.cdr.markForCheck(); },
      error: () => { this.patientActions = []; this.cdr.markForCheck(); }
    });
  }

  setTab(tab: 'overview' | 'risk' | 'prevention' | 'timeline' | 'stats' | 'wellness' | 'riskHistory' | 'medications' | 'careNotes' | 'appointments' | 'interactions' | 'trend'): void {
    this.activeTab = tab;
    this.cdr.markForCheck();
    if (!this.selectedRecord) return;
    // Lazy-load: each tab fetches its own data only when activated
    if (tab === 'timeline')     this.loadPatientTimeline(this.selectedRecord.id);
    if (tab === 'prevention')   this.loadPatientActions(this.selectedRecord.id);
    if (tab === 'riskHistory')  this.loadRiskScoreHistory(this.selectedRecord.id);
    if (tab === 'wellness')     this.loadHealthProfile();
    if (tab === 'medications')  this.loadMedications(this.selectedRecord.id);
    if (tab === 'careNotes')    this.loadCareNotes(this.selectedRecord.id);
    if (tab === 'appointments') this.loadAppointments(this.selectedRecord.id);
    if (tab === 'interactions') this.loadInteractions(this.selectedRecord.id);
    if (tab === 'trend')        this.loadRiskTrend(this.selectedRecord.id);
  }

  filterTimeline(type: string): void { this.timelineFilter = type; this.cdr.markForCheck(); if (this.selectedRecord) this.loadPatientTimeline(this.selectedRecord.id); }

  // Prevention Actions
  openActionForm(): void {
    this.actionFormData = { actionType: '', description: '', actionDate: new Date().toISOString().split('T')[0] + 'T00:00', frequency: '', status: 'PENDING' };
    this.showActionForm = true; this.cdr.markForCheck();
  }
  closeActionForm(): void { this.showActionForm = false; this.cdr.markForCheck(); }
  submitAction(): void {
    if (!this.selectedRecord || !this.actionFormData.actionType) return;
    this.http.post<ApiResponse<PreventionAction>>(this.preventionApiUrl, { ...this.actionFormData, medicalRecordId: this.selectedRecord.id }).subscribe({
      next: () => { this.loadPatientActions(this.selectedRecord!.id); this.closeActionForm(); },
      error: () => { this.error = 'Failed to create action'; this.cdr.markForCheck(); }
    });
  }
  completeAction(actionId: number): void {
    this.http.patch<ApiResponse<PreventionAction>>(`${this.preventionApiUrl}/${actionId}/complete`, {}).subscribe({
      next: () => { if (this.selectedRecord) this.loadPatientActions(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to complete action'; this.cdr.markForCheck(); }
    });
  }
  deleteAction(actionId: number): void {
    if (!confirm('Delete this prevention action?')) return;
    this.http.delete<ApiResponse<void>>(`${this.preventionApiUrl}/${actionId}`).subscribe({
      next: () => { if (this.selectedRecord) this.loadPatientActions(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to delete action'; this.cdr.markForCheck(); }
    });
  }

  // Record Form
  closeDetails(): void { this.selectedRecord = null; this.patientTimeline = []; this.patientActions = []; this.cdr.markForCheck(); }
  openCreateForm(): void {
    this.isEditing = false; this.error = null;
    this.formData = { userId: null, age: null, gender: '', educationLevel: '', familyHistory: 'No', riskFactors: '', currentSymptoms: '', diagnosisNotes: '', apoeStatus: 'NOT_TESTED', diagnosisStage: '' };
    this.loadAvailableUsers();
    this.showForm = true; this.cdr.markForCheck();
  }
  openCreateFormForUser(userId: number, userName: string): void {
    this.isEditing = false; this.error = null;
    this.formData = { userId, age: null, gender: '', educationLevel: '', familyHistory: 'No', riskFactors: '', currentSymptoms: '', diagnosisNotes: '', apoeStatus: 'NOT_TESTED', diagnosisStage: '' };
    this.loadAvailableUsers();
    this.showForm = true; this.cdr.markForCheck();
  }
  openEditForm(record: MedicalRecord): void {
    this.isEditing = true; this.error = null;
    this.formData = {
      userId: record.userId, age: record.age, gender: record.gender,
      educationLevel: record.educationLevel, familyHistory: record.familyHistory,
      riskFactors: record.riskFactors, currentSymptoms: record.currentSymptoms,
      diagnosisNotes: record.diagnosisNotes,
      apoeStatus: record.apoeStatus || 'NOT_TESTED',
      diagnosisStage: record.diagnosisStage || ''
    };
    this.selectedRecord = record; this.showForm = true; this.cdr.markForCheck();
  }
  closeForm(): void { this.showForm = false; this.cdr.markForCheck(); }

  // Symptom checklist data
  cognitiveSymptoms = [
    'Memory loss (short-term)', 'Forgetting recent events', 'Difficulty learning new things',
    'Confusion with time/place', 'Getting lost in familiar places', 'Difficulty with language/word finding',
    'Difficulty concentrating', 'Poor judgment / decision-making'
  ];
  behavioralSymptoms = [
    'Personality changes', 'Social withdrawal', 'Depression / Apathy', 'Mood swings / Irritability',
    'Difficulty with daily tasks (ADLs)', 'Misplacing objects repeatedly', 'Sleep disturbances',
    'Wandering / disorientation'
  ];

  isSymptomSelected(symptom: string): boolean {
    return (this.formData.currentSymptoms || '').toLowerCase().includes(symptom.toLowerCase());
  }

  toggleSymptom(symptom: string): void {
    const current = this.formData.currentSymptoms || '';
    if (this.isSymptomSelected(symptom)) {
      // Remove symptom
      const parts = current.split(', ').filter(s => s.toLowerCase() !== symptom.toLowerCase());
      this.formData.currentSymptoms = parts.join(', ');
    } else {
      // Add symptom
      this.formData.currentSymptoms = current ? current + ', ' + symptom : symptom;
    }
  }
  loadAvailableUsers(): void {
    this.http.get<any[]>('/api/users').subscribe({
      next: (users) => { this.availableUsers = users; this.cdr.markForCheck(); },
      error: () => { this.availableUsers = []; }
    });
  }

  submitForm(): void {
    if (!this.formData.userId || !this.formData.age || !this.formData.gender) {
      this.error = 'Please fill in all required fields (User ID, Age, Gender)'; this.cdr.markForCheck(); return;
    }
    // Editing: just submit directly — no duplicate check needed
    if (this.isEditing && this.selectedRecord) {
      this.doSubmit();
      return;
    }
    // Creating: verify against server (fixes pagination-limited client-side check)
    this.checkingUser = true;
    this.error = null;
    this.cdr.markForCheck();
    this.http.get<ApiResponse<any>>(`${this.apiUrl}/check-user/${this.formData.userId}`).subscribe({
      next: (res) => {
        this.checkingUser = false;
        if (res.data?.hasRecord) {
          this.error = `This user already has a medical record (ID: ${res.data.recordId}). Edit the existing record instead.`;
          this.cdr.markForCheck();
        } else {
          this.doSubmit();
        }
      },
      error: () => { this.checkingUser = false; this.doSubmit(); } // fallback: attempt submit
    });
  }

  private doSubmit(): void {
    const req = this.isEditing && this.selectedRecord
      ? this.http.put<ApiResponse<MedicalRecord>>(`${this.apiUrl}/${this.selectedRecord.id}`, this.formData)
      : this.http.post<ApiResponse<MedicalRecord>>(this.apiUrl, this.formData);
    req.subscribe({
      next: (response) => {
        this.loadRecords();
        this.closeForm();
        if (response.data) this.viewRecord(response.data);
      },
      error: (err) => {
        this.error = 'Failed to save: ' + (err.error?.message || err.error?.error || err.message);
        this.cdr.markForCheck();
      }
    });
  }
  deleteRecord(id: number): void {
    if (!confirm('Delete this medical record?')) return;
    this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).subscribe({
      next: () => { this.loadRecords(); this.closeDetails(); },
      error: () => { this.error = 'Failed to delete record'; this.cdr.markForCheck(); }
    });
  }

  // Helpers
  getRiskClass(level: string): string {
    const m: Record<string, string> = { CRITICAL: 'risk-critical', HIGH: 'risk-high', MEDIUM: 'risk-medium', LOW: 'risk-low', Yes: 'risk-high', No: 'risk-low' };
    return m[level] || 'risk-low';
  }
  getRiskEmoji(level: string): string {
    const m: Record<string, string> = { CRITICAL: '🔴', HIGH: '🟠', MEDIUM: '🟡', LOW: '🟢' };
    return m[level] || '⚪';
  }
  getEventIcon(type: string): string {
    const m: Record<string, string> = { RISK_FACTOR_ADDED: '⚠️', RISK_FACTOR_UPDATED: '✏️', RISK_FACTOR_REMOVED: '🗑️', PREVENTION_ACTION_ADDED: '✅', PREVENTION_ACTION_UPDATED: '🔄', MEDICAL_RECORD_UPDATED: '📋' };
    return m[type] || '📌';
  }
  getStatusClass(s: string): string {
    const m: Record<string, string> = { COMPLETED: 'status-completed', PENDING: 'status-pending', CANCELLED: 'status-cancelled' };
    return m[s] || 'status-pending';
  }
  getHighestRiskInGroup(group: UserGroup): string {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    for (const level of order) { if (group.records.some(r => r.riskLevel === level)) return level; }
    return 'LOW';
  }
  getAvgScoreInGroup(group: UserGroup): number {
    if (!group.records.length) return 0;
    return Math.round(group.records.reduce((a, r) => a + (r.riskScore || 0), 0) / group.records.length);
  }
  formatDate(d: string): string { if (!d) return 'N/A'; return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }); }

  getInitials(name: string): string { if (!name) return '?'; return name.split(' ').map(p => p[0]).join('').substring(0, 2).toUpperCase(); }

  downloadPdfReport(medicalRecordId: number): void {
    this.generatingPdf = true;
    this.cdr.markForCheck();
    this.http.get(`${this.reportApiUrl}/medical-record/${medicalRecordId}/pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.generatingPdf = false;
        this.cdr.markForCheck();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Medical_Report_${medicalRecordId}_${new Date().toISOString().slice(0,10)}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.generatingPdf = false;
        this.error = 'Failed to generate PDF report. Please try again.';
        this.cdr.markForCheck();
      }
    });
  }
  getHighRiskCount(): number { return this.cachedHighRiskCount; }
  getAverageScore(): number  { return this.cachedAverageScore; }
  getRiskScoreWidth(score: number): string { return Math.min(100, score || 0) + '%'; }
  getCompletedCount(): number { return this.cachedCompletedCount; }

  private updateRecordStats(): void {
    this.cachedHighRiskCount = this.allRecords.filter(r => r.riskLevel === 'HIGH' || r.riskLevel === 'CRITICAL').length;
    this.cachedAverageScore  = this.allRecords.length
      ? Math.round(this.allRecords.reduce((a, r) => a + (r.riskScore || 0), 0) / this.allRecords.length) : 0;
  }

  private updateActionStats(): void {
    this.cachedCompletedCount = this.patientActions.filter(a => a.status === 'COMPLETED').length;
  }

  // ═══ Health Prevention Integration ═══

  loadHealthProfile(): void {
    if (!this.selectedRecord) return;
    this.healthProfileLoading = true;
    this.healthProfileError = null;
    this.http.get<any>(`${this.healthPreventionApiUrl}/user/${this.selectedRecord.userId}`).subscribe({
      next: (res) => {
        this.healthProfile = res.data || res;
        this.buildWellnessBars();
        this.healthProfileLoading = false;
        this.syncWellnessRiskToRecord();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.healthProfileLoading = false;
        if (err.status === 404) {
          this.healthProfileError = 'No wellness profile found for this patient. Create one to start tracking lifestyle factors.';
        } else {
          this.healthProfileError = 'Could not reach Health Prevention service. Ensure it is running on port 8082.';
        }
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Convert wellness score (0-100, higher = healthier) to wellness RISK score
   * (0-100, higher = more risk) then PATCH the medical record.
   * wellnessRiskScore = 100 - wellnessScore
   */
  syncWellnessRiskToRecord(): void {
    if (!this.selectedRecord || !this.healthProfile) return;
    const wellnessScore = this.healthProfile.wellnessScore ?? 50;
    const wellnessRiskContribution = Math.round(100 - wellnessScore);
    // Skip if unchanged to avoid redundant API call
    if (this.selectedRecord.wellnessRiskContribution === wellnessRiskContribution) return;
    this.http.patch<any>(
      `${this.apiUrl}/${this.selectedRecord.id}/wellness-risk`,
      { wellnessRiskContribution }
    ).subscribe({
      next: (res) => {
        // Update just the selected record in memory — no full list reload needed
        if (res.data) {
          this.selectedRecord = { ...this.selectedRecord!, ...res.data };
          // Also update the record in allRecords so the list card reflects new score
          const idx = this.allRecords.findIndex(r => r.id === this.selectedRecord!.id);
          if (idx >= 0) this.allRecords[idx] = { ...this.allRecords[idx], ...res.data };
          this.buildUserGroups();
        }
      },
      error: () => { /* silent */ }
    });
  }

  createHealthProfile(): void {
    if (!this.selectedRecord) return;
    const payload = { userId: this.selectedRecord.userId, medicalRecordId: this.selectedRecord.id };
    this.http.post<any>(this.healthPreventionApiUrl, payload).subscribe({
      next: () => this.loadHealthProfile(),
      error: () => { this.healthProfileError = 'Failed to create wellness profile. Please try manually in the Health Prevention module.'; }
    });
  }

  buildWellnessBars(): void {
    if (!this.healthProfile) return;
    const activityMap: Record<string, number> = { SEDENTARY: 10, LIGHT: 30, MODERATE: 60, ACTIVE: 80, VERY_ACTIVE: 100 };
    const dietMap: Record<string, number> = { POOR: 10, BELOW_AVERAGE: 25, AVERAGE: 50, GOOD: 75, EXCELLENT: 100 };
    const sleepHrs = this.healthProfile.sleepHoursPerNight || 0;
    const sleepPct = sleepHrs >= 7 && sleepHrs <= 9 ? 100 : sleepHrs >= 6 ? 60 : 30;
    this.wellnessBars = [
      { label: 'Physical Activity', val: this.healthProfile.physicalActivityLevel || 'Unknown', pct: activityMap[this.healthProfile.physicalActivityLevel] || 0, color: '#10b981' },
      { label: 'Sleep Quality', val: sleepHrs ? sleepHrs + ' hrs' : 'Unknown', pct: sleepPct, color: '#6366f1' },
      { label: 'Diet Quality', val: this.healthProfile.dietQuality || 'Unknown', pct: dietMap[this.healthProfile.dietQuality] || 0, color: '#f59e0b' },
      { label: 'Smoking Free', val: this.healthProfile.smokingStatus ? 'Smoker' : 'Non-smoker', pct: this.healthProfile.smokingStatus ? 0 : 100, color: '#ef4444' },
    ];
  }

  getWellnessScoreColor(): string {
    const s = this.healthProfile?.wellnessScore || 0;
    if (s >= 70) return '#10b981'; if (s >= 40) return '#f59e0b'; return '#ef4444';
  }

  getActivityClass(level: string): string {
    const m: Record<string, string> = { SEDENTARY: 'card-bad', LIGHT: 'card-warn', MODERATE: 'card-ok', ACTIVE: 'card-good', VERY_ACTIVE: 'card-good' };
    return m[level] || '';
  }
  getActivityInsight(level: string): string {
    const m: Record<string, string> = { SEDENTARY: '⚠️ Sedentary lifestyle — significant risk factor', LIGHT: '⚡ Light activity — aim for 150 min/week', MODERATE: '✅ Moderate — meets recommendations', ACTIVE: '✅ Active — neuroprotective benefit', VERY_ACTIVE: '🏆 Very active — optimal for brain health' };
    return m[level] || 'Set activity level for insight';
  }
  getSleepClass(hrs: number): string { return !hrs ? '' : hrs >= 7 && hrs <= 9 ? 'card-good' : hrs >= 6 ? 'card-warn' : 'card-bad'; }
  getSleepInsight(hrs: number): string { return !hrs ? 'Sleep data not recorded' : hrs >= 7 && hrs <= 9 ? '✅ Optimal sleep — glymphatic amyloid clearance' : hrs < 6 ? '⚠️ Insufficient sleep impairs amyloid clearance' : '⚡ Slightly suboptimal — target 7–9 hours'; }
  getDietClass(q: string): string { const m: Record<string, string> = { POOR: 'card-bad', BELOW_AVERAGE: 'card-warn', AVERAGE: '', GOOD: 'card-ok', EXCELLENT: 'card-good' }; return m[q] || ''; }
  getDietInsight(q: string): string { const m: Record<string, string> = { POOR: '⚠️ Poor diet — high dementia risk', BELOW_AVERAGE: '⚡ Below average — improve Mediterranean adherence', AVERAGE: 'Average diet — room for improvement', GOOD: '✅ Good diet — neuroprotective', EXCELLENT: '🏆 Excellent — MIND diet adherence optimal' }; return m[q] || 'Set diet quality for insight'; }
  getStressClass(s: string): string { const m: Record<string, string> = { LOW: 'card-good', MODERATE: 'card-ok', HIGH: 'card-warn', VERY_HIGH: 'card-bad' }; return m[s] || ''; }
  getStressInsight(s: string): string { const m: Record<string, string> = { LOW: '✅ Low stress — cortisol within healthy range', MODERATE: '⚡ Moderate — stress management beneficial', HIGH: '⚠️ High stress — cortisol damages hippocampus', VERY_HIGH: '🔴 Very high — urgent intervention recommended' }; return m[s] || 'Stress data not set'; }
  getCogTrainingClass(f: string): string { const m: Record<string, string> = { Never: 'card-bad', Rarely: 'card-warn', Weekly: 'card-ok', Daily: 'card-good' }; return m[f] || ''; }

  // ═══ Risk Score History Chart Helpers ═══

  /** Human-readable trigger label */
  formatTrigger(raw: string): string {
    const m: Record<string, string> = {
      RECORD_CREATED:          '📋 Record created',
      RECORD_UPDATED:          '✏️ Record edited',
      RISK_FACTOR_CHANGED:     '⚠️ Risk factor changed',
      FAMILY_TREE_UPDATED:     '🧬 Family tree updated',
      WELLNESS_PROFILE_UPDATED:'🌿 Wellness profile saved',
      MANUAL_UPDATE:           '🔄 Manual recalculation',
    };
    return m[raw] || raw || '—';
  }

  /**
   * Build SVG polyline points string.
   * Input: history DESC order (newest first). We plot oldest→newest left→right.
   * SVG viewBox: 0 0 600 160  — score 0 maps to y=150, score 99 maps to y=10
   */
  getChartPoints(history: RiskScoreHistory[]): string {
    if (!history || history.length < 2) return '';
    const pts = [...history].reverse(); // oldest first
    const n = pts.length;
    return pts.map((h, i) => {
      const x = n === 1 ? 300 : 30 + (i / (n - 1)) * 540;
      const y = 150 - (h.score / 99) * 140;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  /** Area fill polygon (polyline + bottom edge) */
  getChartArea(history: RiskScoreHistory[]): string {
    if (!history || history.length < 2) return '';
    const pts = [...history].reverse();
    const n = pts.length;
    const line = pts.map((h, i) => {
      const x = 30 + (i / (n - 1)) * 540;
      const y = 150 - (h.score / 99) * 140;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
    // close the polygon along the bottom
    const firstX = 30;
    const lastX  = 30 + 540;
    return `${firstX},150 ${line} ${lastX},150`;
  }

  /** Dot positions with risk-level colour */
  getChartDots(history: RiskScoreHistory[]): { x: number; y: number; color: string }[] {
    if (!history || history.length === 0) return [];
    const pts = [...history].reverse();
    const n = pts.length;
    const colorMap: Record<string, string> = {
      LOW: '#10b981', MEDIUM: '#f59e0b', HIGH: '#f97316', CRITICAL: '#ef4444'
    };
    return pts.map((h, i) => ({
      x: n === 1 ? 300 : 30 + (i / (n - 1)) * 540,
      y: 150 - (h.score / 99) * 140,
      color: colorMap[h.riskLevel] || '#94a3b8'
    }));
  }

  /** X-axis date labels — show at most 6, evenly spaced */
  getChartLabels(history: RiskScoreHistory[]): string[] {
    if (!history || history.length === 0) return [];
    const pts = [...history].reverse();
    const max = 6;
    if (pts.length <= max) return pts.map(h => this.shortDate(h.calculatedAt));
    const step = Math.floor(pts.length / (max - 1));
    const labels: string[] = [];
    for (let i = 0; i < pts.length; i++) {
      if (i === 0 || i === pts.length - 1 || i % step === 0) labels.push(this.shortDate(pts[i].calculatedAt));
    }
    return labels.slice(0, max);
  }

  private shortDate(d: string): string {
    if (!d) return '';
    const dt = new Date(d);
    return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }


  // ═══ APPOINTMENTS (Session 2) ═══
  appointments: any[] = [];
  appointmentStats: any = null;
  appointmentsLoading = false;
  showAppointmentForm = false;
  editingAppointment: any = null;
  apptFormData = {
    doctorName: '', specialty: '', appointmentType: 'NEUROLOGIST',
    scheduledAt: '', location: '', notes: '', status: 'SCHEDULED'
  };
  readonly appointmentApiUrl = '/api/appointments';
  readonly appointmentTypes = [
    { value: 'GENERAL',       label: '🏥 General Check-up' },
    { value: 'NEUROLOGIST',   label: '🧠 Neurologist' },
    { value: 'MEMORY_CLINIC', label: '💭 Memory Clinic' },
    { value: 'GENETICS',      label: '🧬 Genetics / APOE Testing' },
    { value: 'PHARMACIST',    label: '💊 Pharmacist Review' },
    { value: 'FOLLOW_UP',     label: '📋 Follow-up' },
    { value: 'IMAGING',       label: '🔬 Brain Imaging (MRI/CT)' },
    { value: 'PSYCHIATRY',    label: '🩺 Psychiatry' },
  ];
  statusColors: Record<string, string> = {
    SCHEDULED: '#2563eb', COMPLETED: '#10b981',
    CANCELLED: '#94a3b8', MISSED: '#dc2626', RESCHEDULED: '#f59e0b'
  };

  // ═══ MEDICATION INTERACTIONS (Session 2) ═══
  interactionAlerts: any[] = [];
  interactionSummary: any = null;
  interactionsLoading = false;

  // ═══ RISK TREND & PROJECTION (Session 3) ═══
  riskTrend: any = null;
  riskTrendLoading = false;

  // ═══ Medications ═══

  loadMedications(recordId: number): void {
    this.medicationsLoading = true;
    this.http.get<ApiResponse<any[]>>(`${this.medicationApiUrl}/medical-record/${recordId}`).subscribe({
      next: (r) => { this.medications = r.data || []; this.medicationsLoading = false; this.cdr.markForCheck(); },
      error: () => { this.medications = []; this.medicationsLoading = false; this.cdr.markForCheck(); }
    });
  }

  openMedicationForm(med?: any): void {
    this.editingMedication = med || null;
    if (med) {
      this.medFormData = {
        name: med.name || '', dosage: med.dosage || '', frequency: med.frequency || '',
        prescribedBy: med.prescribedBy || '', startDate: med.startDate || '',
        endDate: med.endDate || '', notes: med.notes || '', isActive: med.isActive !== false
      };
    } else {
      this.medFormData = { name: '', dosage: '', frequency: '', prescribedBy: '', startDate: '', endDate: '', notes: '', isActive: true };
    }
    this.showMedicationForm = true;
    this.cdr.markForCheck();
  }

  closeMedicationForm(): void { this.showMedicationForm = false; this.editingMedication = null; this.cdr.markForCheck(); }

  submitMedication(): void {
    // FIX: explicit validation with visible error messages instead of silent early return
    if (!this.selectedRecord) {
      this.error = 'No patient record selected. Please open a patient record first.';
      this.cdr.markForCheck();
      return;
    }
    if (!this.medFormData.name || this.medFormData.name.trim() === '') {
      this.error = 'Medication name is required.';
      this.cdr.markForCheck();
      return;
    }
    this.error = null;
    const payload = { ...this.medFormData, medicalRecordId: this.selectedRecord.id };
    const req = this.editingMedication
      ? this.http.put<ApiResponse<any>>(`${this.medicationApiUrl}/${this.editingMedication.id}`, payload)
      : this.http.post<ApiResponse<any>>(this.medicationApiUrl, payload);
    req.subscribe({
      next: () => { this.loadMedications(this.selectedRecord!.id); this.closeMedicationForm(); },
      error: (err) => {
        this.error = 'Failed to save medication: ' + (err?.error?.error || err?.error?.message || err?.message || 'Unknown error');
        this.cdr.markForCheck();
      }
    });
  }

  deleteMedication(id: number): void {
    if (!confirm('Remove this medication?')) return;
    this.http.delete<ApiResponse<void>>(`${this.medicationApiUrl}/${id}`).subscribe({
      next: () => { if (this.selectedRecord) this.loadMedications(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to delete medication'; this.cdr.markForCheck(); }
    });
  }

  getMedicationRiskClass(flag: string): string {
    if (flag === 'RISK')       return 'risk-high';
    if (flag === 'PROTECTIVE') return 'risk-low';
    return '';
  }

  getMedicationRiskLabel(flag: string): string {
    if (flag === 'RISK')       return '⚠️ Cognitive risk';
    if (flag === 'PROTECTIVE') return '🛡️ Neuroprotective';
    return '';
  }

  // ═══ Care Notes ═══

  loadCareNotes(recordId: number): void {
    this.careNotesLoading = true;
    this.http.get<ApiResponse<any[]>>(`${this.careNoteApiUrl}/medical-record/${recordId}`).subscribe({
      next: (r) => { this.careNotes = r.data || []; this.careNotesLoading = false; this.cdr.markForCheck(); },
      error: () => { this.careNotes = []; this.careNotesLoading = false; this.cdr.markForCheck(); }
    });
  }

  openCareNoteForm(note?: any): void {
    this.editingCareNote = note || null;
    if (note) {
      this.noteFormData = {
        authorName: note.authorName || '', authorRole: note.authorRole || '',
        content: note.content || '', noteType: note.noteType || 'GENERAL', isPinned: note.isPinned || false
      };
    } else {
      this.noteFormData = { authorName: '', authorRole: '', content: '', noteType: 'GENERAL', isPinned: false };
    }
    this.showCareNoteForm = true;
    this.cdr.markForCheck();
  }

  closeCareNoteForm(): void { this.showCareNoteForm = false; this.editingCareNote = null; this.cdr.markForCheck(); }

  submitCareNote(): void {
    // FIX: explicit validation with visible error messages instead of silent early return.
    // Previously, a null selectedRecord or blank fields caused a silent no-op — the form
    // appeared to do nothing, giving the user no indication of what was wrong.
    if (!this.selectedRecord) {
      this.error = 'No patient record selected. Please open a patient record first.';
      this.cdr.markForCheck();
      return;
    }
    if (!this.noteFormData.authorName || this.noteFormData.authorName.trim() === '') {
      this.error = 'Author name is required to add a care note.';
      this.cdr.markForCheck();
      return;
    }
    if (!this.noteFormData.content || this.noteFormData.content.trim() === '') {
      this.error = 'Note content is required.';
      this.cdr.markForCheck();
      return;
    }
    this.error = null;
    const payload = { ...this.noteFormData, medicalRecordId: this.selectedRecord.id };
    const req = this.editingCareNote
      ? this.http.put<ApiResponse<any>>(`${this.careNoteApiUrl}/${this.editingCareNote.id}`, payload)
      : this.http.post<ApiResponse<any>>(this.careNoteApiUrl, payload);
    req.subscribe({
      next: () => { this.loadCareNotes(this.selectedRecord!.id); this.closeCareNoteForm(); },
      error: (err) => {
        this.error = 'Failed to save note: ' + (err?.error?.error || err?.error?.message || err?.message || 'Unknown error');
        this.cdr.markForCheck();
      }
    });
  }

  togglePin(noteId: number): void {
    this.http.patch<ApiResponse<any>>(`${this.careNoteApiUrl}/${noteId}/pin`, {}).subscribe({
      next: () => { if (this.selectedRecord) this.loadCareNotes(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to toggle pin'; this.cdr.markForCheck(); }
    });
  }

  deleteCareNote(id: number): void {
    if (!confirm('Delete this care note?')) return;
    this.http.delete<ApiResponse<void>>(`${this.careNoteApiUrl}/${id}`).subscribe({
      next: () => { if (this.selectedRecord) this.loadCareNotes(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to delete note'; this.cdr.markForCheck(); }
    });
  }

  getNoteTypeClass(type: string): string {
    const m: Record<string, string> = { CLINICAL: 'note-clinical', OBSERVATION: 'note-obs', FOLLOW_UP: 'note-followup', CONCERN: 'note-concern', GENERAL: 'note-general' };
    return m[type] || 'note-general';
  }

  getNoteTypeIcon(type: string): string {
    const m: Record<string, string> = { CLINICAL: '🩺', OBSERVATION: '👁️', FOLLOW_UP: '📅', CONCERN: '⚠️', GENERAL: '📝' };
    return m[type] || '📝';
  }

  // ── APOE & Diagnosis Stage Helpers ───────────────────────────────────────

  getApoeRiskPoints(status: string): number {
    const map: Record<string, number> = {
      E2_E2: -8, E2_E3: -4, E3_E3: 0, E2_E4: 5, E3_E4: 15, E4_E4: 25, NOT_TESTED: 0
    };
    return map[status] || 0;
  }

  getApoeColor(status: string): string {
    const map: Record<string, string> = {
      E2_E2: '#10b981', E2_E3: '#34d399', E3_E3: '#94a3b8',
      E2_E4: '#fbbf24', E3_E4: '#f97316', E4_E4: '#ef4444', NOT_TESTED: '#64748b'
    };
    return map[status] || '#64748b';
  }

  getStageClass(stage: string): string {
    const m: Record<string, string> = {
      PRECLINICAL: 'stage-preclinical', MCI: 'stage-mci',
      MILD: 'stage-mild', MODERATE: 'stage-moderate', SEVERE: 'stage-severe'
    };
    return m[stage] || '';
  }

  getStageEmoji(stage: string): string {
    const m: Record<string, string> = {
      PRECLINICAL: '🟢', MCI: '🟡', MILD: '🟠', MODERATE: '🔴', SEVERE: '⛔'
    };
    return m[stage] || '⚪';
  }


  // ═══ APPOINTMENTS ═══

  loadAppointments(recordId: number): void {
    this.appointmentsLoading = true;
    this.http.get<ApiResponse<any[]>>(`${this.appointmentApiUrl}/medical-record/${recordId}`).subscribe({
      next: (r) => {
        this.appointments = r.data || [];
        this.appointmentsLoading = false;
        this.cdr.markForCheck();
        this.loadAppointmentStats(recordId);
      },
      error: () => { this.appointments = []; this.appointmentsLoading = false; this.cdr.markForCheck(); }
    });
  }

  loadAppointmentStats(recordId: number): void {
    this.http.get<ApiResponse<any>>(`${this.appointmentApiUrl}/medical-record/${recordId}/stats`).subscribe({
      next: (r) => { this.appointmentStats = r.data; this.cdr.markForCheck(); },
      error: () => {}
    });
  }

  openAppointmentForm(appt?: any): void {
    this.editingAppointment = appt || null;
    if (appt) {
      const local = appt.scheduledAt ? appt.scheduledAt.substring(0, 16) : '';
      this.apptFormData = {
        doctorName: appt.doctorName || '', specialty: appt.specialty || '',
        appointmentType: appt.appointmentType || 'NEUROLOGIST',
        scheduledAt: local, location: appt.location || '',
        notes: appt.notes || '', status: appt.status || 'SCHEDULED'
      };
    } else {
      this.apptFormData = {
        doctorName: '', specialty: '', appointmentType: 'NEUROLOGIST',
        scheduledAt: '', location: '', notes: '', status: 'SCHEDULED'
      };
    }
    this.showAppointmentForm = true;
    this.cdr.markForCheck();
  }

  closeAppointmentForm(): void { this.showAppointmentForm = false; this.editingAppointment = null; this.cdr.markForCheck(); }

  submitAppointment(): void {
    if (!this.apptFormData.scheduledAt || !this.selectedRecord) return;
    const payload = { ...this.apptFormData, medicalRecordId: this.selectedRecord.id };
    const req = this.editingAppointment
      ? this.http.put<ApiResponse<any>>(`${this.appointmentApiUrl}/${this.editingAppointment.id}`, payload)
      : this.http.post<ApiResponse<any>>(this.appointmentApiUrl, payload);
    req.subscribe({
      next: () => { this.loadAppointments(this.selectedRecord!.id); this.closeAppointmentForm(); },
      error: (err) => {
        const msg = err?.error?.message || err?.error?.error || err?.message || 'Unknown error';
        this.error = 'Failed to save appointment: ' + msg;
        console.error('Appointment save error:', err);
        this.cdr.markForCheck();
      }
    });
  }

  completeAppointment(id: number): void {
    this.http.patch<ApiResponse<any>>(`${this.appointmentApiUrl}/${id}/complete`, {}).subscribe({
      next: () => { if (this.selectedRecord) this.loadAppointments(this.selectedRecord.id); },
      error: () => { this.error = 'Failed'; this.cdr.markForCheck(); }
    });
  }

  cancelAppointment(id: number): void {
    if (!confirm('Cancel this appointment?')) return;
    this.http.patch<ApiResponse<any>>(`${this.appointmentApiUrl}/${id}/cancel`, {}).subscribe({
      next: () => { if (this.selectedRecord) this.loadAppointments(this.selectedRecord.id); },
      error: () => { this.error = 'Failed'; this.cdr.markForCheck(); }
    });
  }

  deleteAppointment(id: number): void {
    if (!confirm('Delete this appointment permanently?')) return;
    this.http.delete<ApiResponse<void>>(`${this.appointmentApiUrl}/${id}`).subscribe({
      next: () => { if (this.selectedRecord) this.loadAppointments(this.selectedRecord.id); },
      error: () => { this.error = 'Failed to delete'; this.cdr.markForCheck(); }
    });
  }

  getApptStatusColor(status: string): string { return this.statusColors[status] || '#64748b'; }
  getApptUpcoming(): any[] { return this.appointments.filter(a => a.status === 'SCHEDULED').sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime()); }
  getApptCompleted(): any[] { return this.appointments.filter(a => a.status === 'COMPLETED'); }
  getApptMissed(): any[] { return this.appointments.filter(a => a.status === 'MISSED'); }

  formatDateTime(dt: string): string {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  // ═══ MEDICATION INTERACTIONS ═══

  loadInteractions(recordId: number): void {
    this.interactionsLoading = true;
    this.http.get<ApiResponse<any>>(`/api/medications/medical-record/${recordId}/interactions`).subscribe({
      next: (r) => {
        this.interactionAlerts  = r.data?.alerts  || [];
        this.interactionSummary = r.data?.summary || null;
        this.interactionsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.interactionAlerts = []; this.interactionsLoading = false; this.cdr.markForCheck(); }
    });
  }

  getAlertBorderColor(severity: string, type: string): string {
    if (type === 'PROTECTIVE') return '#10b981';
    return severity === 'HIGH' ? '#dc2626' : severity === 'MEDIUM' ? '#f97316' : '#f59e0b';
  }

  getAlertBgColor(severity: string, type: string): string {
    if (type === 'PROTECTIVE') return '#f0fdf4';
    return severity === 'HIGH' ? '#fef2f2' : severity === 'MEDIUM' ? '#fff7ed' : '#fffbeb';
  }

  getAlertIcon(type: string, severity: string): string {
    if (type === 'PROTECTIVE') return '🛡️';
    if (type === 'INTERACTION') return '⚡';
    return severity === 'HIGH' ? '🚨' : '⚠️';
  }

  // ═══ RISK TREND ═══

  loadRiskTrend(recordId: number): void {
    this.riskTrendLoading = true;
    this.http.get<ApiResponse<any>>(`/api/medical-records/${recordId}/risk-trend`).subscribe({
      next: (r) => { this.riskTrend = r.data; this.riskTrendLoading = false; this.cdr.markForCheck(); },
      error: () => { this.riskTrend = null; this.riskTrendLoading = false; this.cdr.markForCheck(); }
    });
  }

  getTrendDataPoints(): any[] { return this.riskTrend?.dataPoints || []; }

  getTrendChartPoints(dataPoints: any[]): string {
    if (!dataPoints || dataPoints.length < 2) return '';
    const n = dataPoints.length;
    return dataPoints.map((pt, i) => {
      const x = 30 + (i / (n - 1)) * 540;
      const y = 150 - (pt.score / 99) * 140;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  getTrendDots(dataPoints: any[]): { x: number; y: number; score: number; color: string }[] {
    if (!dataPoints || dataPoints.length === 0) return [];
    const n = dataPoints.length;
    const colorMap: Record<string, string> = { LOW: '#10b981', MEDIUM: '#f59e0b', HIGH: '#f97316', CRITICAL: '#ef4444' };
    return dataPoints.map((pt, i) => ({
      x: n === 1 ? 300 : 30 + (i / (n - 1)) * 540,
      y: 150 - (pt.score / 99) * 140,
      score: pt.score,
      color: colorMap[pt.riskLevel] || '#94a3b8'
    }));
  }

  getProjection(key: string): any { return this.riskTrend?.projections?.[key]; }


}
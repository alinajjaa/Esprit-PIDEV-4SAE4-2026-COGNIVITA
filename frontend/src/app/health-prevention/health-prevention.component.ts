import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import {
  HealthPreventionService,
  HealthProfile,
  WellnessDashboard,
  HealthRecommendation,
  WellnessActivity,
  ActivityLevel,
  DietQuality,
  StressLevel
} from '../services/health-prevention.service';

@Component({
  selector: 'app-health-prevention',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './health-prevention.component.html',
  styleUrls: ['./health-prevention.component.css']
})
export class HealthPreventionComponent implements OnInit {

  // ─── State ────────────────────────────────────────────────────────────────
  activeTab: 'dashboard' | 'profile' | 'recommendations' | 'activities' = 'dashboard';
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // ─── Data ─────────────────────────────────────────────────────────────────
  userId = 1; // Resolved from queryParams on init
  userName = '';
  healthProfile: HealthProfile | null = null;
  dashboard: WellnessDashboard | null = null;
  recommendations: HealthRecommendation[] = [];
  activities: WellnessActivity[] = [];

  // ─── Form ─────────────────────────────────────────────────────────────────
  profileForm: HealthProfile = {
    userId: this.userId,
    physicalActivityLevel: 'SEDENTARY',
    dietQuality: 'AVERAGE',
    stressLevel: 'MODERATE',
    alcoholConsumption: 'NONE',
    socialEngagementLevel: 'MODERATE',
    smokingStatus: false,
    sleepHoursPerNight: 7,
    cognitiveTrainingFrequency: 'Weekly'
  };

  activityForm: WellnessActivity = {
    healthProfileId: 0,
    activityName: '',
    activityDate: new Date().toISOString().slice(0, 16),
    activityType: 'WALKING',
    durationMinutes: 30,
    intensityLevel: 'MODERATE'
  };

  showActivityForm = false;
  showProfileForm = false;
  isEditMode = false;

  // ─── Enum options for dropdowns ───────────────────────────────────────────
  activityLevels = ['SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE', 'EXTREMELY_ACTIVE'];
  dietQualities = ['POOR', 'BELOW_AVERAGE', 'AVERAGE', 'GOOD', 'EXCELLENT'];
  stressLevels = ['MINIMAL', 'LOW', 'MODERATE', 'HIGH', 'VERY_HIGH'];
  alcoholOptions = ['NONE', 'OCCASIONAL', 'MODERATE', 'HEAVY'];
  engagementLevels = ['ISOLATED', 'LOW', 'MODERATE', 'HIGH', 'VERY_HIGH'];
  activityTypes = ['AEROBIC', 'STRENGTH_TRAINING', 'FLEXIBILITY', 'COGNITIVE', 'SOCIAL', 'MEDITATION', 'WALKING', 'SWIMMING', 'YOGA', 'OTHER'];
  intensityLevels = ['LIGHT', 'MODERATE', 'VIGOROUS', 'MAXIMUM'];
  trainingFrequencies = ['Daily', 'Weekly', 'Monthly', 'Rarely', 'Never'];

  constructor(
    private healthPreventionService: HealthPreventionService,
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
        this.profileForm.userId = this.userId;
      }
      this.loadData();
    });
  }

  goBackToMedicalRecords(): void {
    this.router.navigate(['/medical-records']);
  }

  loadData(): void {
    this.isLoading = true;
    this.healthPreventionService.getProfileByUserId(this.userId).subscribe({
      next: (res) => {
        if (res.success) {
          this.healthProfile = res.data;
          this.loadDashboard();
          this.loadRecommendations();
          this.loadActivities();
        } else {
          this.isLoading = false;
        }
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  loadDashboard(): void {
    this.healthPreventionService.getDashboard(this.userId).subscribe({
      next: (res) => {
        if (res.success) this.dashboard = res.data;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadRecommendations(): void {
    if (!this.healthProfile?.id) return;
    this.healthPreventionService.getRecommendationsByProfile(this.healthProfile.id).subscribe({
      next: (res) => { if (res.success) this.recommendations = res.data; }
    });
  }

  loadActivities(): void {
    if (!this.healthProfile?.id) return;
    this.healthPreventionService.getActivitiesByProfile(this.healthProfile.id).subscribe({
      next: (res) => { if (res.success) this.activities = res.data; }
    });
  }

  // ─── Profile Actions ──────────────────────────────────────────────────────
  saveProfile(): void {
    this.isLoading = true;
    const action = this.isEditMode && this.healthProfile?.id
      ? this.healthPreventionService.updateProfile(this.healthProfile.id, this.profileForm)
      : this.healthPreventionService.createProfile({ ...this.profileForm, userId: this.userId });

    action.subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.healthProfile = res.data;
          this.showProfileForm = false;
          this.successMessage = this.isEditMode ? 'Profile updated!' : 'Profile created!';
          this.loadDashboard();
          // Push wellness risk score to medical record so risk score recalculates
          this.pushWellnessRiskToMedicalRecord(res.data);
          setTimeout(() => this.successMessage = '', 3000);
        } else {
          this.errorMessage = res.message;
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Failed to save profile. Please try again.';
      }
    });
  }

  /**
   * After saving a health profile, find the user's medical record and push
   * the wellness risk contribution so the overall risk score updates.
   * wellnessRiskContribution = 100 - wellnessScore
   * (high wellness = low risk; low wellness = high risk)
   */
  private pushWellnessRiskToMedicalRecord(profile: HealthProfile): void {
    if (!this.userId) return;
    const medicalApiUrl = '/api/medical-records';
    // Get the medical record for this user
    this.http.get<any>(`${medicalApiUrl}/user/${this.userId}`).subscribe({
      next: (res) => {
        const records: any[] = res.data || [];
        if (!records.length) return;
        const wellnessScore = profile.wellnessScore ?? 50;
        const wellnessRiskContribution = Math.round(100 - wellnessScore);
        // Push to every record for this user (usually just one)
        for (const record of records) {
          this.http.patch(`${medicalApiUrl}/${record.id}/wellness-risk`,
            { wellnessRiskContribution }
          ).subscribe({ error: () => {} }); // silent failure OK
        }
      },
      error: () => {} // medical records service may not be running — silent
    });
  }

  editProfile(): void {
    if (!this.healthProfile) return;
    this.profileForm = { ...this.healthProfile };
    this.isEditMode = true;
    this.showProfileForm = true;
    this.activeTab = 'profile';
  }

  createNewProfile(): void {
    this.profileForm = {
      userId: this.userId,
      physicalActivityLevel: 'SEDENTARY',
      dietQuality: 'AVERAGE',
      stressLevel: 'MODERATE',
      alcoholConsumption: 'NONE',
      socialEngagementLevel: 'MODERATE',
      smokingStatus: false,
      sleepHoursPerNight: 7,
      cognitiveTrainingFrequency: 'Weekly'
    };
    this.isEditMode = false;
    this.showProfileForm = true;
    this.activeTab = 'profile';
  }

  // ─── Recommendation Actions ───────────────────────────────────────────────
  generateRecommendations(): void {
    if (!this.healthProfile?.id) return;
    this.isLoading = true;
    this.healthPreventionService.generateRecommendations(this.healthProfile.id).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.recommendations = [...this.recommendations, ...res.data];
          this.successMessage = `${res.data.length} recommendations generated!`;
          setTimeout(() => this.successMessage = '', 3000);
        }
      },
      error: () => { this.isLoading = false; this.errorMessage = 'Failed to generate recommendations.'; }
    });
  }

  completeRecommendation(id: number): void {
    this.healthPreventionService.completeRecommendation(id).subscribe({
      next: (res) => {
        if (res.success) {
          const idx = this.recommendations.findIndex(r => r.id === id);
          if (idx !== -1) this.recommendations[idx] = res.data;
          this.successMessage = 'Recommendation completed!';
          setTimeout(() => this.successMessage = '', 3000);
        }
      }
    });
  }

  deleteRecommendation(id: number): void {
    this.healthPreventionService.deleteRecommendation(id).subscribe({
      next: (res) => {
        if (res.success) {
          this.recommendations = this.recommendations.filter(r => r.id !== id);
        }
      }
    });
  }

  // ─── Activity Actions ─────────────────────────────────────────────────────
  logActivity(): void {
    if (!this.healthProfile?.id) return;
    this.activityForm.healthProfileId = this.healthProfile.id;
    this.isLoading = true;
    this.healthPreventionService.logActivity(this.activityForm).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.activities = [res.data, ...this.activities];
          this.showActivityForm = false;
          this.successMessage = 'Activity logged!';
          this.activityForm = {
            healthProfileId: this.healthProfile!.id!,
            activityName: '',
            activityDate: new Date().toISOString().slice(0, 16),
            activityType: 'WALKING',
            durationMinutes: 30,
            intensityLevel: 'MODERATE'
          };
          setTimeout(() => this.successMessage = '', 3000);
        }
      },
      error: () => { this.isLoading = false; this.errorMessage = 'Failed to log activity.'; }
    });
  }

  deleteActivity(id: number): void {
    this.healthPreventionService.deleteActivity(id).subscribe({
      next: (res) => {
        if (res.success) this.activities = this.activities.filter(a => a.id !== id);
      }
    });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────
  getWellnessColor(score: number): string {
    if (score >= 80) return '#00ff88';
    if (score >= 60) return '#00ccff';
    if (score >= 40) return '#ffaa00';
    return '#ff4444';
  }

  getPriorityColor(priority: string): string {
    return { CRITICAL: '#ff4444', HIGH: '#ff8800', MEDIUM: '#ffcc00', LOW: '#00cc88' }[priority] || '#888';
  }

  getStatusColor(status: string): string {
    return { ACTIVE: '#00ccff', COMPLETED: '#00ff88', DISMISSED: '#888', PENDING_REVIEW: '#ffcc00' }[status] || '#888';
  }

  formatEnum(value: string): string {
    return value?.replace(/_/g, ' ') ?? '';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString();
  }

  getCircleOffset(score: number): number {
    const circumference = 2 * Math.PI * 54;
    return circumference - (score / 100) * circumference;
  }

  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    this.errorMessage = '';
  }
}

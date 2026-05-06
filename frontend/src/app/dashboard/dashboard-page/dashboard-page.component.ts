import { Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AlertTypePressureDto, DashboardService, PatientSafetyBurdenDto } from '../../services/dashboard.service';

interface DashboardSystemStatus {
  highRiskPatients: number;
  unreadAlerts: number;
  monitoringStatus: 'ACTIVE' | 'IDLE';
}

@Component({
  selector: 'app-dashboard-page',
  standalone: false,
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css'
})
export class DashboardPageComponent implements OnInit, OnDestroy {
  systemStatus: DashboardSystemStatus | null = null;
  criticalPatient: PatientSafetyBurdenDto | null = null;
  secondsSinceUpdate = 0;
  loadingTop = true;
  topError = false;

  private updateTimer?: ReturnType<typeof setInterval>;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadTopInsights();
    this.updateTimer = setInterval(() => {
      this.secondsSinceUpdate += 1;
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.updateTimer) {
      clearInterval(this.updateTimer);
    }
  }

  get hasTopData(): boolean {
    return this.systemStatus != null && this.criticalPatient != null;
  }

  private loadTopInsights(): void {
    this.loadingTop = true;
    this.topError = false;
    forkJoin({
      ranking: this.dashboardService.getPatientRanking(),
      pressure: this.dashboardService.getAlertPressure()
    }).subscribe({
      next: ({ ranking, pressure }) => {
        this.criticalPatient = this.selectCriticalPatient(ranking);
        this.systemStatus = this.buildSystemStatus(ranking, pressure);
        this.loadingTop = false;
      },
      error: () => {
        this.criticalPatient = null;
        this.systemStatus = null;
        this.topError = true;
        this.loadingTop = false;
      }
    });
  }

  private selectCriticalPatient(ranking: PatientSafetyBurdenDto[]): PatientSafetyBurdenDto | null {
    if (ranking.length === 0) {
      return null;
    }
    return ranking[0];
  }

  private buildSystemStatus(ranking: PatientSafetyBurdenDto[], pressure: AlertTypePressureDto[]): DashboardSystemStatus | null {
    if (ranking.length === 0 && pressure.length === 0) {
      return null;
    }
    const highRiskPatients = ranking.filter((row) => row.riskScore >= 18).length;
    const unreadAlerts = pressure.reduce((sum, row) => sum + row.unreadCount, 0);
    const monitoringStatus: 'ACTIVE' | 'IDLE' = ranking.length > 0 || pressure.length > 0 ? 'ACTIVE' : 'IDLE';
    return {
      highRiskPatients,
      unreadAlerts,
      monitoringStatus
    };
  }
}

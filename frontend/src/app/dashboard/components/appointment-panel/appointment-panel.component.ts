import { Component, OnInit } from '@angular/core';
import { AppointmentComplianceDto, DashboardService } from '../../../services/dashboard.service';

type ComplianceState = 'high' | 'low';

@Component({
  selector: 'app-appointment-panel',
  standalone: false,
  templateUrl: './appointment-panel.component.html',
  styleUrl: './appointment-panel.component.css'
})
export class AppointmentPanelComponent implements OnInit {
  rows: AppointmentComplianceDto[] = [];
  loading = true;
  error = false;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadCompliance();
  }

  get hasData(): boolean {
    return this.rows.length > 0;
  }

  getCompletionRate(completed: number, total: number): number {
    if (total === 0) {
      return 0;
    }
    return Math.round((completed / total) * 100);
  }

  getComplianceState(completed: number, total: number): ComplianceState {
    return this.getCompletionRate(completed, total) >= 70 ? 'high' : 'low';
  }

  private loadCompliance(): void {
    this.loading = true;
    this.error = false;
    this.dashboardService.getAppointmentCompliance().subscribe({
      next: (rows) => {
        this.rows = rows;
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.error = true;
        this.loading = false;
      }
    });
  }
}

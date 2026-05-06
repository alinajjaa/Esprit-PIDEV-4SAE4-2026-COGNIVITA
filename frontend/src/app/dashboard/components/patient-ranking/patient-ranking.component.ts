import { Component, OnInit } from '@angular/core';
import { DashboardService, PatientSafetyBurdenDto } from '../../../services/dashboard.service';

@Component({
  selector: 'app-patient-ranking',
  standalone: false,
  templateUrl: './patient-ranking.component.html',
  styleUrl: './patient-ranking.component.css'
})
export class PatientRankingComponent implements OnInit {
  ranking: PatientSafetyBurdenDto[] = [];
  loading = true;
  error = false;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadRanking();
  }

  get hasData(): boolean {
    return this.ranking.length > 0;
  }

  getRiskLabel(score: number): string {
    if (score >= 18) {
      return 'HIGH';
    }
    if (score >= 10) {
      return 'MEDIUM';
    }
    return 'LOW';
  }

  getRiskClass(score: number): string {
    if (score >= 18) {
      return 'high';
    }
    if (score >= 10) {
      return 'medium';
    }
    return 'low';
  }

  private loadRanking(): void {
    this.loading = true;
    this.error = false;
    this.dashboardService.getPatientRanking().subscribe({
      next: (rows) => {
        this.ranking = rows;
        this.loading = false;
      },
      error: () => {
        this.ranking = [];
        this.error = true;
        this.loading = false;
      }
    });
  }
}

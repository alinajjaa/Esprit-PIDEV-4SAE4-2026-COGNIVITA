import { Component, OnInit } from '@angular/core';
import { DashboardService, MmseMonthlySeverityDto } from '../../../services/dashboard.service';

type MmseTrend = 'up' | 'down' | 'steady';

@Component({
  selector: 'app-mmse-panel',
  standalone: false,
  templateUrl: './mmse-panel.component.html',
  styleUrl: './mmse-panel.component.css'
})
export class MmsePanelComponent implements OnInit {
  months: MmseMonthlySeverityDto[] = [];
  loading = true;
  error = false;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadMmseSeverity();
  }

  get hasData(): boolean {
    return this.months.length > 0;
  }

  getWorstMonth(): string | null {
    if (!this.hasData) {
      return null;
    }
    const worst = this.months.reduce((lowest, month) => {
      const lowestScore = lowest.avgScore ?? Number.POSITIVE_INFINITY;
      const currentScore = month.avgScore ?? Number.POSITIVE_INFINITY;
      return currentScore < lowestScore ? month : lowest;
    }, this.months[0]);
    return worst.month;
  }

  getTrend(index: number): MmseTrend {
    if (index === 0) {
      return 'steady';
    }
    const current = this.months[index].avgScore;
    const previous = this.months[index - 1].avgScore;
    if (current == null || previous == null) {
      return 'steady';
    }
    if (current > previous) {
      return 'up';
    }
    if (current < previous) {
      return 'down';
    }
    return 'steady';
  }

  getTrendLabel(index: number): string {
    const trend = this.getTrend(index);
    if (trend === 'up') {
      return '\u2191 improving';
    }
    if (trend === 'down') {
      return '\u2193 declining';
    }
    return '\u2192 stable';
  }

  private loadMmseSeverity(): void {
    this.loading = true;
    this.error = false;
    this.dashboardService.getMmseSeverity().subscribe({
      next: (rows) => {
        this.months = rows;
        this.loading = false;
      },
      error: () => {
        this.months = [];
        this.error = true;
        this.loading = false;
      }
    });
  }
}

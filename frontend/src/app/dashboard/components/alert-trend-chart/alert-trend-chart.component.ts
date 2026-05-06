import { Component, OnInit } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { DashboardService, AlertTrendResponseDto } from '../../../services/dashboard.service';

@Component({
  selector: 'app-alert-trend-chart',
  standalone: false,
  templateUrl: './alert-trend-chart.component.html',
  styleUrl: './alert-trend-chart.component.css'
})
export class AlertTrendChartComponent implements OnInit {
  trendRows: AlertTrendResponseDto[] = [];
  loading = true;
  error = false;
  hasData = false;

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'OUT_OF_ZONE',
        data: [],
        borderColor: '#f87171',
        backgroundColor: 'rgba(248, 113, 113, 0.12)',
        pointBackgroundColor: '#fca5a5',
        pointBorderColor: '#7f1d1d',
        pointRadius: 3,
        pointHoverRadius: 4,
        tension: 0.35
      },
      {
        label: 'NO_MOVEMENT',
        data: [],
        borderColor: '#facc15',
        backgroundColor: 'rgba(250, 204, 21, 0.12)',
        pointBackgroundColor: '#fde68a',
        pointBorderColor: '#713f12',
        pointRadius: 3,
        pointHoverRadius: 4,
        tension: 0.35
      }
    ]
  };

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: {
          color: '#dbeafe',
          boxWidth: 16
        }
      },
      tooltip: {
        backgroundColor: 'rgba(2, 6, 23, 0.9)',
        titleColor: '#ffffff',
        bodyColor: '#dbeafe',
        borderColor: 'rgba(148, 163, 184, 0.35)',
        borderWidth: 1
      }
    },
    scales: {
      x: {
        grid: {
          color: 'rgba(148, 163, 184, 0.08)'
        },
        ticks: {
          color: '#93c5fd'
        }
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(148, 163, 184, 0.12)'
        },
        ticks: {
          precision: 0,
          color: '#93c5fd'
        }
      }
    },
    elements: {
      line: {
        borderWidth: 2
      }
    }
  };

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadTrendData();
  }

  getLatestOutOfZone(): number | null {
    if (!this.hasData) {
      return null;
    }
    return this.trendRows[this.trendRows.length - 1].outOfZoneCount;
  }

  getLatestNoMovement(): number | null {
    if (!this.hasData) {
      return null;
    }
    return this.trendRows[this.trendRows.length - 1].noMovementCount;
  }

  getAvgOutOfZone(): number | null {
    if (!this.hasData) {
      return null;
    }
    const total = this.trendRows.reduce((sum, row) => sum + row.outOfZoneCount, 0);
    return Number((total / this.trendRows.length).toFixed(1));
  }

  getAvgNoMovement(): number | null {
    if (!this.hasData) {
      return null;
    }
    const total = this.trendRows.reduce((sum, row) => sum + row.noMovementCount, 0);
    return Number((total / this.trendRows.length).toFixed(1));
  }

  private loadTrendData(): void {
    this.loading = true;
    this.error = false;
    this.dashboardService.getAlertTrend().subscribe({
      next: (rows) => {
        this.trendRows = rows;
        this.hasData = rows.length > 0;
        if (this.hasData) {
          this.chartData = {
            labels: rows.map((row) => row.date),
            datasets: [
              {
                ...this.chartData.datasets[0],
                data: rows.map((row) => row.outOfZoneCount)
              },
              {
                ...this.chartData.datasets[1],
                data: rows.map((row) => row.noMovementCount)
              }
            ]
          };
        } else {
          this.chartData = {
            ...this.chartData,
            labels: [],
            datasets: [
              { ...this.chartData.datasets[0], data: [] },
              { ...this.chartData.datasets[1], data: [] }
            ]
          };
        }
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.trendRows = [];
        this.hasData = false;
        this.chartData = {
          ...this.chartData,
          labels: [],
          datasets: [
            { ...this.chartData.datasets[0], data: [] },
            { ...this.chartData.datasets[1], data: [] }
          ]
        };
        this.loading = false;
      }
    });
  }
}

import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule } from 'ng-apexcharts';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexLegend,
  ApexPlotOptions,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexFill
} from 'ng-apexcharts';

type RdvPerDay = { day: string; count: number };
type RdvPerHour = { hour: number | string; count: number };
type TopMedication = { name: string; count: number };

export type DoctorStatsLike = {
  total: number;
  upcoming: number;
  today: number;
  uniquePatients: number;
  cancellationRate?: number;
  byStatus?: Record<string, number>;
  rdvPerDay: RdvPerDay[];
  rdvPerHour: RdvPerHour[];
  topMedications: TopMedication[];
};

@Component({
  selector: 'app-stats-charts',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  template: `
    <div class="charts-grid">
      <div class="chart-card">
        <h3>Appointments per Day</h3>
        <apx-chart
          [series]="perDaySeries"
          [chart]="perDayChart"
          [xaxis]="perDayXAxis"
          [yaxis]="perDayYAxis"
          [stroke]="perDayStroke"
          [dataLabels]="dataLabels"
          [tooltip]="tooltip"
          [fill]="fill"
        ></apx-chart>
      </div>

      <div class="chart-card">
        <h3>Rendez-vous by Status</h3>
        <apx-chart
          [series]="statusSeries"
          [chart]="statusChart"
          [labels]="statusLabels"
          [legend]="legend"
          [tooltip]="tooltip"
        ></apx-chart>
      </div>

      <div class="chart-card">
        <h3>Peak Hours</h3>
        <apx-chart
          [series]="perHourSeries"
          [chart]="perHourChart"
          [xaxis]="perHourXAxis"
          [plotOptions]="plotOptions"
          [dataLabels]="dataLabels"
          [tooltip]="tooltip"
        ></apx-chart>
      </div>

      <div class="chart-card">
        <h3>Top Medications</h3>
        <apx-chart
          [series]="medSeries"
          [chart]="medChart"
          [xaxis]="medXAxis"
          [plotOptions]="plotOptions"
          [dataLabels]="dataLabels"
          [tooltip]="tooltip"
        ></apx-chart>
      </div>
    </div>
  `,
  styles: [`
    .charts-grid{
      display:grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap:14px;
    }
    @media (max-width: 900px){
      .charts-grid{ grid-template-columns: 1fr; }
    }
    .chart-card{
      background:#fff;
      border:1px solid #e6e6e6;
      border-radius:14px;
      padding:14px;
      box-shadow:0 8px 22px rgba(0,0,0,.05);
    }
    .chart-card h3{
      margin:0 0 10px 0;
      font-size:16px;
      font-weight:800;
      color:#2c3e50;
    }
  `],
})
export class StatsChartsComponent implements OnChanges {
  @Input() stats: DoctorStatsLike | null = null;

  // shared
  dataLabels: ApexDataLabels = { enabled: false };
  tooltip: ApexTooltip = { theme: 'light' };
  legend: ApexLegend = { position: 'bottom' };
  fill: ApexFill = { opacity: 0.9 };

  // per day (area/line)
  perDaySeries: ApexAxisChartSeries = [{ name: 'RDV', data: [] }];
  perDayChart: ApexChart = { type: 'area', height: 280, toolbar: { show: false }, zoom: { enabled: false } };
  perDayXAxis: ApexXAxis = { categories: [], labels: { rotate: -45 } };
  perDayYAxis: ApexYAxis = { min: 0, tickAmount: 4 };
  perDayStroke: ApexStroke = { curve: 'smooth', width: 3 };

  // status (donut)
  statusSeries: number[] = [];
  statusLabels: string[] = [];
  statusChart: ApexChart = { type: 'donut', height: 280 };

  // per hour (bar)
  perHourSeries: ApexAxisChartSeries = [{ name: 'RDV', data: [] }];
  perHourChart: ApexChart = { type: 'bar', height: 280, toolbar: { show: false } };
  perHourXAxis: ApexXAxis = { categories: [] };

  // medications (bar horizontal)
  medSeries: ApexAxisChartSeries = [{ name: 'Count', data: [] }];
  medChart: ApexChart = { type: 'bar', height: 280, toolbar: { show: false } };
  medXAxis: ApexXAxis = { categories: [] };

  plotOptions: ApexPlotOptions = {
    bar: {
      borderRadius: 8,
      columnWidth: '55%',
      horizontal: false
    }
  };

  ngOnChanges(): void {
    if (!this.stats) return;

    // per day
    const days = (this.stats.rdvPerDay ?? []).slice().sort((a,b) => a.day.localeCompare(b.day));
    this.perDayXAxis = { ...this.perDayXAxis, categories: days.map(x => x.day) };
    this.perDaySeries = [{ name: 'RDV', data: days.map(x => x.count) }];

    // status donut
    const byStatus = this.stats.byStatus ?? {};
    const statusEntries = Object.entries(byStatus).sort((a,b) => (b[1] ?? 0) - (a[1] ?? 0));
    this.statusLabels = statusEntries.map(x => x[0]);
    this.statusSeries = statusEntries.map(x => x[1] ?? 0);

    // per hour bar
    const hours = (this.stats.rdvPerHour ?? []).slice().sort((a,b) => Number(a.hour) - Number(b.hour));
    this.perHourXAxis = { categories: hours.map(x => String(x.hour).padStart(2,'0') + ':00') };
    this.perHourSeries = [{ name: 'RDV', data: hours.map(x => x.count) }];

    // meds top 10 (horizontal bars look better)
    const meds = (this.stats.topMedications ?? []).slice(0, 10);
    this.medXAxis = { categories: meds.map(x => x.name) };
    this.medSeries = [{ name: 'Count', data: meds.map(x => x.count) }];
    this.plotOptions = { bar: { ...this.plotOptions.bar, horizontal: true } };
  }
}

import { Component, OnInit } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { AppointmentComplianceDto, DashboardService, MmseMonthlySeverityDto, PatientSafetyBurdenDto, AlertTypePressureDto } from '../../../services/dashboard.service';

type CardState = 'stable' | 'warning' | 'critical';
type CardDirection = 'up' | 'down';
type CardAccent = 'neutral' | 'alert' | 'positive' | 'warning';

interface KpiCard {
  label: string;
  value: string;
  trend: string;
  direction: CardDirection;
  state: CardState;
  accent: CardAccent;
  context: string;
}

interface ApiBlock<T> {
  ok: boolean;
  data: T;
}

@Component({
  selector: 'app-kpi-cards',
  standalone: false,
  templateUrl: './kpi-cards.component.html',
  styleUrl: './kpi-cards.component.css'
})
export class KpiCardsComponent implements OnInit {
  cards: KpiCard[] = [];
  loading = true;
  error = false;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadCards();
  }

  get hasData(): boolean {
    return this.cards.length > 0;
  }

  getArrow(direction: CardDirection): string {
    return direction === 'up' ? '\u2191' : '\u2193';
  }

  getStateLabel(state: CardState): string {
    if (state === 'stable') {
      return 'Stable';
    }
    if (state === 'warning') {
      return 'Warning';
    }
    return 'Critical';
  }

  private loadCards(): void {
    this.loading = true;
    this.error = false;
    forkJoin({
      ranking: this.wrapCall(this.dashboardService.getPatientRanking(), [] as PatientSafetyBurdenDto[]),
      pressure: this.wrapCall(this.dashboardService.getAlertPressure(), [] as AlertTypePressureDto[]),
      compliance: this.wrapCall(this.dashboardService.getAppointmentCompliance(), [] as AppointmentComplianceDto[]),
      mmse: this.wrapCall(this.dashboardService.getMmseSeverity(), [] as MmseMonthlySeverityDto[])
    }).subscribe(({ ranking, pressure, compliance, mmse }) => {
      const allFailed = !ranking.ok && !pressure.ok && !compliance.ok && !mmse.ok;
      if (allFailed) {
        this.cards = [];
        this.error = true;
      } else {
        this.cards = this.buildCards(ranking, pressure, compliance, mmse);
        this.error = false;
      }
      this.loading = false;
    });
  }

  private wrapCall<T>(observable: Observable<T>, fallback: T): Observable<ApiBlock<T>> {
    return observable.pipe(
      map((data: T): ApiBlock<T> => ({ ok: true, data })),
      catchError(() => of({ ok: false, data: fallback }))
    );
  }

  private buildCards(
    rankingBlock: ApiBlock<PatientSafetyBurdenDto[]>,
    pressureBlock: ApiBlock<AlertTypePressureDto[]>,
    complianceBlock: ApiBlock<AppointmentComplianceDto[]>,
    mmseBlock: ApiBlock<MmseMonthlySeverityDto[]>
  ): KpiCard[] {
    const ranking = rankingBlock.data;
    const pressure = pressureBlock.data;
    const compliance = complianceBlock.data;
    const mmse = mmseBlock.data;

    const activePatients = ranking.length;
    const highRiskPatients = ranking.filter((row) => row.riskScore >= 18).length;

    const totalAlerts = pressure.reduce((sum, row) => sum + row.totalCount, 0);
    const unreadAlerts = pressure.reduce((sum, row) => sum + row.unreadCount, 0);
    const unreadRate = totalAlerts > 0 ? (unreadAlerts / totalAlerts) * 100 : 0;

    const totalAppointments = compliance.reduce((sum, row) => sum + row.totalAppointments, 0);
    const completedAppointments = compliance.reduce((sum, row) => sum + row.completedCount, 0);
    const completionRate = totalAppointments > 0 ? (completedAppointments / totalAppointments) * 100 : 0;

    const mmseScores = mmse.map((row) => row.avgScore).filter((score): score is number => score != null);
    const avgMmse = mmseScores.length > 0
      ? Number((mmseScores.reduce((sum, score) => sum + score, 0) / mmseScores.length).toFixed(1))
      : null;

    return [
      {
        label: 'Active Patients',
        value: rankingBlock.ok ? String(activePatients) : '-',
        trend: rankingBlock.ok ? `${highRiskPatients} high-risk` : 'Backend unavailable',
        direction: rankingBlock.ok && highRiskPatients > 0 ? 'up' : 'down',
        state: rankingBlock.ok ? (highRiskPatients > 0 ? 'warning' : 'stable') : 'warning',
        accent: 'neutral',
        context: 'current ranking window'
      },
      {
        label: 'Unread Alerts',
        value: pressureBlock.ok ? String(unreadAlerts) : '-',
        trend: pressureBlock.ok ? `${unreadRate.toFixed(1)}% unread` : 'Backend unavailable',
        direction: pressureBlock.ok && unreadRate >= 40 ? 'up' : 'down',
        state: pressureBlock.ok
          ? (unreadRate >= 40 ? 'critical' : unreadRate >= 20 ? 'warning' : 'stable')
          : 'warning',
        accent: 'alert',
        context: 'alerts pressure window'
      },
      {
        label: 'Care Compliance',
        value: complianceBlock.ok ? `${completionRate.toFixed(0)}%` : '-',
        trend: complianceBlock.ok ? `${totalAppointments} appointments` : 'Backend unavailable',
        direction: complianceBlock.ok && completionRate >= 70 ? 'up' : 'down',
        state: complianceBlock.ok
          ? (completionRate >= 70 ? 'stable' : completionRate >= 50 ? 'warning' : 'critical')
          : 'warning',
        accent: 'positive',
        context: 'appointment window'
      },
      {
        label: 'Avg MMSE Score',
        value: mmseBlock.ok ? (avgMmse == null ? '-' : avgMmse.toFixed(1)) : '-',
        trend: mmseBlock.ok ? `${mmseScores.length} monthly points` : 'Backend unavailable',
        direction: mmseBlock.ok && avgMmse != null && avgMmse >= 24 ? 'up' : 'down',
        state: mmseBlock.ok
          ? (avgMmse == null ? 'warning' : avgMmse >= 24 ? 'stable' : avgMmse >= 18 ? 'warning' : 'critical')
          : 'warning',
        accent: 'warning',
        context: 'mmse severity window'
      }
    ];
  }
}

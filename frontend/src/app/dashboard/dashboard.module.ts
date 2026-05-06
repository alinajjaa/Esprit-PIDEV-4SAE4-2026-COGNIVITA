import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { DashboardRoutingModule } from './dashboard-routing.module';
import { DashboardPageComponent } from './dashboard-page/dashboard-page.component';
import { KpiCardsComponent } from './components/kpi-cards/kpi-cards.component';
import { AlertTrendChartComponent } from './components/alert-trend-chart/alert-trend-chart.component';
import { PatientRankingComponent } from './components/patient-ranking/patient-ranking.component';
import { AppointmentPanelComponent } from './components/appointment-panel/appointment-panel.component';
import { MmsePanelComponent } from './components/mmse-panel/mmse-panel.component';

@NgModule({
  declarations: [
    DashboardPageComponent,
    KpiCardsComponent,
    AlertTrendChartComponent,
    PatientRankingComponent,
    AppointmentPanelComponent,
    MmsePanelComponent
  ],
  imports: [CommonModule, DashboardRoutingModule, BaseChartDirective]
})
export class DashboardModule {}

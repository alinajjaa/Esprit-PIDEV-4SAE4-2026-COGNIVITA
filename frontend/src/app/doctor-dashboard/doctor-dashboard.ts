import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';

import { DoctorStatsDto, DoctorStatsService } from '../services/doctor-stats.service';
import {StatsChartsComponent} from './stats-charts.component';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

function toIsoDate(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function addDays(d: Date, days: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + days);
  return x;
}

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule,StatsChartsComponent],
  templateUrl: './doctor-dashboard.html',
  styleUrls: ['./doctor-dashboard.css'],
})
export class DoctorDashboardComponent implements OnInit {


  async  downloadPdf() {
    const el = document.getElementById('statsExport');
    if (!el || !this.stats) return;
    await new Promise((r) => setTimeout(r, 250)); // let charts finish

    const canvas = await html2canvas(el, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      windowWidth: el.scrollWidth,
    });

    const imgData = canvas.toDataURL('image/png');

    const pdf = new jsPDF('p', 'mm', 'a4');
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();

    // Layout constants
    const marginX = 12;
    const headerH = 22;
    const footerH = 12;
    const contentY = headerH + 6;
    const contentH = pageHeight - contentY - footerH;

    const usableW = pageWidth - marginX * 2;

    // image sizing
    const imgW = usableW;
    const imgH = (canvas.height * imgW) / canvas.width;

    // header strings
    const title = 'Alzheimer Clinique — Doctor Statistics Report';
    const range = `Range: ${this.from} → ${this.to}`;
    const generated = `${new Date().toLocaleString()}`;

    // how many pages needed (based on the image height vs content area)
    const totalPages = Math.max(1, Math.ceil(imgH / contentH));

    for (let page = 1; page <= totalPages; page++) {
      if (page > 1) pdf.addPage();

      // ---- Header (clean, professional) ----
      pdf.setFont('helvetica', 'bold');
      pdf.setFontSize(14);
      pdf.text(title, marginX, 12);

      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(10);
      pdf.text(range, marginX, 17);
      pdf.setTextColor(100);
      pdf.text(generated, marginX, 21);
      pdf.setTextColor(0);

      // line under header
      pdf.setDrawColor(230);
      pdf.line(marginX, headerH + 2, pageWidth - marginX, headerH + 2);

      // ---- Content image slice ----
      // We draw the same full image but shift it up so each page shows a different "slice"
      const offsetY = -(page - 1) * contentH;

      pdf.addImage(
        imgData,
        'PNG',
        marginX,
        contentY + offsetY,
        imgW,
        imgH,
        undefined,
        'FAST'
      );

      // ---- Footer ----
      pdf.setDrawColor(230);
      pdf.line(marginX, pageHeight - footerH, pageWidth - marginX, pageHeight - footerH);

      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(10);
      pdf.setTextColor(100);
      pdf.text('Alzheimer Clinique', marginX, pageHeight - 4);
      pdf.text(`Page ${page} / ${totalPages}`, pageWidth - marginX, pageHeight - 4, { align: 'right' });
      pdf.setTextColor(0);
    }

    const filename = `alzheimer_clinique_doctor_${this.medecinId}_${this.from}_to_${this.to}.pdf`;
    pdf.save(filename);
  }

  private medecinId = 1;

  from = toIsoDate(addDays(new Date(), -30));
  to = toIsoDate(new Date());

  loading = false;
  error: string | null = null;
  stats: DoctorStatsDto | null = null;


  constructor(private api: DoctorStatsService) {}

  ngOnInit(): void {
    this.load(); // auto load
  }

  get statusEntries(): Array<[string, number]> {
    if (!this.stats?.byStatus) return [];
    return Object.entries(this.stats.byStatus).sort((a, b) => (b[1] ?? 0) - (a[1] ?? 0));
  }

  get cancelRatePct(): number {
    if (!this.stats) return 0;
    return Math.round((this.stats.cancellationRate ?? 0) * 1000) / 10;
  }

  quickRange(days: number) {
    this.from = toIsoDate(addDays(new Date(), -days));
    this.to = toIsoDate(new Date());
    this.load();
  }

  onDateChange() {
    // prevent spam if already loading
    if (!this.loading) this.load();
  }

  load() {
    if (this.loading) return;

    this.loading = true;
    this.error = null;

    this.api
      .getStats(this.medecinId, this.from, this.to)
      .pipe(
        timeout(8000), // never hang forever
        finalize(() => (this.loading = false))
      )
      .subscribe({
        next: (res) => {
          this.stats = res;
        },
        error: (err) => {
          this.stats = null;
          this.error =
            err?.name === 'TimeoutError'
              ? 'Timeout: Gateway 8089 not responding'
              : err?.error?.message || err?.message || 'Failed to load doctor stats';

          console.error('Doctor stats error:', err);
        },
      });
  }

}

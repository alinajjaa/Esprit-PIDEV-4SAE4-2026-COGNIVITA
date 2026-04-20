import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { API } from '../core/api';

type RendezVousStatus = 'PLANIFIE' | 'CONFIRME' | 'ANNULE' | 'TERMINE';

type MedecinOption = {
  id: number;
  nomMedecin: string;
};

@Component({
  selector: 'app-rendezvous-list',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './rendezvous-list.html',
  styleUrls: ['./rendezvous-list.css'],
})
export class RendezvousList implements OnInit {

  private rdvApiUrl = API.RDV_API;
  private medecinApiUrl = API.MEDECIN_API;

  patientId: number = 1;

  loading = false;
  errorMsg = '';

  rendezvousList: any[] = [];
  medecinOptions: MedecinOption[] = [];

  searchTerm: string = '';
  filteredRendezvous: any[] = [];

  pageSize: number = 8;
  currentPage: number = 1;
  totalPages: number = 1;
  pagedRendezvous: any[] = [];

  sortAscending: boolean = true;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loading = true;
    this.errorMsg = '';

    this.loadMedecinOptions();
    this.loadRendezVousForPatient();
  }

  loadMedecinOptions(): void {
    this.http.get<MedecinOption[]>(`${this.medecinApiUrl}/options`).subscribe({
      next: (data) => {
        this.medecinOptions = Array.isArray(data) ? [...data] : [];
        this.rebuild();
        this.cdr.detectChanges();
      },
      error: () => {
        this.medecinOptions = [];
        this.cdr.detectChanges();
      }
    });
  }

  loadRendezVousForPatient(): void {
    this.http.get<any[]>(`${this.rdvApiUrl}/patient/${this.patientId}`)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          this.rendezvousList = Array.isArray(data) ? [...data] : [];
          this.rebuild();
          this.cdr.detectChanges();
        },
        error: () => {
          this.rendezvousList = [];
          this.filteredRendezvous = [];
          this.pagedRendezvous = [];
          this.errorMsg = 'Erreur chargement rendez-vous du patient';
          this.cdr.detectChanges();
        }
      });
  }

  rebuild(): void {
    let list = [...this.rendezvousList];

    const term = this.searchTerm.trim().toLowerCase();

    if (term) {
      list = list.filter(r => {
        const date = new Date(r.dateHeure).toLocaleString().toLowerCase();
        const medecinName =
          (r.medecin?.nomMedecin || this.getMedecinName(r.medecin?.id) || '').toLowerCase();
        const status = (r.status || '').toLowerCase();
        const chambre = (r.chambre || '').toLowerCase();
        const infermiere = (r.infermiere || '').toLowerCase();
        const meds = Array.isArray(r.medicaments)
          ? r.medicaments.join(' ').toLowerCase()
          : '';

        return (
          date.includes(term) ||
          medecinName.includes(term) ||
          status.includes(term) ||
          chambre.includes(term) ||
          infermiere.includes(term) ||
          meds.includes(term)
        );
      });
    }

    list.sort((a, b) => {
      const dateA = new Date(a.dateHeure).getTime();
      const dateB = new Date(b.dateHeure).getTime();
      return this.sortAscending ? dateA - dateB : dateB - dateA;
    });

    this.filteredRendezvous = list;
    this.currentPage = 1;
    this.updatePagination();
  }

  sortByDate(): void {
    this.sortAscending = !this.sortAscending;
    this.rebuild();
    this.cdr.detectChanges();
  }

  private updatePagination(): void {
    this.totalPages = Math.max(
      1,
      Math.ceil(this.filteredRendezvous.length / this.pageSize)
    );

    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    if (this.currentPage < 1) this.currentPage = 1;

    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;

    this.pagedRendezvous = this.filteredRendezvous.slice(start, end);
  }

  onPageSizeChange(): void {
    this.currentPage = 1;
    this.updatePagination();
    this.cdr.detectChanges();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
      this.cdr.detectChanges();
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
      this.cdr.detectChanges();
    }
  }

  annulerRendezVous(id: number): void {
    const ok = confirm('Voulez-vous annuler ce rendez-vous ?');
    if (!ok) return;

    const payload = { status: 'ANNULE' as RendezVousStatus };

    this.http.put(`${this.rdvApiUrl}/${id}`, payload).subscribe({
      next: () => this.loadRendezVousForPatient(),
      error: () => {
        this.errorMsg = 'Erreur lors de l’annulation.';
        this.cdr.detectChanges();
      }
    });
  }

  getMedecinName(medecinId?: number): string {
    if (!medecinId) return '';
    const found = this.medecinOptions.find(m => m.id === medecinId);
    return found ? found.nomMedecin : '';
  }
}
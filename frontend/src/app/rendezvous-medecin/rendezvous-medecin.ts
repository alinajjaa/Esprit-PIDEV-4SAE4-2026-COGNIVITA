import { API } from '../core/api';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastService } from '../services/toast.service';

type CareForm = {
  chambre: string | null;
  infermiere: string | null;
  medicaments: string[];
};

@Component({
  selector: 'app-rendezvous-medecin',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './rendezvous-medecin.html',
  styleUrls: ['./rendezvous-medecin.css']
})
export class RendezvousMedecinComponent implements OnInit {

  constructor(
    private http: HttpClient,
    public toastService: ToastService,
    private router: Router
  ) {}

  private apiUrl = API.RDV_API;
  private suiviUrl = API.SUIVI_API;
  private optionsUrl = API.OPTIONS_API;

  medecinId: number = 1;

  allRendezvous: any[] = [];
  rendezvousList: any[] = [];
  pagedRendezvous: any[] = [];
  baseRendezvous: any[] = [];

  activeStatus: string = 'ALL';
  onlyToday: boolean = false;

  successMsg = '';
  errorMsg = '';

  sortAscending: boolean = true;
  searchTerm: string = '';

  pageSize: number = 8;
  currentPage: number = 1;
  totalPages: number = 1;

  // ✅ Care modal state
  careModalOpen = false;
  selectedRdv: any | null = null;
  savingCare = false;

  rooms: string[] = [];
  nurses: string[] = [];
  medications: string[] = [];

  careForm: CareForm = {
    chambre: null,
    infermiere: null,
    medicaments: []
  };

  ngOnInit(): void {
    this.loadRendezVousForMedecin();
    this.loadOptions(); // preload lists
  }

  goToSuivi(patientId: number | undefined) {
    if (!patientId) return;
    this.router.navigate(['/suivi/patient', patientId]);
  }

  // =========================
  // Load RDVs (DTO endpoint)
  // =========================
  loadRendezVousForMedecin(): void {
    this.successMsg = '';
    this.errorMsg = '';

    this.http.get<any[]>(`${this.apiUrl}/medecin/${this.medecinId}/table`).subscribe({
      next: (data) => {
        this.allRendezvous = [...data];

        this.sortAscending = true;
        this.activeStatus = 'ALL';
        this.onlyToday = false;
        this.baseRendezvous = [...this.allRendezvous];

        this.applySearchSortAndPaginate();
      },
      error: () => {
        this.errorMsg = 'Erreur chargement rendez-vous';
      }
    });
  }

  // =========================
  // Load enum options
  // =========================
  loadOptions(): void {
    this.http.get<string[]>(`${this.optionsUrl}/rooms`).subscribe({
      next: (d) => this.rooms = d || [],
      error: () => this.rooms = []
    });

    this.http.get<string[]>(`${this.optionsUrl}/nurses`).subscribe({
      next: (d) => this.nurses = d || [],
      error: () => this.nurses = []
    });

    this.http.get<string[]>(`${this.optionsUrl}/medications`).subscribe({
      next: (d) => this.medications = d || [],
      error: () => this.medications = []
    });
  }

  // =========================
  // Filters
  // =========================
  filterStatus(status: string): void {
    this.activeStatus = status;
    this.onlyToday = false;
    this.rebuildBaseAndApply();
  }

  showToday(): void {
    this.onlyToday = true;
    this.activeStatus = 'ALL';
    this.rebuildBaseAndApply();
  }

  private rebuildBaseAndApply(): void {
    let base = [...this.allRendezvous];

    if (this.activeStatus !== 'ALL') {
      base = base.filter(r => r.status === this.activeStatus);
    }

    if (this.onlyToday) {
      const today = new Date().toDateString();
      base = base.filter(r => new Date(r.dateHeure).toDateString() === today);
    }

    this.baseRendezvous = base;
    this.applySearchSortAndPaginate();
  }

  // =========================
  // Search + Sort + Pagination
  // =========================
  applySearchSortAndPaginate(): void {
    let list = [...this.baseRendezvous];

    const term = this.searchTerm.trim().toLowerCase();
    if (term) {
      list = list.filter(r => {
        const date = new Date(r.dateHeure).toLocaleString().toLowerCase();
        const patient = (r.patientNom || '').toLowerCase();
        const status = (r.status || '').toLowerCase();
        const chambre = (r.chambre || '').toLowerCase();
        const infermiere = (r.infermiere || '').toLowerCase();
        const meds = Array.isArray(r.medicaments) ? r.medicaments.join(' ').toLowerCase() : '';

        return (
          date.includes(term) ||
          patient.includes(term) ||
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
      return this.sortAscending ? (dateA - dateB) : (dateB - dateA);
    });

    this.rendezvousList = list;

    this.currentPage = 1;
    this.updatePagination();
  }

  sortByDate(): void {
    this.sortAscending = !this.sortAscending;
    this.applySearchSortAndPaginate();
  }

  private updatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.rendezvousList.length / this.pageSize));

    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    if (this.currentPage < 1) this.currentPage = 1;

    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;

    this.pagedRendezvous = this.rendezvousList.slice(start, end);
  }

  onPageSizeChange(): void {
    this.currentPage = 1;
    this.updatePagination();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
    }
  }

  isSoon(dateHeure: string): boolean {
    const now = new Date().getTime();
    const rdvTime = new Date(dateHeure).getTime();
    const diff = rdvTime - now;
    return diff > 0 && diff < 3600000;
  }

  // =========================
  // Confirm RDV
  // =========================
  confirmerRendezVous(rdv: any): void {
    this.successMsg = '';
    this.errorMsg = '';

    const patientId: number | undefined = rdv?.patientId;
    const patientName: string = rdv?.patientNom || '';
    const dateTxt: string = new Date(rdv?.dateHeure).toLocaleString();

    const ok = confirm(`Confirmer le rendez-vous ?\n\nPatient: ${patientName}\nDate: ${dateTxt}`);
    if (!ok) return;

    this.http.put(`${this.apiUrl}/${rdv.id}/confirm`, {}).subscribe({
      next: () => {
        this.http.post(`${this.suiviUrl}/plans/from-rdv/${rdv.id}`, {}).subscribe({
          next: () => {
            this.toastService.success('Confirmé', 'Rendez-vous confirmé. Plan de suivi créé.');
            if (patientId) {
              this.router.navigate(['/suivi/patient', patientId]);
            } else {
              this.loadRendezVousForMedecin();
            }
          },
          error: () => {
            this.toastService.success('Confirmé', 'Rendez-vous confirmé. Redirection vers le plan de suivi...');
            if (patientId) {
              this.router.navigate(['/suivi/patient', patientId]);
            } else {
              this.loadRendezVousForMedecin();
            }
          }
        });
      },
      error: () => {
        this.errorMsg = 'Erreur confirmation';
        this.toastService.error('Erreur', 'Erreur lors de la confirmation.');
      }
    });
  }

  // =========================
  // Finish RDV
  // =========================
  terminerRendezVous(rdv: any): void {
    this.successMsg = '';
    this.errorMsg = '';

    this.http.patch(`${this.apiUrl}/${rdv.id}/terminate`, {}).subscribe({
      next: () => {
        this.toastService.success('Terminé', 'Rendez-vous terminé.');
        this.loadRendezVousForMedecin();
      },
      error: () => {
        this.errorMsg = 'Erreur terminaison';
        this.toastService.error('Erreur', 'Erreur lors de la terminaison.');
      }
    });
  }

  // =========================
  // ✅ Care Modal
  // =========================
  openCareModal(rdv: any): void {
    this.selectedRdv = rdv;

    this.careForm = {
      chambre: rdv?.chambre ?? null,
      infermiere: rdv?.infermiere ?? null,
      medicaments: Array.isArray(rdv?.medicaments) ? [...rdv.medicaments] : []
    };

    this.careModalOpen = true;
  }

  closeCareModal(): void {
    this.careModalOpen = false;
    this.selectedRdv = null;
    this.savingCare = false;
  }

  isMedicationSelected(m: string): boolean {
    return this.careForm.medicaments.includes(m);
  }

  toggleMedication(m: string): void {
    const idx = this.careForm.medicaments.indexOf(m);
    if (idx >= 0) this.careForm.medicaments.splice(idx, 1);
    else this.careForm.medicaments.push(m);
  }

  saveCare(): void {
    if (!this.selectedRdv?.id) return;

    this.savingCare = true;

    const payload = {
      chambre: this.careForm.chambre,
      infermiere: this.careForm.infermiere,
      medicaments: this.careForm.medicaments
    };

    this.http.patch(`${this.apiUrl}/${this.selectedRdv.id}/care`, payload).subscribe({
      next: () => {
        this.toastService.success('Enregistré', 'Soins affectés au rendez-vous.');
        this.closeCareModal();
        this.loadRendezVousForMedecin();
      },
      error: () => {
        this.savingCare = false;
        this.toastService.error('Erreur', 'Impossible d’affecter les soins.');
      }
    });
  }
}

import { API } from '../core/api';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';

type Patient = {
  id: number;
  nomPatient?: string;
  telephone?: string;
};

@Component({
  selector: 'app-suivi-patient',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './suivi-patient.html',
  styleUrls: ['./suivi-patient.css']
})
export class SuiviPatientComponent implements OnInit {

  private suiviUrl = API.SUIVI_API;
  private patientUrl = API.PATIENTS_API;

  patientId!: number;

  // ✅ patient details
  patient: Patient | null = null;
  patientLoading = false;
  patientError = '';

  plans: any[] = [];

  loading = false;
  errorMsg = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit(): void {
    this.patientId = Number(this.route.snapshot.paramMap.get('patientId'));

    // load both
    this.loadPatient();
    this.loadPlans();
  }

  loadPatient(): void {
    this.patientLoading = true;
    this.patientError = '';

    this.http.get<Patient>(`${this.patientUrl}/${this.patientId}`).subscribe({
      next: (data) => {
        this.patient = data;
        this.patientLoading = false;
      },
      error: () => {
        this.patientError = 'Erreur chargement patient';
        this.patientLoading = false;
      }
    });
  }

  loadPlans(): void {
    this.loading = true;
    this.errorMsg = '';

    this.http.get<any[]>(`${this.suiviUrl}/plans/patient/${this.patientId}`).subscribe({
      next: (data) => {
        this.plans = data || [];
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'Erreur chargement plans de suivi';
        this.loading = false;
      }
    });
  }

  markStepDone(stepId: number): void {
    this.http.patch(`${this.suiviUrl}/steps/${stepId}/done`, {}).subscribe({
      next: () => this.loadPlans(),
      error: () => console.error('Erreur lors de la mise à jour')
    });
  }
}

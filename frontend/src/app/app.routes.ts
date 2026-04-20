import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { MMSETestComponent } from './mmse/mmse-test.component';
import { CNNPredictionComponent } from './cnn/cnn-prediction.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';
import { MedicalRecordsComponent } from './medical-records/medical-records.component';
import { HealthPreventionComponent } from './health-prevention/health-prevention.component';
import { FamilyTreeComponent } from './family-tree/family-tree.component';
import { MedicationAdherenceComponent } from './medication-adherence/medication-adherence.component';
import { NotificationsComponent } from './notifications/notifications.component';
// Work2 routes
import { Rendezvous } from './rendezvous/rendezvous';
import { RendezvousMedecinComponent } from './rendezvous-medecin/rendezvous-medecin';
import { AdminRendezvousComponent } from './rendezvous-admin/rendezvous-admin';
import { SuiviPatientComponent } from './suivi-patient/suivi-patient';
import { RendezvousList } from './rendezvous-list/rendezvous-list';
import { DoctorDashboardComponent } from './doctor-dashboard/doctor-dashboard';
// Cognitive Activities & Journal routes
import { CognitiveActivities } from './cognitive-activities/cognitive-activities';
import { ActivityList } from './cognitive-activities/activity-list/activity-list';
import { ActivityForm } from './cognitive-activities/activity-form/activity-form';
import { ActivityDetail } from './cognitive-activities/activity-detail/activity-detail';
import { ActivityPlay } from './cognitive-activities/activity-play/activity-play';
import { JournalComponent } from './cognitive-activities/journal/journal.component';
import { JournalHistoryComponent } from './cognitive-activities/journal/journal-history/journal-history.component';
import { JournalEntryFormComponent } from './cognitive-activities/journal/journal-entry-form/journal-entry-form.component';

export const routes: Routes = [
  // Shared routes
  { path: '', component: HomeComponent },
  { path: 'mmse', component: MMSETestComponent },
  { path: 'cnn', component: CNNPredictionComponent },
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'medical-records', component: MedicalRecordsComponent },
  // Work1 routes
  { path: 'health-prevention', component: HealthPreventionComponent },
  { path: 'family-tree', component: FamilyTreeComponent },
  { path: 'medication-adherence', component: MedicationAdherenceComponent },
  { path: 'notifications', component: NotificationsComponent },
  // Work2 routes
  { path: 'rendezvous', component: Rendezvous },
  { path: 'rendezvousList', component: RendezvousList },
  { path: 'rendezvousMedecin', component: RendezvousMedecinComponent },
  { path: 'rendezvousAdmin', component: AdminRendezvousComponent },
  { path: 'suivi/patient/:patientId', component: SuiviPatientComponent },
  { path: 'medecin-dashboard', component: DoctorDashboardComponent },
  // Cognitive Activities routes
  {
    path: 'activities',
    component: CognitiveActivities,
    children: [
      { path: '', component: ActivityList },
      { path: 'new', component: ActivityForm },
      { path: 'edit/:id', component: ActivityForm },
      { path: ':id', component: ActivityDetail },
      { path: ':id/play', component: ActivityPlay }
    ]
  },
  // Journal routes
  {
    path: 'journal',
    component: JournalComponent,
    children: [
      { path: '', component: JournalHistoryComponent },
      { path: 'new', component: JournalEntryFormComponent },
      { path: 'edit/:id', component: JournalEntryFormComponent }
    ]
  },
];

import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { MMSETestComponent } from './mmse/mmse-test.component';
import { CNNPredictionComponent } from './cnn/cnn-prediction.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';
import { MedicalRecordsComponent } from './medical-records/medical-records.component';

// Imports pour Activities
import { CognitiveActivities } from './cognitive-activities/cognitive-activities';
import { ActivityList } from './cognitive-activities/activity-list/activity-list';
import { ActivityForm } from './cognitive-activities/activity-form/activity-form';
import { ActivityDetail } from './cognitive-activities/activity-detail/activity-detail';
import { ActivityPlay } from './cognitive-activities/activity-play/activity-play';

// ✅ AJOUTE LES IMPORTS POUR JOURNAL
import { JournalComponent } from './cognitive-activities/journal/journal.component';
import { JournalHistoryComponent } from './cognitive-activities/journal/journal-history/journal-history.component';
import { JournalEntryFormComponent } from './cognitive-activities/journal/journal-entry-form/journal-entry-form.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'mmse',
    component: MMSETestComponent
  },
  {
    path: 'cnn',
    component: CNNPredictionComponent
  },
  {
    path: 'admin',
    component: AdminDashboardComponent
  },
  {
    path: 'medical-records',
    component: MedicalRecordsComponent
  },
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
  // ✅ AJOUTE LES ROUTES POUR JOURNAL
  {
    path: 'journal',
    component: JournalComponent,
    children: [
      { path: '', component: JournalHistoryComponent },        // /journal
      { path: 'new', component: JournalEntryFormComponent },   // /journal/new
      { path: 'edit/:id', component: JournalEntryFormComponent } // /journal/edit/1
    ]
  }
];

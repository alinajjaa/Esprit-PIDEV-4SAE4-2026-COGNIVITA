import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { MMSETestComponent } from './mmse/mmse-test.component';
import { CNNPredictionComponent } from './cnn/cnn-prediction.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';
import { MedicalRecordsComponent } from './medical-records/medical-records.component';
import { AdminMmseTestsComponent } from './admin/mmse-tests/admin-mmse-tests.component';

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
    path: 'admin/mmse-tests',
    component: AdminMmseTestsComponent
  },
  {
    path: 'medical-records',
    component: MedicalRecordsComponent
  }
];

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TrackingLiveComponent } from './tracking-live.component';

const routes: Routes = [
  {
    path: '',
    component: TrackingLiveComponent
  }
];

@NgModule({
  imports: [TrackingLiveComponent, RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TrackingModule {}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ActivityDetail } from './activity-detail';
import { provideActivatedRouteStub } from '../../../testing/route-stubs';

describe('ActivityDetail', () => {
  let component: ActivityDetail;
  let fixture: ComponentFixture<ActivityDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivityDetail, HttpClientTestingModule],
      providers: [provideRouter([]), provideActivatedRouteStub({ id: '1' })],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivityDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ActivityPlay } from './activity-play';
import { provideActivatedRouteStub } from '../../../testing/route-stubs';

describe('ActivityPlay', () => {
  let component: ActivityPlay;
  let fixture: ComponentFixture<ActivityPlay>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivityPlay, HttpClientTestingModule],
      providers: [provideRouter([]), provideActivatedRouteStub({ id: '1' })],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivityPlay);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

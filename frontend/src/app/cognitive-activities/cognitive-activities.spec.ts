import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { CognitiveActivities } from './cognitive-activities';

describe('CognitiveActivities', () => {
  let component: CognitiveActivities;
  let fixture: ComponentFixture<CognitiveActivities>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CognitiveActivities, HttpClientTestingModule],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CognitiveActivities);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


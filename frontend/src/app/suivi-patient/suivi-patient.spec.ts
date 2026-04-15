import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { SuiviPatientComponent } from './suivi-patient';

describe('SuiviPatientComponent', () => {
  let component: SuiviPatientComponent;
  let fixture: ComponentFixture<SuiviPatientComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuiviPatientComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SuiviPatientComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

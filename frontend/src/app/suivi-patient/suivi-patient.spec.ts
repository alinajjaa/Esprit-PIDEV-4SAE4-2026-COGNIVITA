import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuiviPatient } from './suivi-patient';

describe('SuiviPatient', () => {
  let component: SuiviPatient;
  let fixture: ComponentFixture<SuiviPatient>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuiviPatient]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SuiviPatient);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

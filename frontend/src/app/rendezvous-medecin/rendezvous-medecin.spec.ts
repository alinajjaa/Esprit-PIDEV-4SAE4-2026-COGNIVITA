import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RendezvousMedecin } from './rendezvous-medecin';

describe('RendezvousMedecin', () => {
  let component: RendezvousMedecin;
  let fixture: ComponentFixture<RendezvousMedecin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RendezvousMedecin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RendezvousMedecin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

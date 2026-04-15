import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RendezvousMedecinComponent } from './rendezvous-medecin';

describe('RendezvousMedecinComponent', () => {
  let component: RendezvousMedecinComponent;
  let fixture: ComponentFixture<RendezvousMedecinComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RendezvousMedecinComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RendezvousMedecinComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

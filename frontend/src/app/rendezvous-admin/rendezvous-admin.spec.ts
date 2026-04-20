import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AdminRendezvousComponent } from './rendezvous-admin';

describe('AdminRendezvousComponent', () => {
  let component: AdminRendezvousComponent;
  let fixture: ComponentFixture<AdminRendezvousComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminRendezvousComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminRendezvousComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

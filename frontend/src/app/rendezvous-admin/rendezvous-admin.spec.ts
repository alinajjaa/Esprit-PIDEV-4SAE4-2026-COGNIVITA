import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RendezvousAdmin } from './rendezvous-admin';

describe('RendezvousAdmin', () => {
  let component: RendezvousAdmin;
  let fixture: ComponentFixture<RendezvousAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RendezvousAdmin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RendezvousAdmin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

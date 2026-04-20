import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RendezvousList } from './rendezvous-list';

describe('RendezvousList', () => {
  let component: RendezvousList;
  let fixture: ComponentFixture<RendezvousList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RendezvousList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RendezvousList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Rendezvous } from './rendezvous';

describe('Rendezvous', () => {
  let component: Rendezvous;
  let fixture: ComponentFixture<Rendezvous>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Rendezvous],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Rendezvous);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

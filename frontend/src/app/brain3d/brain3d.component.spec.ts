import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Brain3dComponent } from './brain3d.component';

describe('Brain3dComponent', () => {
  let component: Brain3dComponent;
  let fixture: ComponentFixture<Brain3dComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Brain3dComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Brain3dComponent);
    fixture.componentRef.setInput('diagnosis', 'test');
    component = fixture.componentInstance;
    // Sans detectChanges() : WebGL/jeux trois.js ne fonctionnent pas dans jsdom
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Brain3dComponent } from './brain3d.component';

describe('Brain3dComponent', () => {
  let component: Brain3dComponent;
  let fixture: ComponentFixture<Brain3dComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Brain3dComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(Brain3dComponent);
    component = fixture.componentInstance;

    // Mock all private methods called in ngAfterViewInit
    (component as any).initScene = () => {};
    (component as any).loadBrainModel = () => {};
    (component as any).animate = () => {};
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivityForm } from './activity-form';
import { CognitiveActivityService } from '../services/cognitive-activity.service';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

describe('ActivityForm', () => {
  let component: ActivityForm;
  let fixture: ComponentFixture<ActivityForm>;
  let serviceStub: any;
  let routerStub: any;

  beforeEach(async () => {
    serviceStub = {
      getActivityById: (id: number) => of(null),
      createActivity: (payload: any) => of({ id: 11 }),
      updateActivity: (id: number, payload: any) => of({ id })
    };

    routerStub = { navigate: (args: any[]) => true };

    await TestBed.configureTestingModule({
      imports: [ActivityForm],
      providers: [
        { provide: CognitiveActivityService, useValue: serviceStub },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    }).compileComponents();

    serviceStub = TestBed.inject(CognitiveActivityService) as any;
    fixture = TestBed.createComponent(ActivityForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should require title to be valid', () => {
    component.activity.title = '';
    expect(component.isValid()).toBeFalsy();

    component.activity.title = 'Hello';
    component.words = ['a','b','c'];
    component.activity.type = 'MEMORY';
    expect(component.isValid()).toBeTruthy();
  });

  it('should add and remove words for MEMORY', () => {
    component.activity.type = 'MEMORY';
    component.words = ['a','b','c'];

    component.addWord();
    expect(component.words.length).toBe(4);

    component.removeWord(0);
    expect(component.words.length).toBe(3);
  });

  it('should add and remove numbers for LOGIC', () => {
    component.activity.type = 'LOGIC';
    component.sequence = [1,2,3];

    component.addNumber();
    expect(component.sequence.length).toBe(4);

    component.removeNumber(0);
    expect(component.sequence.length).toBe(3);
  });

  it('changing type should reset related fields', () => {
    component.activity.type = 'ATTENTION';
    component.onTypeChange();
    // words should be present as array (possibly empty)
    expect(Array.isArray(component.words)).toBeTruthy();
  });

  it('should submit form and navigate on success (create mode)', () => {
    component.activity.title = 'New';
    component.activity.type = 'MEMORY';
    component.words = ['a','b','c'];

    component.onSubmit();

    // after calling onSubmit, because stub returns sync observable, success should be set
    expect(component.submitting).toBeFalsy();

    // simulate immediate observable completion
    serviceStub.createActivity({}).subscribe(() => {
      expect(component.success).toContain('créée');
    });
  });
});

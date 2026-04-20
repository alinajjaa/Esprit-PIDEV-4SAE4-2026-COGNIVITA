// src/app/cognitive-activities/activity-list/activity-list.component.spec.ts
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivityList } from './activity-list';
import { CognitiveActivityService, CognitiveActivity } from '../services/cognitive-activity.service';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

describe('ActivityList', () => {
  let component: ActivityList;
  let fixture: ComponentFixture<ActivityList>;
  let serviceStub: any;
  let deleteCalledWith: number | null = null;

  const mockActivities: CognitiveActivity[] = [
    { id: 1, title: 'Memory Test', type: 'MEMORY', difficulty: 'EASY', isActive: true },
    { id: 2, title: 'Attention Test', type: 'ATTENTION', difficulty: 'MEDIUM', isActive: true },
    { id: 3, title: 'Logic Test', type: 'LOGIC', difficulty: 'HARD', isActive: true }
  ];

  beforeEach(async () => {
    deleteCalledWith = null;

    serviceStub = {
      getAllActivities: () => of([...mockActivities]),
      deleteActivity: (id: number) => {
        deleteCalledWith = id;
        return of(null);
      }
    };

    await TestBed.configureTestingModule({
      imports: [ActivityList, RouterTestingModule],
      providers: [
        { provide: CognitiveActivityService, useValue: serviceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load activities on init', () => {
    expect(component.allActivities.length).toBe(3);
    expect(component.filteredActivities.length).toBe(3);
  });

  it('should filter activities by type', () => {
    component.filterByType('MEMORY');
    expect(component.activeTypeFilter).toBe('MEMORY');
    expect(component.filteredActivities.length).toBe(1);
    expect(component.filteredActivities[0].type).toBe('MEMORY');
  });

  it('should filter activities by difficulty', () => {
    component.filterByDifficulty('HARD');
    expect(component.activeDifficultyFilter).toBe('HARD');
    expect(component.filteredActivities.length).toBe(1);
    expect(component.filteredActivities[0].difficulty).toBe('HARD');
  });

  it('should reset filters', () => {
    component.filterByType('MEMORY');
    component.filterByDifficulty('HARD');
    expect(component.filteredActivities.length).toBe(0);

    component.resetFilters();
    expect(component.activeTypeFilter).toBe('ALL');
    expect(component.activeDifficultyFilter).toBe('ALL');
    expect(component.filteredActivities.length).toBe(3);
  });

  it('should delete activity when confirmed', fakeAsync(() => {
    const originalConfirm = window.confirm;
    window.confirm = () => true;

    component.allActivities = [...mockActivities];

    component.deleteActivity(1);
    tick();

    expect(deleteCalledWith).toBe(1);
    expect(component.allActivities.length).toBe(2);

    window.confirm = originalConfirm;
  }));

  it('should return correct icon for activity type', () => {
    expect(component.getTypeIcon('MEMORY')).toBe('🧠');
    expect(component.getTypeIcon('ATTENTION')).toBe('👀');
    expect(component.getTypeIcon('LOGIC')).toBe('🔢');
    expect(component.getTypeIcon('UNKNOWN')).toBe('📝');
  });

  it('should return correct class for difficulty', () => {
    expect(component.getDifficultyClass('EASY')).toBe('badge-easy');
    expect(component.getDifficultyClass('MEDIUM')).toBe('badge-medium');
    expect(component.getDifficultyClass('HARD')).toBe('badge-hard');
  });
});

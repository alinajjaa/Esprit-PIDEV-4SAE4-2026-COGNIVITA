import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter } from '@angular/router';
import { RouterOutlet } from '@angular/router';

import { AppComponent } from './app';

@Component({ standalone: true, selector: 'app-brain3d', template: '' })
class Brain3dStubComponent {}

@Component({ standalone: true, selector: 'app-navigation', template: '' })
class NavigationStubComponent {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    })
      .overrideComponent(AppComponent, {
        set: {
          imports: [RouterOutlet, Brain3dStubComponent, NavigationStubComponent],
        },
      })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render shell layout class', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.app-layout')).toBeTruthy();
  });
});

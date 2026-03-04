import { Injectable } from '@angular/core';
import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class EmailExistsValidator {

  constructor(private userService: UserService) {}

  validate(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {

      // Ne pas vérifier si le champ est vide ou trop court
      if (!control.value || control.value.length < 3) {
        return of(null);
      }

      return of(control.value).pipe(
        debounceTime(600),          // attendre 600ms après frappe
        distinctUntilChanged(),     // ne pas re-vérifier si même valeur
        switchMap(email =>
          this.userService.checkEmailExists(email).pipe(
            map(exists => exists ? { emailExists: true } : null),
            catchError(() => of(null)) // en cas d'erreur réseau → pas de blocage
          )
        )
      );
    };
  }
}
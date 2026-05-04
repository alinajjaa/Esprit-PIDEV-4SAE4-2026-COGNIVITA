import { ActivatedRoute } from '@angular/router';
import { convertToParamMap, ParamMap } from '@angular/router';
import { Provider } from '@angular/core';
import { of } from 'rxjs';

/**
 * Injects ActivatedRoute.snapshot.paramMap + paramMap observable for unit tests.
 * Default `{ id: '1' }` matches typical `/activities/:id` routes.
 */
export function provideActivatedRouteStub(params: Record<string, string> = { id: '1' }): Provider {
  const pm: ParamMap = convertToParamMap(params);
  return {
    provide: ActivatedRoute,
    useValue: {
      snapshot: { paramMap: pm },
      paramMap: of(pm),
    },
  };
}

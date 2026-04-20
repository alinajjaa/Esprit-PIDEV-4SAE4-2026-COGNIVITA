/**
 * Central API configuration — single source of truth for all microservice URLs.
 *
 * All services go through the single API Gateway at localhost:9090.
 * Your services use the Angular proxy (/api → 9090).
 * RDV/Suivi services hit the gateway directly at localhost:9090.
 */
export const API = {
  // ── Gateway (proxied via proxy.conf.json → 9090) ──────────────────────────
  GATEWAY: '',           // empty = proxy-relative, e.g. /api/users/...

  // ── RDV Microservice ──────────────────────────────────────────────────────
  RDV_BASE:    'http://localhost:9090/rdv',
  RDV_API:     'http://localhost:9090/rdv/api/rendezvous',
  MEDECIN_API: 'http://localhost:9090/rdv/api/medecins',
  OPTIONS_API: 'http://localhost:9090/rdv/api/options',
  PATIENTS_API:'http://localhost:9090/rdv/api/patients',

  // ── Suivi / Plan de suivi Microservice ────────────────────────────────────
  SUIVI_API:   'http://localhost:9090/plan-suivi/api/suivi',

  // ── Doctor Stats ──────────────────────────────────────────────────────────
  DOCTORS_API: 'http://localhost:9090/rdv/api/doctors',

  // ── Cognitive Activities & Journal (via Gateway) ──────────────────────────
  ACTIVITIES_API: 'http://localhost:9090/api/activities',
  JOURNAL_API:    'http://localhost:9090/api/journal',
};

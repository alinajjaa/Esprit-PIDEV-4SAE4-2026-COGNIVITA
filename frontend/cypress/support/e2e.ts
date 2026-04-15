// ============================================================
// SUPPORT GLOBAL — chargé avant chaque spec Cypress
// ============================================================

import './commands';

// ─────────────────────────────────────────────
// Avant chaque test : nettoie la session pour
// garantir un état propre (pas de token résiduel)
// ─────────────────────────────────────────────
beforeEach(() => {
  cy.clearLocalStorage();
  cy.clearCookies();
});

// ─────────────────────────────────────────────
// Capture les erreurs Angular non-catchées pour
// les afficher clairement dans les logs Cypress
// ─────────────────────────────────────────────
Cypress.on('uncaught:exception', (err) => {
  // Ne pas faire échouer le test sur les erreurs
  // Angular zone.js ou de navigation attendues
  if (
    err.message.includes('ExpressionChangedAfterItHasBeenCheckedError') ||
    err.message.includes('NavigationCanceled') ||
    err.message.includes('ResizeObserver loop')
  ) {
    return false;
  }
  return true;
});

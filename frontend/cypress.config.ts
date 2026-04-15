import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    // Angular app URL
    baseUrl: 'http://localhost:4200',

    // Pas de mocks réseau — vrais appels HTTP vers le backend
    // Les tests échouent explicitement si le backend n'est pas disponible
    experimentalRunAllSpecs: false,

    // Timeouts adaptés aux appels API réels
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    pageLoadTimeout: 30000,

    // Garde les cookies et sessions entre les tests d'une même spec
    testIsolation: false,

    viewportWidth: 1280,
    viewportHeight: 800,

    // Dossiers
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    fixturesFolder: 'cypress/fixtures',

    setupNodeEvents(on, config) {
      // Charge les variables d'environnement depuis cypress.env.json
      // (credentials de test, URLs backend)
      return config;
    },
  },

  // Variables d'environnement disponibles dans les tests via Cypress.env()
  env: {
    // ── Gateway — point d'entrée unique (microservices) ──────────────
    gatewayUrl: 'http://localhost:8080',
    apiUsersUrl: 'http://localhost:8080/api/users',
    apiAdminUrl: 'http://localhost:8080/api/admin',
    apiPredictUrl: 'http://localhost:8080/predict',


    // Credentials de test (overridables via cypress.env.json ou CLI)
    // Exemple : npx cypress run --env testUserEmail=user@test.com
    testUserEmail: 'test@cognivita.com',
    testUserPassword: 'Test1234!',
    testAdminEmail: 'admin@cognivita.com',
    testAdminPassword: 'Admin1234!',
  },
});

// ============================================================
// CUSTOM CYPRESS COMMANDS — COGNIVITA
// ============================================================

declare global {
  namespace Cypress {
    interface Chainable {
      loginByApi(email: string, password: string): Chainable<void>;
      loginAdminByApi(): Chainable<void>;
      loginUserByApi(): Chainable<void>;
      logout(): Chainable<void>;
      assertBackendAvailable(): Chainable<void>;
    }
  }
}

// ─────────────────────────────────────────────
// Helper : lit une variable d'env Cypress
// eslint-disable-next-line @typescript-eslint/no-explicit-any
// ─────────────────────────────────────────────
function getEnv(key: string, fallback: string): string {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const value = (Cypress as any).env(key);
  return value !== undefined && value !== null ? String(value) : fallback;
}

// ─────────────────────────────────────────────
// loginByApi : POST réel via le Gateway (:8080)
// Skippe le test si les credentials sont faux (401/403)
// au lieu de faire planter toute la suite.
// ─────────────────────────────────────────────
Cypress.Commands.add('loginByApi', (email: string, password: string) => {
  const apiUrl = getEnv('apiUsersUrl', 'http://localhost:8080/api/users');

  cy.request({
    method: 'POST',
    url: `${apiUrl}/login`,
    body: { email, password },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 401 || response.status === 403) {
      cy.log(
        `⚠️  Credentials invalides pour ${email} (${response.status}). ` +
        'Mettez à jour cypress.env.json avec vos vrais credentials. ' +
        'Les tests nécessitant ce compte seront ignorés.'
      );
      return; // retour sans token — les tests conditionnels vérifieront localStorage
    }

    if (response.status === 503) {
      cy.log(`⚠️  user-service non disponible (503). Vérifiez que le service est démarré.`);
      return; // retour sans token
    }

    if (response.status !== 200) {
      throw new Error(
        `Login API failed (${response.status}): ${JSON.stringify(response.body)}. ` +
        `Gateway: ${apiUrl}`
      );
    }

    const token: string = response.body.token;
    if (!token) {
      throw new Error(`Pas de token JWT dans la réponse : ${JSON.stringify(response.body)}`);
    }

    const user = response.body.user;
    window.localStorage.setItem('jwt_token', token);
    window.localStorage.setItem('currentUser', JSON.stringify({
      id: user?.id ?? 0,
      email: user?.email ?? email,
      fullName: user?.fullName ?? user?.full_name ?? '',
      photoUrl: user?.photoUrl ?? '',
      role: response.body.role ?? user?.role ?? 'USER',
      createdAt: user?.createdAt ?? '',
      blocked: user?.blocked ?? false,
    }));
  });
});

// ─────────────────────────────────────────────
// loginAdminByApi
// ─────────────────────────────────────────────
Cypress.Commands.add('loginAdminByApi', () => {
  const email    = getEnv('testAdminEmail',    'admin@cognivita.com');
  const password = getEnv('testAdminPassword', 'Admin1234!');
  cy.loginByApi(email, password);
});

// ─────────────────────────────────────────────
// loginUserByApi
// ─────────────────────────────────────────────
Cypress.Commands.add('loginUserByApi', () => {
  const email    = getEnv('testUserEmail',    'test@cognivita.com');
  const password = getEnv('testUserPassword', 'Test1234!');
  cy.loginByApi(email, password);
});

// ─────────────────────────────────────────────
// logout
// ─────────────────────────────────────────────
Cypress.Commands.add('logout', () => {
  cy.clearLocalStorage();
  cy.clearCookies();
});

// ─────────────────────────────────────────────
// assertBackendAvailable
// ─────────────────────────────────────────────
Cypress.Commands.add('assertBackendAvailable', () => {
  const gatewayUrl = getEnv('gatewayUrl', 'http://localhost:8080');
  cy.request({
    method: 'GET',
    url: `${gatewayUrl}/api/users`,
    failOnStatusCode: false,
    timeout: 5000,
  }).then((response) => {
    if (response.status === 0 || response.status >= 500) {
      throw new Error(
        `Gateway non disponible sur ${gatewayUrl}. ` +
        'Démarrez config-server → eureka → services → gateway avant les tests.'
      );
    }
  });
});

export {};

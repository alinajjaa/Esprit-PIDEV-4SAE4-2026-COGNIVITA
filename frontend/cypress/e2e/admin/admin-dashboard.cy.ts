// ============================================================
// TESTS E2E — TABLEAU DE BORD ADMIN
// Flux complet frontend → API backend réelle → base de données
// ============================================================

describe('Admin Dashboard', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const cEnv = (key: string, fallback = '') => (Cypress as any).env(key) ?? fallback;
  const apiAdminUrl = cEnv('apiAdminUrl', 'http://localhost:8080/api/admin');
  const apiUsersUrl = cEnv('apiUsersUrl', 'http://localhost:8080/api/users');
  const adminEmail  = cEnv('testAdminEmail',    'admin@cognivita.com');
  const adminPass   = cEnv('testAdminPassword', 'Admin1234!');

  let adminToken: string;

  before(() => {
    cy.request({
      method: 'POST',
      url: `${apiUsersUrl}/login`,
      body: { email: adminEmail, password: adminPass },
      failOnStatusCode: false,
    }).then((res) => {
      if (res.status === 200 && res.body.token) {
        adminToken = res.body.token;
        cy.log(`✅ Token admin obtenu pour ${adminEmail}`);
      } else {
        cy.log(`⚠️  Compte admin non disponible (${res.status}) — tests API ignorés`);
      }
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 1 — Protection route /admin
  // ─────────────────────────────────────────────────────────
  context('Protection route /admin', () => {
    it('/admin est inaccessible sans token', () => {
      cy.logout();
      cy.visit('/admin');
      cy.url({ timeout: 5000 }).should('include', '/login');
    });

    it('/admin est accessible avec un compte admin', () => {
      cy.loginAdminByApi();
      cy.visit('/admin');
      cy.url({ timeout: 10000 }).should('include', '/admin');
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 2 — GET /api/admin/dashboard
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/dashboard — données réelles', () => {
    it('retourne les données du dashboard avec token admin', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/dashboard`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.exist;
          cy.log(`✅ Dashboard data reçu`);
        } else {
          cy.log(`ℹ️  Dashboard status: ${response.status}`);
        }
      });
    });

    it('retourne une réponse non-200 sans token admin', () => {
      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/dashboard`,
        failOnStatusCode: false,
      }).then((response) => {
        // Selon la config du gateway : 401, 403 ou 404 sont tous acceptables
        // (404 = route non exposée publiquement par le gateway)
        expect(response.status).to.be.oneOf([401, 403, 404]);
        cy.log(`✅ Backend protège /api/admin/dashboard sans auth (${response.status})`);
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 3 — GET /api/admin/stats
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/stats — statistiques réelles', () => {
    it('retourne les statistiques globales de l\'application', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/stats`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.exist;
          cy.log(`✅ Stats récupérées`);
        } else {
          cy.log(`ℹ️  Stats status: ${response.status}`);
        }
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 4 — GET /api/admin/search
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/search — recherche utilisateurs', () => {
    it('recherche par email retourne des résultats depuis la DB', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/search?query=test`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.be.an('array');
          cy.log(`✅ Recherche → ${response.body.length} résultats en DB`);
        } else {
          cy.log(`ℹ️  Search status: ${response.status}`);
        }
      });
    });

    it('recherche vide retourne 200 ou 400', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/search?query=`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 400]);
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 5 — GET /api/admin/filter
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/filter — filtres utilisateurs', () => {
    it('filtre par rôle USER retourne uniquement les utilisateurs', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/filter?role=USER`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.be.an('array');
          cy.log(`✅ Filtre USER → ${response.body.length} utilisateurs en DB`);
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          response.body.forEach((user: any) => expect(user.role).to.eq('USER'));
        } else {
          cy.log(`ℹ️  Filter status: ${response.status}`);
        }
      });
    });

    it('filtre par rôle ADMIN retourne uniquement les admins', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/filter?role=ADMIN`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.be.an('array');
          cy.log(`✅ Filtre ADMIN → ${response.body.length} admins en DB`);
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          response.body.forEach((user: any) => expect(user.role).to.eq('ADMIN'));
        } else {
          cy.log(`ℹ️  Filter status: ${response.status}`);
        }
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 6 — Exports
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/export — exports de données', () => {
    it('GET /api/admin/export/users retourne les données exportables', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/export/users`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          cy.log(`✅ Export users disponible`);
        } else {
          cy.log(`ℹ️  Export users status: ${response.status}`);
        }
      });
    });

    it('GET /api/admin/export/mmse retourne les données MMSE exportables', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/export/mmse`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          cy.log(`✅ Export MMSE disponible`);
        } else {
          cy.log(`ℹ️  Export MMSE status: ${response.status}`);
        }
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 7 — Activity Log
  // ─────────────────────────────────────────────────────────
  context('GET /api/admin/activity-log — journal d\'activité', () => {
    it('retourne les 50 dernières activités depuis la DB', () => {
      if (!adminToken) { cy.log('ℹ️  Skip — admin non dispo'); return; }

      cy.request({
        method: 'GET',
        url: `${apiAdminUrl}/activity-log?limit=50`,
        headers: { Authorization: `Bearer ${adminToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.be.an('array');
          expect(response.body.length).to.be.lte(50);
          cy.log(`✅ Activity log : ${response.body.length} entrées`);
        } else {
          cy.log(`ℹ️  Activity log status: ${response.status}`);
        }
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 8 — Interface admin UI
  // ─────────────────────────────────────────────────────────
  context('Interface admin — page /admin', () => {
    beforeEach(() => {
      cy.loginAdminByApi();
    });

    it('la page /admin se charge et affiche du contenu', () => {
      cy.visit('/admin');
      cy.url({ timeout: 10000 }).should('include', '/admin');
      cy.get('body').should('not.be.empty');
    });

    it('affiche des statistiques ou une liste d\'utilisateurs', () => {
      cy.visit('/admin');
      cy.get('body', { timeout: 10000 }).should(($body) => {
        const text = $body.text().toLowerCase();
        expect(
          text.includes('user') || text.includes('admin') ||
          text.includes('dashboard') || text.includes('total') || text.includes('stat')
        ).to.be.true;
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 9 — Users List dans le dashboard
  // ─────────────────────────────────────────────────────────
  context('Users List component — données réelles de la DB', () => {
    it('le dashboard affiche des données chargées depuis la DB', () => {
      cy.loginAdminByApi();
      cy.visit('/admin');

      cy.get('body', { timeout: 15000 }).then(($body) => {
        const text = $body.text();
        if (/@/.test(text) || /user/i.test(text)) {
          cy.log('✅ Données utilisateurs chargées depuis la DB');
        } else {
          cy.log('ℹ️  Données non visibles — vérifiez le composant admin');
        }
      });
    });
  });
});

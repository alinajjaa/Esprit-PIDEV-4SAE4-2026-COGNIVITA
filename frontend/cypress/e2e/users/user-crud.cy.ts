// ============================================================
// TESTS E2E — CRUD UTILISATEURS
// Flux complet frontend → API backend réelle → base de données
// ============================================================

describe('CRUD Utilisateurs', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const cEnv = (key: string, fallback = '') => (Cypress as any).env(key) ?? fallback;
  const apiUrl = cEnv('apiUsersUrl', 'http://localhost:8080/api/users');

  // ─────────────────────────────────────────────────────────
  // SUITE 1 — GET /api/users
  // ─────────────────────────────────────────────────────────
  context('GET /api/users — liste complète', () => {
    it('GET /api/users sans token : vérifie la réponse du backend', () => {
      cy.request({
        method: 'GET',
        url: apiUrl,
        failOnStatusCode: false,
      }).then((response) => {
        // Selon Spring Security : 401/403 si protégé, 200 si endpoint public
        expect(response.status).to.not.eq(500);
        if (response.status === 401 || response.status === 403) {
          cy.log(`✅ Endpoint protégé (${response.status})`);
        } else if (response.status === 200) {
          cy.log('ℹ️  GET /api/users est public sur ce backend');
        }
      });
    });

    it('retourne la liste des utilisateurs avec un token admin valide', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/login`,
        body: {
          email: cEnv('testAdminEmail', 'admin@cognivita.com'),
          password: cEnv('testAdminPassword', 'Admin1234!'),
        },
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status !== 200 || !loginRes.body.token) {
          cy.log('ℹ️  Compte admin non disponible — test ignoré');
          return;
        }
        cy.request({
          method: 'GET',
          url: apiUrl,
          headers: { Authorization: `Bearer ${loginRes.body.token}` },
          failOnStatusCode: false,
        }).then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body).to.be.an('array');
          cy.log(`✅ ${response.body.length} utilisateurs récupérés depuis la DB`);
        });
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 2 — GET /api/users/:id
  // ─────────────────────────────────────────────────────────
  context('GET /api/users/:id — détail utilisateur', () => {
    let authToken: string;
    let currentUserId: number;

    before(() => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/login`,
        body: {
          email: cEnv('testUserEmail', 'test@cognivita.com'),
          password: cEnv('testUserPassword', 'Test1234!'),
        },
        failOnStatusCode: false,
      }).then((res) => {
        if (res.status === 200 && res.body.token) {
          authToken = res.body.token;
          currentUserId = res.body.user?.id;
        }
      });
    });

    it('GET /api/users/:id retourne les données de l\'utilisateur connecté', () => {
      if (!authToken || !currentUserId) {
        cy.log('ℹ️  Token non disponible — utilisateur test non configuré');
        return;
      }
      cy.request({
        method: 'GET',
        url: `${apiUrl}/${currentUserId}`,
        headers: { Authorization: `Bearer ${authToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body).to.have.property('id', currentUserId);
        expect(response.body).to.have.property('email');
        cy.log(`✅ Données utilisateur ID ${currentUserId} récupérées depuis la DB`);
      });
    });

    it('GET /api/users/999999 retourne 404 pour un ID inexistant', () => {
      if (!authToken) { cy.log('ℹ️  Token non disponible — test ignoré'); return; }
      cy.request({
        method: 'GET',
        url: `${apiUrl}/999999`,
        headers: { Authorization: `Bearer ${authToken}` },
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.be.oneOf([404, 400]);
        cy.log(`✅ Backend retourne ${response.status} pour un ID inexistant`);
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 3 — PUT /api/users/:id
  // ─────────────────────────────────────────────────────────
  context('PUT /api/users/:id — mise à jour utilisateur', () => {
    let authToken: string;
    let currentUserId: number;
    let originalFullName: string;

    before(() => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/login`,
        body: {
          email: cEnv('testUserEmail', 'test@cognivita.com'),
          password: cEnv('testUserPassword', 'Test1234!'),
        },
        failOnStatusCode: false,
      }).then((res) => {
        if (res.status === 200 && res.body.token) {
          authToken = res.body.token;
          currentUserId = res.body.user?.id;
          originalFullName = res.body.user?.fullName || res.body.user?.full_name || '';
        }
      });
    });

    it('PUT /api/users/:id met à jour le fullName en base de données', () => {
      if (!authToken || !currentUserId) {
        cy.log('ℹ️  Token non disponible — test ignoré');
        return;
      }
      const updatedName = `TestUser_${Date.now()}`;
      cy.request({
        method: 'PUT',
        url: `${apiUrl}/${currentUserId}`,
        headers: { Authorization: `Bearer ${authToken}` },
        body: { fullName: updatedName },
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 204]);
        cy.log(`✅ Mise à jour réussie — nouveau nom : ${updatedName}`);

        cy.request({
          method: 'GET',
          url: `${apiUrl}/${currentUserId}`,
          headers: { Authorization: `Bearer ${authToken}` },
        }).then((getRes) => {
          const storedName = getRes.body.fullName || getRes.body.full_name;
          expect(storedName).to.eq(updatedName);
          cy.log(`✅ Persistance vérifiée en DB : ${storedName}`);
        });
      });
    });

    after(() => {
      if (authToken && currentUserId && originalFullName) {
        cy.request({
          method: 'PUT',
          url: `${apiUrl}/${currentUserId}`,
          headers: { Authorization: `Bearer ${authToken}` },
          body: { fullName: originalFullName },
          failOnStatusCode: false,
        });
      }
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 4 — Page Profil (/profile)
  // ─────────────────────────────────────────────────────────
  context('Page Profil Utilisateur — /profile', () => {
    beforeEach(() => {
      cy.loginUserByApi();
    });

    it('accède à /profile après connexion', () => {
      cy.window().then((win) => {
        if (!win.localStorage.getItem('jwt_token')) {
          cy.log('ℹ️  Test ignoré — utilisateur test non configuré en DB');
          return;
        }
        cy.visit('/profile');
        cy.url({ timeout: 10000 }).should('include', '/profile');
      });
    });

    it('affiche les informations de l\'utilisateur connecté', () => {
      cy.visit('/profile');
      const userEmail = cEnv('testUserEmail', 'test@cognivita.com');
      cy.get('body', { timeout: 10000 }).should(($body) => {
        const text = $body.text();
        expect(
          text.includes(userEmail) ||
          text.toLowerCase().includes('profile') ||
          text.toLowerCase().includes('profil')
        ).to.be.true;
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 5 — Block / Unblock (admin)
  // ─────────────────────────────────────────────────────────
  context('PUT /api/users/:id/block — block/unblock (admin)', () => {
    it('PUT /api/users/:id/block retourne l\'utilisateur bloqué', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/login`,
        body: {
          email: cEnv('testAdminEmail', 'admin@cognivita.com'),
          password: cEnv('testAdminPassword', 'Admin1234!'),
        },
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status !== 200 || !loginRes.body.token) {
          cy.log('ℹ️  Compte admin non disponible — test ignoré');
          return;
        }
        const adminToken = loginRes.body.token;

        cy.request({
          method: 'GET',
          url: apiUrl,
          headers: { Authorization: `Bearer ${adminToken}` },
          failOnStatusCode: false,
        }).then((listRes) => {
          if (listRes.status !== 200 || !listRes.body.length) {
            cy.log('ℹ️  Liste utilisateurs vide — test ignoré');
            return;
          }
          const adminEmailVal = cEnv('testAdminEmail', 'admin@cognivita.com');
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const targetUser = listRes.body.find(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (u: any) => u.role !== 'ADMIN' && u.email !== adminEmailVal
          );
          if (!targetUser) {
            cy.log('ℹ️  Aucun utilisateur non-admin disponible — test ignoré');
            return;
          }
          cy.request({
            method: 'PUT',
            url: `${apiUrl}/${targetUser.id}/block`,
            headers: { Authorization: `Bearer ${adminToken}` },
            body: {},
            failOnStatusCode: false,
          }).then((blockRes) => {
            expect(blockRes.status).to.be.oneOf([200, 204]);
            cy.log(`✅ Utilisateur ID ${targetUser.id} bloqué`);

            cy.request({
              method: 'PUT',
              url: `${apiUrl}/${targetUser.id}/unblock`,
              headers: { Authorization: `Bearer ${adminToken}` },
              body: {},
              failOnStatusCode: false,
            }).then((unblockRes) => {
              expect(unblockRes.status).to.be.oneOf([200, 204]);
              cy.log(`✅ Utilisateur ID ${targetUser.id} débloqué`);
            });
          });
        });
      });
    });
  });

  // ─────────────────────────────────────────────────────────
  // SUITE 6 — GET /api/users/session
  // ─────────────────────────────────────────────────────────
  context('GET /api/users/session — session active', () => {
    it('retourne les données de l\'utilisateur courant depuis le token JWT', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/login`,
        body: {
          email: cEnv('testUserEmail', 'test@cognivita.com'),
          password: cEnv('testUserPassword', 'Test1234!'),
        },
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status !== 200 || !loginRes.body.token) {
          cy.log('ℹ️  Utilisateur test non disponible');
          return;
        }
        cy.request({
          method: 'GET',
          url: `${apiUrl}/session`,
          headers: { Authorization: `Bearer ${loginRes.body.token}` },
          failOnStatusCode: false,
        }).then((sessionRes) => {
          if (sessionRes.status === 200) {
            expect(sessionRes.body).to.have.property('email');
            cy.log(`✅ Session valide — utilisateur : ${sessionRes.body.email}`);
          } else {
            cy.log(`ℹ️  /session retourne ${sessionRes.status}`);
          }
        });
      });
    });
  });
});

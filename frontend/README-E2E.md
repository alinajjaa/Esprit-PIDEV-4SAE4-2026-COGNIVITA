# Tests E2E Cypress — COGNIVITA

Tests end-to-end avec **vrais appels HTTP** vers le backend réel.  
Aucun mock, aucun stub, aucune simulation — le flux complet est testé :  
`Frontend Angular → API Spring Boot → Base de données`.

---

## Prérequis

Avant de lancer les tests, **les trois services doivent être démarrés** :

| Service | URL | Commande |
|---|---|---|
| Angular frontend | `http://localhost:4200` | `npm start` |
| Spring Boot (users) | `http://localhost:8081` | `mvn spring-boot:run` |
| Spring Boot (admin) | `http://localhost:8080` | (même instance ou séparée) |
| Python (CNN predict) | `http://localhost:8000` | `uvicorn main:app` |

> **Les tests ne fonctionneront pas sans backend actif.** C'est intentionnel :  
> ces tests valident l'intégration complète, pas des composants isolés.

---

## Comptes de test requis

Créez ces deux comptes dans votre base de données **avant** de lancer les tests :

```
Utilisateur standard :
  email    : test@cognivita.com
  password : Test1234!
  role     : USER
  2FA      : désactivé (recommandé pour les tests automatisés)
  Face ID  : désactivé (recommandé)

Administrateur :
  email    : admin@cognivita.com
  password : Admin1234!
  role     : ADMIN
```

> Si vos credentials diffèrent, modifiez [cypress/fixtures/credentials.json](cypress/fixtures/credentials.json)  
> ou passez-les en ligne de commande (voir section "Personnalisation").

---

## Lancer les tests

### Tous les tests (mode headless CI)
```bash
npm run e2e:real
```

### Interface graphique Cypress (recommandé pour le débogage)
```bash
npm run e2e:real:open
```
Puis sélectionnez "E2E Testing" → choisissez un navigateur → lancez les specs.

### Par fonctionnalité
```bash
npm run e2e:auth      # Tests login, register, logout, guards
npm run e2e:users     # Tests CRUD utilisateurs + profil
npm run e2e:nav       # Tests navigation et route guards
npm run e2e:admin     # Tests dashboard admin + API admin
```

### Spec individuelle
```bash
npx cypress run --spec "cypress/e2e/auth/login.cy.ts"
npx cypress run --spec "cypress/e2e/users/user-crud.cy.ts"
```

---

## Personnaliser les credentials (sans modifier les fichiers)

### Via variables d'environnement Cypress (CLI)
```bash
npx cypress run \
  --env testUserEmail=monuser@example.com \
  --env testUserPassword=MonPassword1! \
  --env testAdminEmail=admin@example.com \
  --env testAdminPassword=AdminPass1!
```

### Via fichier `cypress.env.json` (gitignored)
Créez ce fichier à la racine du projet frontend :
```json
{
  "testUserEmail": "monuser@example.com",
  "testUserPassword": "MonPassword1!",
  "testAdminEmail": "admin@example.com",
  "testAdminPassword": "AdminPass1!",
  "apiUsersUrl": "http://localhost:8081/api/users",
  "apiAdminUrl": "http://localhost:8080/api/admin"
}
```

> `cypress.env.json` est automatiquement ignoré par Cypress pour le versioning  
> (ne pas committer vos vrais credentials).

---

## Structure des tests

```
cypress/
├── e2e/
│   ├── auth/
│   │   ├── login.cy.ts        # Login UI, API, guards, logout, comptes bloqués
│   │   └── register.cy.ts     # Inscription UI, validation email async, OTP
│   ├── users/
│   │   └── user-crud.cy.ts    # GET/PUT users, profil, block/unblock
│   ├── navigation/
│   │   └── navigation.cy.ts   # Routes publiques, authGuard, adminGuard, publicGuard
│   └── admin/
│       └── admin-dashboard.cy.ts  # Dashboard, stats, search, filter, export, activity-log
├── fixtures/
│   └── credentials.json       # Credentials de test (à adapter)
└── support/
    ├── commands.ts             # cy.loginByApi(), cy.loginAdminByApi(), cy.logout()
    └── e2e.ts                  # Configuration globale (hooks beforeEach)
```

---

## Ce que testent ces fichiers

### `auth/login.cy.ts`
- Affichage du formulaire login
- Soumission avec credentials invalides → erreur backend réelle (HTTP 401/403)
- Login via formulaire UI → redirection `/home` ou `/admin`
- `POST /api/users/login` retourne un JWT valide
- `adminGuard` : `/admin` redirigé vers `/login` sans token
- Logout → JWT supprimé du localStorage → `/login`

### `auth/register.cy.ts`
- Affichage du formulaire d'inscription
- Validation async email via `GET /api/users/by-email` (appel backend réel)
- `POST /api/users` crée un utilisateur en base
- Email en doublon → backend retourne 409
- Flux UI complet → étape OTP après soumission réussie

### `users/user-crud.cy.ts`
- `GET /api/users` protégé : 401 sans token
- `GET /api/users` retourne la liste complète (token admin)
- `GET /api/users/:id` retourne un utilisateur par ID
- `GET /api/users/999999` → 404 pour ID inexistant
- `PUT /api/users/:id` met à jour et **vérifie la persistance en DB**
- `PUT /api/users/:id/block` + `/unblock` (restaure l'état après test)
- `GET /api/users/session` vérifie le token actif
- Page `/profile` accessible et affiche les données

### `navigation/navigation.cy.ts`
- Routes publiques accessibles sans token
- `authGuard` : 6 routes protégées redirigent vers `/login`
- `adminGuard` : `/admin` inaccessible pour un user standard
- `publicGuard` : `/login` et `/register` redirigent si déjà connecté
- Toutes les routes protégées accessibles après `loginByApi()`
- Token JWT invalide/forgé → comportement documenté

### `admin/admin-dashboard.cy.ts`
- `/admin` protégé : redirige sans token
- `GET /api/admin/dashboard` → données réelles
- `GET /api/admin/stats` → statistiques depuis la DB
- `GET /api/admin/search?query=test` → résultats filtrés
- `GET /api/admin/filter?role=USER` → uniquement les USERs
- `GET /api/admin/export/users` et `/export/mmse`
- `GET /api/admin/activity-log?limit=50`
- Interface admin affiche des données chargées depuis la DB

---

## Commandes custom disponibles

Définies dans [cypress/support/commands.ts](cypress/support/commands.ts) :

```typescript
// Login via POST /api/users/login réel → stocke JWT dans localStorage
cy.loginByApi('email@test.com', 'Password1!')

// Login avec les credentials de test configurés
cy.loginUserByApi()   // testUserEmail + testUserPassword
cy.loginAdminByApi()  // testAdminEmail + testAdminPassword

// Efface localStorage et cookies
cy.logout()

// Vérifie que le backend répond avant les tests
cy.assertBackendAvailable()
```

---

## Dépannage

| Problème | Cause probable | Solution |
|---|---|---|
| `Backend non disponible` | Spring Boot ne tourne pas | `mvn spring-boot:run` |
| `Login API failed (401)` | Mauvais credentials | Vérifier `cypress.env.json` |
| `Login API failed (403)` | Compte bloqué | Débloquer le compte en DB |
| Tests en timeout | Angular app non démarrée | `npm start` dans un autre terminal |
| `Cannot find module 'cypress'` | Installation incomplète | `npm install` |
| Page redirige toujours vers /login | JWT non persisté | Vérifier `loginByApi` dans les logs |

---

## Notes sur la stratégie de test

**Pourquoi `cy.loginByApi()` au lieu du formulaire ?**  
Pour les tests qui ne testent PAS le login lui-même, utiliser l'API directement est plus rapide et plus fiable. Le login multi-étapes (2FA + Face ID) ne peut pas être automatisé sans caméra réelle — on bypass donc la UI pour ces tests, tout en utilisant le vrai endpoint JWT.

**Pourquoi les tests sont conditionnels (`if (!adminToken)`)?**  
Certains tests nécessitent un compte admin ou un utilisateur test configuré en DB. Si ces comptes n'existent pas (premier lancement, nouvelle DB), les tests sont ignorés proprement plutôt que de casser tout le run.

**Ordre recommandé pour un premier lancement :**
1. Créer les comptes test en DB
2. Démarrer backend + frontend
3. `npm run e2e:real:open` pour voir les tests en direct
4. Corriger les credentials si nécessaire
5. `npm run e2e:real` pour le run complet en CI

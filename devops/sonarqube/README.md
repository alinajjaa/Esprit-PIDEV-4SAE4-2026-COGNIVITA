## SonarQube (local) pour `pi` et `planSuivi`

### Prérequis
- Docker Desktop démarré (Windows) **ou** Docker fonctionnel dans WSL avec accès au socket.

### Démarrer SonarQube + PostgreSQL
Dans PowerShell, à la racine du dossier `devops/sonarqube` :

```powershell
Copy-Item .\.env.example .\.env
notepad .\.env
docker compose pull
docker compose up -d --force-recreate
```

Attends 1–3 minutes au premier démarrage, puis ouvre :
- `http://localhost:9000`

Identifiants par défaut SonarQube :
- user : `admin`
- password : `admin` (il te demandera de le changer au premier login)

### Si l’UI affiche “SonarQube is offline” / `/maintenance`
Ça arrive quand le conteneur **redémarre**, **n’a pas fini de booter**, ou quand l’image/tag est incorrect.

Vérifie l’état :

```powershell
docker compose ps
docker compose logs --tail=200 sonarqube-db
docker compose logs --tail=200 sonarqube
```

Checklist rapide :
- **Docker Desktop → Settings → Resources** : donne au moins **8 Go RAM** et **4 CPUs** (Sonar est lourd).
- Le port **9000** ne doit pas être pris par une autre appli.
- Si tu as déjà démarré une mauvaise image une fois, refais un recreate (commande ci-dessus).

### Créer un token (pour GitHub Actions)
Dans SonarQube :
- **My Account → Security → Generate Tokens**

Ensuite sur GitHub (repo) :
- `SONAR_HOST_URL` = `http://<ton-ip-lan>:9000` si tu analyses depuis GitHub-hosted runners, **ou** mieux : un SonarQube accessible publiquement / via VPN / self-hosted runner.
- `SONAR_TOKEN` = le token généré

### Projets analysés par le repo
Les workflows utilisent déjà :
- `cognivita-pi`
- `cognivita-plansuivi`

Sonar créera les projets automatiquement au premier scan Maven si le token a les droits.

### Commandes d'analyse locale (PI + PlanSuivi)
Depuis la racine du repo :

```powershell
cd .\pi\pi
mvn -B clean verify sonar:sonar `
  -Dsonar.projectKey=cognivita-pi `
  -Dsonar.projectName=COGNIVITA-PI `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.token=<TON_TOKEN>

cd ..\..\planSuivi\planSuivi
mvn -B clean verify sonar:sonar `
  -Dsonar.projectKey=cognivita-plansuivi `
  -Dsonar.projectName=COGNIVITA-PlanSuivi `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.token=<TON_TOKEN>
```

### Presentation demandee (etat initial / apres amelioration)
1. **Etat initial**
   - Lance les analyses Sonar avant refactoring.
   - Prends des captures de `Overview`, `Issues`, `Code Smells`, `Coverage`, `Duplications`.
2. **Amelioration**
   - Applique refactoring + ajout/amelioration des tests unitaires.
   - Relance l'analyse Sonar.
3. **Etat final**
   - Prends les memes captures et compare : baisse des code smells/bugs + hausse de la couverture.

### Captures recommandees pour la soutenance
- Dashboard global du projet `cognivita-pi`.
- Dashboard global du projet `cognivita-plansuivi`.
- Onglet `Measures` avec `Coverage` et `Lines to cover`.
- Onglet `Issues` filtre par severite (avant/apres).
- Resultat des pipelines GitHub Actions (`CI PI`, `CI PlanSuivi`) montrant le passage Sonar.

### Note importante (CI GitHub)
Les runners GitHub **ne peuvent pas joindre** un SonarQube sur `localhost` de ton PC.

Options :
- **SonarCloud** (simple pour un projet étudiant), ou
- exposer SonarQube sur une URL réseau accessible aux runners, ou
- utiliser un **self-hosted runner** sur ta machine/VM du groupe.

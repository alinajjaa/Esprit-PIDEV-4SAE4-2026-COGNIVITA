# README-DEV - activities-service (mode local sans conflit équipe)

## Objectif
Exécuter le développement local de `activities-service` sans toucher :
- au code source (`src/**`)
- aux ports des microservices
- au `docker-compose.yml` partagé de l'équipe à la racine

## Principe
- Les microservices démarrent dans IntelliJ avec leurs ports originaux :
  - `activities-service` -> `8087`
  - `api-gateway` -> `9090`
  - `eureka-server` -> `8761`
- Docker dans `activities-service/devops` démarre uniquement l'infrastructure :
  - MySQL -> `3307`
  - Prometheus -> `9091`
  - Grafana -> `3001`

## Structure locale DevOps
- `devops/docker-compose.devops.yml`
- `devops/prometheus/prometheus.yml`
- `devops/grafana/provisioning/...`
- `devops/scripts/start-docker.ps1`
- `devops/scripts/stop-docker.ps1`
- `devops/scripts/health-check.ps1`
- `devops/scripts/run-dev.ps1`
- `.env.local` (ignoré par git)

## Lancement rapide
Depuis `activities-service/` :

```powershell
.\devops\scripts\run-dev.ps1
```

Ou manuellement :

```powershell
.\devops\scripts\start-docker.ps1
```

Puis lancer dans IntelliJ :
- `activities-service` (port `8087`)
- `api-gateway` (port `9090`)
- `eureka-server` (port `8761`)

## Vérification
```powershell
.\devops\scripts\health-check.ps1
```

URLs utiles :
- MySQL: `localhost:3307`
- Prometheus: `http://localhost:9091`
- Grafana: `http://localhost:3001` (`admin` / `admin`)
- Activities API (IntelliJ): `http://localhost:8087/api/journal`

## Arrêt
```powershell
.\devops\scripts\stop-docker.ps1
```

## Dépannage
- Si Prometheus ne voit pas les microservices, vérifier qu'ils tournent dans IntelliJ.
- `host.docker.internal` est requis pour que Prometheus en conteneur scrape les services sur la machine hôte.
- Ne pas démarrer `activities-service` en Docker en même temps qu'en IntelliJ sur `8087`.


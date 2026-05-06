README - Dashboard Cognitive Activities

But
----
Ce dossier contient des scripts pour :
- Installer le plugin Grafana "JSON API" (simpod-json-datasource)
- Créer une datasource Grafana pointant vers le service activities-service
- Importer un dashboard Grafana qui interroge les endpoints REST
- Vérifier la santé des endpoints REST

Contraintes
-----------
- Aucun code source (.java/.properties/.yml) n'est modifié.
- Les ports des microservices ne sont pas modifiés (ex: 8087 pour activities-service).

Prérequis
---------
- Docker Desktop en fonctionnement.
- Un conteneur Grafana accessible (par défaut : http://localhost:3001). Si votre Grafana écoute sur un autre port, exportez $env:GRAFANA_URL avant d'exécuter les scripts.
- Identifiants Grafana (admin) via GRAFANA_ADMIN_USER et GRAFANA_ADMIN_PASSWORD ou utilisez le mot de passe par défaut.

Variables d'environnement utiles
--------------------------------
- GRAFANA_URL (défaut: http://localhost:3001)
- GRAFANA_ADMIN_USER (défaut: admin)
- GRAFANA_ADMIN_PASSWORD
- GRAFANA_CONTAINER (défaut: grafana)
- DATASOURCE_NAME (défaut: Activities JSON API)
- DATASOURCE_URL (défaut: http://host.docker.internal:8087)
- SERVICE_BASE_URL (pour health-check, défaut: http://host.docker.internal:8087)

Ordre d'exécution
-----------------
1) Installer le plugin JSON API :
   .\\scripts\\install-json-datasource.ps1
2) Créer la datasource :
   $env:GRAFANA_ADMIN_USER = 'admin'; $env:GRAFANA_ADMIN_PASSWORD = 'yourpw'; .\\scripts\\setup-json-datasource.ps1
3) Importer le dashboard :
   .\\scripts\\setup-grafana-dashboard.ps1
4) Vérifier les endpoints :
   .\\health-check.ps1

Validation
----------
- Vérifier dans Grafana (UI) que la datasource 'Activities JSON API' existe.
- Vérifier Dashboards → "Cognitive Activities".

Notes
-----
- Les panels du dashboard utilisent l'URL interne 'http://host.docker.internal:8087' pour joindre services qui tournent sur l'hôte depuis le conteneur Grafana.
- Si votre conteneur Grafana a un nom différent, exportez GRAFANA_CONTAINER avant d'exécuter le script d'installation du plugin.


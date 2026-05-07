# Cognivita Minikube Deployment

This folder contains a simple Kubernetes deployment for the current Cognivita
application stack only:

- mysql
- eureka-server
- api-gateway
- content-service
- tracking-service
- dashboard-service
- frontend

Jenkins and SonarQube stay outside Kubernetes for this DevOps sprint. Jenkins
builds and pushes application images to Docker Hub, then Kubernetes pulls those
images directly.

## Docker Hub Images

The manifests use these public Docker Hub image names:

- `medsadek/frontend:latest`
- `medsadek/content-service:latest`
- `medsadek/tracking-service:latest`
- `medsadek/dashboard-service:latest`
- `medsadek/api-gateway:latest`
- `medsadek/eureka-server:latest`

Each application deployment uses:

```yaml
imagePullPolicy: Always
```

Minikube pulls these images directly from Docker Hub.

## Deploy

Start Minikube:

```powershell
minikube start
```

Apply everything:

```powershell
kubectl apply -k .\k8s
```

Deployment order if applying file-by-file:

```powershell
kubectl apply -f .\k8s\00-namespace.yaml
kubectl apply -f .\k8s\01-mysql.yaml
kubectl wait --namespace cognivita --for=condition=available deployment/mysql --timeout=180s
kubectl apply -f .\k8s\02-eureka-server.yaml
kubectl wait --namespace cognivita --for=condition=available deployment/eureka-server --timeout=180s
kubectl apply -f .\k8s\03-content-service.yaml
kubectl apply -f .\k8s\04-tracking-service.yaml
kubectl apply -f .\k8s\05-dashboard-service.yaml
kubectl apply -f .\k8s\06-api-gateway.yaml
kubectl wait --namespace cognivita --for=condition=available deployment/api-gateway --timeout=180s
kubectl apply -f .\k8s\07-frontend.yaml
```

## Update Existing Deployment

After Jenkins pushes new `latest` images:

```powershell
kubectl apply -k .\k8s
kubectl rollout restart deployment/eureka-server -n cognivita
kubectl rollout restart deployment/api-gateway -n cognivita
kubectl rollout restart deployment/content-service -n cognivita
kubectl rollout restart deployment/tracking-service -n cognivita
kubectl rollout restart deployment/dashboard-service -n cognivita
kubectl rollout restart deployment/frontend -n cognivita
```

## Verify Image Pulling

```powershell
kubectl get pods -n cognivita
kubectl describe pod -n cognivita <pod-name>
kubectl get events -n cognivita --sort-by=.lastTimestamp
```

Look for successful pulls such as:

```text
Successfully pulled image "medsadek/frontend:latest"
```

Check the images currently assigned to deployments:

```powershell
kubectl get deployments -n cognivita -o wide
```

## Logs

```powershell
kubectl logs -n cognivita deployment/eureka-server
kubectl logs -n cognivita deployment/api-gateway
kubectl logs -n cognivita deployment/content-service
kubectl logs -n cognivita deployment/tracking-service
kubectl logs -n cognivita deployment/dashboard-service
kubectl logs -n cognivita deployment/frontend
```

## Access Frontend

The frontend service is exposed as NodePort `30420` and maps service port `4200`
to container port `80`.

Recommended:

```powershell
minikube service frontend -n cognivita --url
```

Open the printed URL in the browser.

Manual URL:

```powershell
minikube ip
```

Then open:

```text
http://<minikube-ip>:30420
```

For local port `4200`:

```powershell
kubectl port-forward -n cognivita service/frontend 4200:4200
```

Then open:

```text
http://localhost:4200
```

## Cleanup

```powershell
kubectl delete namespace cognivita
```

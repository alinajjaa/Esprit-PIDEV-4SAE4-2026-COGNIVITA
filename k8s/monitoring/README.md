# Cognivita Monitoring

Lightweight Prometheus + Grafana monitoring for the `cognivita` namespace.

This setup is intentionally simple for Minikube and academic DevOps demos. It
does not use Helm, operators, Alertmanager, or kube-prometheus-stack.

## Components

- Prometheus
  - Deployment: `prometheus`
  - Service: `prometheus`
  - NodePort: `30090`
  - ConfigMap: `prometheus-config`
- Grafana
  - Deployment: `grafana`
  - Service: `grafana`
  - NodePort: `30300`
  - Auto-provisioned Prometheus datasource
  - Auto-provisioned `Cognivita Overview` dashboard

## Deploy

Deploy the application stack first:

```powershell
kubectl apply -k .\k8s
```

Deploy monitoring:

```powershell
kubectl apply -k .\k8s\monitoring
```

Wait for rollout:

```powershell
kubectl rollout status deployment/prometheus -n cognivita
kubectl rollout status deployment/grafana -n cognivita
```

## Verify

```powershell
kubectl get pods -n cognivita
kubectl get services -n cognivita
kubectl logs -n cognivita deployment/prometheus
kubectl logs -n cognivita deployment/grafana
```

Prometheus targets:

```powershell
kubectl port-forward -n cognivita service/prometheus 9090:9090
```

Open:

```text
http://localhost:9090/targets
```

## Access Prometheus

Recommended:

```powershell
minikube service prometheus -n cognivita --url
```

Manual:

```powershell
minikube ip
```

Open:

```text
http://<minikube-ip>:30090
```

## Access Grafana

Recommended:

```powershell
minikube service grafana -n cognivita --url
```

Manual:

```powershell
minikube ip
```

Open:

```text
http://<minikube-ip>:30300
```

Default login:

```text
admin / admin
```

Grafana automatically connects to Prometheus at:

```text
http://prometheus:9090
```

## Example Dashboards

Included dashboard:

- `Cognivita Overview`

Panels:

- Pod CPU usage
- Pod memory usage
- Prometheus scrape target health
- Spring Boot HTTP request rate
- Spring Boot JVM memory

Useful public Grafana dashboard ideas for manual import:

- Kubernetes cluster monitoring dashboards using cAdvisor metrics
- JVM Micrometer dashboards for Spring Boot
- Prometheus 2.0 overview dashboards

Spring Boot service panels require the services to expose Prometheus metrics at:

```text
/actuator/prometheus
```

If a Spring service does not include Micrometer Prometheus registry yet, the
Kubernetes CPU/memory panels will still work through cAdvisor metrics.

## Cleanup

```powershell
kubectl delete -k .\k8s\monitoring
```

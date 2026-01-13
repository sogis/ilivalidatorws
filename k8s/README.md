# Kubernetes Deployment

Kubernetes-spezifische Dokumentation für ilivalidator-web-service. Für allgemeine Informationen siehe [../README.md](../README.md).

## Deployment-Varianten

| File | Beschreibung | Worker | Use Case |
|------|--------------|--------|----------|
| `deployment-base.yaml` | Standard Deployment | 2 (fix) | Vorhersagbare Last |
| `deployment-keda.yaml` | KEDA Autoscaling | 0-2 (dynamisch) | Variable Last, Scale-to-Zero |

## Quick Start

```bash
# Standard (ohne KEDA)
kubectl apply -f k8s/deployment-base.yaml

# Mit KEDA Autoscaling
# 1. KEDA installieren
helm install keda kedacore/keda --namespace keda --create-namespace

# 2. Prometheus deployen (für KEDA Metrics)
kubectl apply -f k8s/prometheus.yaml

# 3. KEDA Deployment
kubectl apply -f k8s/deployment-keda.yaml
```

## Voraussetzungen

### Shared Storage (ReadWriteMany)

Alle Pods (Frontend + Worker) benötigen Zugriff auf `/work/` für:
- SQLite Job Queue DB
- Upload Files
- Log Files

**Unterstützte Storage-Classes:**
- NFS
- CephFS
- GlusterFS
- Azure Files
- AWS EFS

**Testen:**
```bash
kubectl get storageclass
kubectl describe storageclass <name>
```

**Falls RWX nicht verfügbar:**
- NFS Server aufsetzen
- Auf PostgreSQL statt SQLite wechseln

### KEDA (nur für deployment-keda.yaml)

**Installation:**
```bash
helm repo add kedacore https://kedacore.github.io/charts
helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# Verify
kubectl get pods -n keda
```

### Prometheus (nur für deployment-keda.yaml)

KEDA benötigt einen Prometheus-Server zum Abfragen der Metriken. Ein minimales Prometheus-Setup ist in `k8s/prometheus.yaml` enthalten.

**Installation:**
```bash
kubectl apply -f k8s/prometheus.yaml

# Verify
kubectl get pods -n ilivalidator -l app=prometheus
kubectl logs -n ilivalidator deployment/prometheus
```

**Prometheus UI (optional):**
```bash
kubectl port-forward -n ilivalidator svc/prometheus 9090:9090
# Browser: http://localhost:9090
# Query testen: jobrunr_pending_jobs
```

**Hinweis:** Für `deployment-base.yaml` ist Prometheus nicht erforderlich.

## Konfiguration anpassen

### ConfigMap bearbeiten

```bash
kubectl edit configmap ilivalidator-config -n ilivalidator
```

Wichtige Einstellungen:
- `TZ` - Zeitzone
- `LOG_LEVEL_APPLICATION` - Log Level
- `ILIDIRS` - INTERLIS Model Repositories
- `MAX_FILE_SIZE` - Upload Limit (MB)

Nach Änderungen Pods neu starten:
```bash
kubectl rollout restart deployment/ilivalidator-frontend -n ilivalidator
kubectl rollout restart deployment/ilivalidator-worker -n ilivalidator
```

### Worker Resources anpassen

```yaml
# In deployment-*.yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "8Gi"
    cpu: "4000m"
```

### KEDA Scaling anpassen

```yaml
# In deployment-keda.yaml
spec:
  minReplicaCount: 0
  maxReplicaCount: 10  # Mehr Worker
  pollingInterval: 10   # Schnelleres Polling

  triggers:
  - type: prometheus
    metadata:
      threshold: "2"  # Aggressiveres Scaling (1 Worker pro 2 Jobs)
```

## Monitoring

### Metrics prüfen

```bash
# Port-Forward
kubectl port-forward -n ilivalidator svc/ilivalidator-frontend 8080:8080

# Metrics abrufen
curl http://localhost:8080/actuator/prometheus | grep jobrunr
```

### KEDA Status

```bash
# ScaledObject
kubectl get scaledobject -n ilivalidator
kubectl describe scaledobject ilivalidator-worker-scaler -n ilivalidator

# HPA (wird von KEDA erstellt)
kubectl get hpa -n ilivalidator

# KEDA Operator Logs
kubectl logs -n keda deployment/keda-operator -f
```

### Logs

```bash
# Frontend
kubectl logs -f deployment/ilivalidator-frontend -n ilivalidator

# Worker
kubectl logs -f deployment/ilivalidator-worker -n ilivalidator

# Alle Pods
kubectl logs -f -l app=ilivalidator -n ilivalidator --all-containers=true

# Mit Timestamps
kubectl logs --tail=100 --timestamps deployment/ilivalidator-worker -n ilivalidator
```

## Ingress (optional)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ilivalidator-ingress
  namespace: ilivalidator
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "200m"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - ilivalidator.example.com
    secretName: ilivalidator-tls
  rules:
  - host: ilivalidator.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ilivalidator-frontend
            port:
              number: 8080
```

## ServiceMonitor für Prometheus

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: ilivalidator-frontend
  namespace: ilivalidator
spec:
  selector:
    matchLabels:
      app: ilivalidator
      component: frontend
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

## Secrets Management

### Jobrunr Dashboard Credentials

```bash
# Secret erstellen
kubectl create secret generic jobrunr-creds \
  --from-literal=username=admin \
  --from-literal=password=secure-password \
  -n ilivalidator

# In Deployment referenzieren
env:
- name: JOBRUNR_DASHBOARD_USER
  valueFrom:
    secretKeyRef:
      name: jobrunr-creds
      key: username
- name: JOBRUNR_DASHBOARD_PWD
  valueFrom:
    secretKeyRef:
      name: jobrunr-creds
      key: password
```

## Troubleshooting

### Pods starten nicht

```bash
kubectl describe pod <pod-name> -n ilivalidator
kubectl logs <pod-name> -n ilivalidator --previous
```

Häufige Probleme:
- ImagePullBackOff → Image Tag prüfen
- CrashLoopBackOff → Logs prüfen
- Pending → PVC Status prüfen

### PVC wird nicht gemountet

```bash
kubectl get pvc -n ilivalidator
kubectl describe pvc ilivalidator-work-pvc -n ilivalidator
kubectl get pv

# Events prüfen
kubectl get events -n ilivalidator --sort-by='.lastTimestamp'
```

### KEDA skaliert nicht

```bash
# 1. Metrics verfügbar?
kubectl exec -it deployment/ilivalidator-frontend -n ilivalidator -- \
  curl localhost:8080/actuator/prometheus | grep jobrunr_pending_jobs

# 2. KEDA kann Frontend erreichen?
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -n ilivalidator -- \
  curl http://ilivalidator-frontend:8080/actuator/prometheus

# 3. ScaledObject Status
kubectl describe scaledobject ilivalidator-worker-scaler -n ilivalidator

# 4. KEDA Operator Logs
kubectl logs -n keda deployment/keda-operator --tail=100
```

### Worker Pods löschen nicht

```bash
# Force delete
kubectl delete pod <pod-name> -n ilivalidator --grace-period=0 --force

# Deployment neu starten
kubectl rollout restart deployment/ilivalidator-worker -n ilivalidator
```

## Migration

### Image Update

```bash
# Neue Version setzen
kubectl set image deployment/ilivalidator-frontend \
  ilivalidator=sogis/ilivalidator-web-service:3.1.0 \
  -n ilivalidator

kubectl set image deployment/ilivalidator-worker \
  ilivalidator=sogis/ilivalidator-web-service:3.1.0 \
  -n ilivalidator

# Rollout Status
kubectl rollout status deployment/ilivalidator-frontend -n ilivalidator
kubectl rollout status deployment/ilivalidator-worker -n ilivalidator

# Bei Problemen: Rollback
kubectl rollout undo deployment/ilivalidator-frontend -n ilivalidator
```

### Namespace löschen

```bash
# Vorsicht: Löscht alle Ressourcen!
kubectl delete namespace ilivalidator
```

## Performance Tuning

### Node Affinity (Worker auf bestimmten Nodes)

```yaml
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: workload-type
                operator: In
                values:
                - compute-intensive
```

### Pod Priority

```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: ilivalidator-high-priority
value: 1000
globalDefault: false
---
# In Deployment
spec:
  template:
    spec:
      priorityClassName: ilivalidator-high-priority
```

### Resource Quotas

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: ilivalidator-quota
  namespace: ilivalidator
spec:
  hard:
    requests.cpu: "20"
    requests.memory: 40Gi
    limits.cpu: "40"
    limits.memory: 80Gi
    persistentvolumeclaims: "5"
```

## Weitere Ressourcen

- [Haupt-README](../README.md) - Allgemeine Dokumentation
- [KEDA Documentation](https://keda.sh/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Prometheus Operator](https://prometheus-operator.dev/)

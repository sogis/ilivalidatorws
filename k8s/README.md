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

# Mit KEDA v2 Autoscaling
# 1. KEDA v2 installieren
helm repo add kedacore https://kedacore.github.io/charts
helm install keda kedacore/keda --namespace keda --create-namespace

# 2. Prometheus deployen (für KEDA Metrics)
kubectl apply -f k8s/prometheus.yaml

# 3. KEDA Deployment (verwendet KEDA v2 Features)
kubectl apply -f k8s/deployment-keda.yaml

# 4. Verify
kubectl get scaledobject -n ilivalidator
kubectl get pods -n ilivalidator
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

### KEDA v2 (nur für deployment-keda.yaml)

**Installation (empfohlene Version: v2.14+):**
```bash
helm repo add kedacore https://kedacore.github.io/charts
helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# Verify
kubectl get pods -n keda
kubectl get scaledobjects -A
```

**Upgrade von älteren KEDA Versionen:**
```bash
# Bestehende Version prüfen
helm list -n keda

# Upgrade auf neueste v2.x
helm upgrade keda kedacore/keda --namespace keda

# Falls KEDA Operator nach Upgrade crasht (ScaledJob CRD Bug):
curl -sL https://raw.githubusercontent.com/kedacore/keda/v2.18.3/config/crd/bases/keda.sh_scaledjobs.yaml | kubectl apply --server-side=true -f -
kubectl delete pod -n keda -l app.kubernetes.io/name=keda-operator
```

**KEDA v2 Features (genutzt in deployment-keda.yaml):**
- `idleReplicaCount`: Explizite Idle-Replicas für saubereres Scale-to-Zero
- `fallback`: Automatischer Fallback zu 1 Worker bei Prometheus-Ausfall
- `ignoreNullValues`: Verhindert falschen Scale-down bei fehlenden Metriken
- `restoreToOriginalReplicaCount`: Kontrolliert Rückkehr zu Deployment-Replicas

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

### KEDA v2 Scaling anpassen

```yaml
# In deployment-keda.yaml ScaledObject
spec:
  # Replicas
  minReplicaCount: 0
  idleReplicaCount: 0   # KEDA v2: Explizite Idle-Replicas
  maxReplicaCount: 10   # Mehr Worker

  # Timing
  pollingInterval: 10   # Schnelleres Polling (Sekunden)
  cooldownPeriod: 60    # Schnellerer Scale-down (Sekunden)

  # Fallback (KEDA v2)
  fallback:
    failureThreshold: 3
    replicas: 2         # Bei Prometheus-Ausfall → 2 Worker

  # Advanced (KEDA v2)
  advanced:
    restoreToOriginalReplicaCount: false
    horizontalPodAutoscalerConfig:
      behavior:
        scaleUp:
          stabilizationWindowSeconds: 0
          policies:
          - type: Pods
            value: 5    # Schnelleres Scale-up: +5 Pods
            periodSeconds: 5

  triggers:
  - type: prometheus
    metadata:
      threshold: "2"              # 1 Worker pro 2 Jobs
      activationThreshold: "0.5"  # Scale-to-Zero Trigger
      ignoreNullValues: "true"    # KEDA v2: Ignore fehlende Metriken
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

### KEDA v2 skaliert nicht

```bash
# 1. KEDA Version prüfen
helm list -n keda
kubectl get pods -n keda

# 2. ScaledObject Status (KEDA v2 zeigt fallback, activationThreshold)
kubectl describe scaledobject ilivalidator-worker-scaler -n ilivalidator

# 3. HPA Status (von KEDA erstellt)
kubectl get hpa -n ilivalidator
kubectl describe hpa keda-hpa-ilivalidator-worker -n ilivalidator

# 4. Metrics verfügbar?
kubectl exec -it deployment/ilivalidator-frontend -n ilivalidator -- \
  curl localhost:8080/actuator/prometheus | grep jobrunr_pending_jobs

# 5. Prometheus Query testen
kubectl port-forward -n ilivalidator svc/prometheus 9090:9090
# Browser: http://localhost:9090 → Query: jobrunr_pending_jobs

# 6. KEDA kann Prometheus erreichen?
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -n ilivalidator -- \
  curl http://prometheus.ilivalidator.svc.cluster.local:9090/api/v1/query?query=jobrunr_pending_jobs

# 7. KEDA Operator Logs (KEDA v2 zeigt detaillierte Trigger-Events)
kubectl logs -n keda deployment/keda-operator --tail=100 -f

# 8. KEDA Metrics Server Logs
kubectl logs -n keda deployment/keda-metrics-apiserver --tail=100

# 9. KEDA v2 Events prüfen
kubectl get events -n ilivalidator --field-selector involvedObject.name=ilivalidator-worker-scaler
```

**Häufige KEDA v2 Probleme:**
- `activationThreshold: "0"` verhindert Scale-to-Zero → auf `"0.5"` ändern
- Prometheus nicht erreichbar → Fallback-Replicas werden genutzt
- `ignoreNullValues: false` → Scale-down bei fehlenden Metriken

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

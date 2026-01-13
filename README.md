[![CI/CD](https://github.com/edigonzales/ilivalidator-web-service/actions/workflows/main.yml/badge.svg)](https://github.com/edigonzales/ilivalidator-web-service/actions/workflows/main.yml)

# ilivalidator-web-service

Spring Boot Webservice für die Validierung von INTERLIS-Transferdateien mit [ilivalidator](https://github.com/claeis/ilivalidator).

## Features

- ✅ Validierung von INTERLIS 1+2 Transferdateien
- ✅ Remote Config Files für Validierungs-Profile
- ✅ Multi-File Upload
- ✅ REST API
- ✅ Clustering für horizontale Skalierung
- ✅ Prometheus Metrics für Monitoring & Autoscaling
- ✅ Jobrunr Dashboard für Job-Management
- ✅ KEDA Scale-to-Zero Support

## Quick Start

### Lokal mit Spring Boot

```bash
./mvnw spring-boot:run -pl *-server -am -Penv-dev
```

**URLs:**
- App: http://localhost:8080
- Dashboard: http://localhost:8000/dashboard (admin/admin)
- Metrics: http://localhost:8080/actuator/prometheus

### Docker Compose

```bash
# Mit offiziellem Image
docker-compose up -d

# Worker skalieren
docker-compose up -d --scale worker=5

# Logs
docker-compose logs -f

# Stop
docker-compose down
```

### Kubernetes

```bash
# Standard Deployment
kubectl apply -f k8s/deployment-base.yaml

# Mit KEDA Autoscaling (Scale-to-Zero)
kubectl apply -f k8s/deployment-keda.yaml
```

Siehe [k8s/README.md](k8s/README.md) für Details.

## Dokumentation

- **GUI:** [docs/user-manual-de.md](docs/user-manual-de.md)
- **REST-API:** [docs/rest-api-de.md](docs/rest-api-de.md)
- **Nutzungsplanung:** [docs/user-manual-de-nplso.md](docs/user-manual-de-nplso.md)
- **Kubernetes:** [k8s/README.md](k8s/README.md)

## Entwicklung

### Option 1: Spring Boot (Schnellste Iteration)

```bash
# Backend
./mvnw spring-boot:run -pl *-server -am -Penv-dev

# Frontend Hot Reload (optional)
./mvnw gwt:codeserver -pl *-client -am -nsu
```

### Option 2: Docker Compose (Realistisches Setup)

```bash
# Build
./mvnw clean package -DskipTests -Penv-prod

# Start
docker-compose -f docker-compose.local.yaml up -d

# Rebuild bei Code-Änderungen
./mvnw package -DskipTests -Penv-prod -pl ilivalidator-web-service-server
docker-compose -f docker-compose.local.yaml up -d --build
```

### Build

```bash
# Standard Build (mit GWT-Client)
./mvnw clean package -DskipTests -Penv-prod

# Production Build (ohne Tests)
./mvnw clean package -DskipTests -Penv-prod -DexcludedGroups="docker"

# Tests
./mvnw -Penv-test clean test -DexcludedGroups="docker"

# Docker Tests
./mvnw -Penv-test clean test -Dgroups="docker"

# Einzelner Test
./mvnw -Penv-test test -Dtest=SpringJobControllerTests#validate_File_Interlis2_Ok
```

**Maven Profile:**
- `-Penv-prod` - Baut GWT-Client und packt JavaScript-Dateien ins Server-JAR (erforderlich für Docker/Production)
- `-Penv-dev` - Überspringt GWT-Build für schnellere Iteration (nur für `spring-boot:run`)
- `-Penv-test` - Baut Test-Docker-Images

## Deployment

### Docker

#### Einzelner Container

```bash
docker run -p 8080:8080 -p 8000:8000 \
  -v $(pwd)/work:/work \
  sogis/ilivalidator-web-service:3
```

#### Docker Compose (Clustering)

```bash
# Production Setup
docker-compose up -d

# Development (Complete Build from Source)
docker-compose -f docker-compose.dev.yaml up -d --build

# Fast Iteration (mit vorgebautem JAR)
./mvnw clean package -DskipTests -Penv-prod
docker-compose -f docker-compose.local.yaml up -d
```

**Verfügbare docker-compose Files:**
- `docker-compose.yaml` - Production mit offiziellem Image
- `docker-compose.dev.yaml` - Complete Build from Source (~5 Min)
- `docker-compose.local.yaml` - Schnelle Iteration mit JAR (~30s)

### Kubernetes

```bash
# Standard Deployment (feste Worker-Anzahl)
kubectl apply -f k8s/deployment-base.yaml

# KEDA Autoscaling (Scale-to-Zero)
helm install keda kedacore/keda --namespace keda --create-namespace
kubectl apply -f k8s/deployment-keda.yaml

# Status
kubectl get pods -n ilivalidator
kubectl get scaledobject -n ilivalidator

# Logs
kubectl logs -f deployment/ilivalidator-frontend -n ilivalidator

# Port-Forward
kubectl port-forward -n ilivalidator svc/ilivalidator-frontend 8080:8080 8000:8000
```

Details siehe [k8s/README.md](k8s/README.md).

### k3d (Lokales Kubernetes)

```bash
# Cluster erstellen
k3d cluster create ilivalidator-dev \
  --port "8080:30080@server:0" \
  --port "8000:30800@server:0"

# Image bauen und laden
./mvnw clean package -DskipTests -Penv-prod
docker build -f Dockerfile.local -t ilivalidator-web-service:local .
k3d image import ilivalidator-web-service:local -c ilivalidator-dev

# Deploy
kubectl apply -f k8s/deployment-base.yaml

# Cleanup
k3d cluster delete ilivalidator-dev
```

## Konfiguration

### Umgebungsvariablen

#### Wichtigste Settings

| Variable | Frontend | Worker | Beschreibung | Default |
|----------|----------|--------|--------------|---------|
| `JOBRUNR_SERVER_ENABLED` | `false` | `true` | Background Job Server aktivieren | `true` |
| `REST_API_ENABLED` | `true` | `false` | REST API aktivieren | `true` |
| `JOBRUNR_DASHBOARD_ENABLED` | `true` | `false` | Dashboard aktivieren | `true` |
| `CLEANER_ENABLED` | `true` | `false` | Alte Files automatisch löschen | `true` |
| `MAX_FILE_SIZE` | `200` | `200` | Max Upload Size (MB) | `200` |
| `JOBRUNR_POLL_INTERVAL` | - | `5` | Job Polling Intervall (Sekunden) | `10` |
| `JOBRUNR_WORKER_COUNT` | - | `1` | Jobs pro Worker | `1` |

#### Alle Optionen

| Name | Beschreibung | Standard |
|------|--------------|----------|
| `TZ` | Zeitzone | - |
| `LOG_LEVEL` | Spring Boot Logging Level | `INFO` |
| `LOG_LEVEL_APPLICATION` | Application Logging Level | `DEBUG` |
| `CONNECT_TIMEOUT` | Connection Timeout (ms) | `5000` |
| `READ_TIMEOUT` | Read Timeout (ms) | `5000` |
| `WORK_DIRECTORY` | Verzeichnis für Uploads/Logs | `/work/` |
| `FOLDER_PREFIX` | Prefix für temp. Verzeichnisse | `ilivalidatorws_` |
| `JDBC_URL` | SQLite/PostgreSQL Connection String | `jdbc:sqlite:/work/jobrunr_db.sqlite` |
| `TOMCAT_THREADS_MAX` | Max Tomcat Threads | `20` |
| `TOMCAT_ACCEPT_COUNT` | Request Queue Size | `100` |
| `TOMCAT_MAX_CONNECTIONS` | Max Connections | `500` |
| `HIKARI_MAX_POOL_SIZE` | DB Connection Pool Size | `10` |
| `ILIDIRS` | INTERLIS Model Repositories | `https://geo.so.ch/models;...` |

### Clustering Setup

Für Multi-Container Deployments:

**Frontend (eine Instanz):**
- `JOBRUNR_SERVER_ENABLED=false` - Führt keine Jobs aus
- `REST_API_ENABLED=true` - Nimmt Requests entgegen
- `JOBRUNR_DASHBOARD_ENABLED=true` - Dashboard verfügbar
- `CLEANER_ENABLED=true` - Räumt alte Files auf

**Worker (mehrere Instanzen):**
- `JOBRUNR_SERVER_ENABLED=true` - Führt Jobs aus
- `REST_API_ENABLED=false` - Kein API
- `JOBRUNR_DASHBOARD_ENABLED=false` - Kein Dashboard
- `CLEANER_ENABLED=false` - Nur Frontend räumt auf

**Shared Storage:** Alle Container benötigen Zugriff auf `/work/` für SQLite DB und Uploads.

## Monitoring

### Prometheus Metrics

Die App exponiert Jobrunr-Metriken unter `/actuator/prometheus`:

```bash
curl http://localhost:8080/actuator/prometheus | grep jobrunr
```

**Verfügbare Metrics:**
- `jobrunr_pending_jobs` - Wartende Jobs (SCHEDULED + ENQUEUED)
- `jobrunr_processing_jobs` - Aktuell verarbeitende Jobs
- `jobrunr_succeeded_jobs` - Erfolgreich abgeschlossene Jobs
- `jobrunr_failed_jobs` - Fehlgeschlagene Jobs

Diese Metriken können für:
- **Monitoring** mit Prometheus/Grafana genutzt werden
- **Autoscaling** mit KEDA für Scale-to-Zero

### Health Checks

```bash
# Liveness
curl http://localhost:8080/actuator/health/liveness

# Readiness
curl http://localhost:8080/actuator/health/readiness

# Full Health
curl http://localhost:8080/actuator/health
```

### Jobrunr Dashboard

Das Dashboard zeigt Job-Status, Statistiken und Server-Metriken:

```bash
# Lokal
open http://localhost:8000/dashboard

# Kubernetes
kubectl port-forward -n ilivalidator svc/ilivalidator-frontend 8000:8000
open http://localhost:8000/dashboard

# Login: admin / admin
```

## REST API

### Job hochladen

```bash
curl -X POST http://localhost:8080/api/jobs \
  -F "files=@/path/to/file.xtf" \
  -F "profile=Nutzungsplanung"
```

**Response:**
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000"
}
```

### Job Status abfragen

```bash
curl http://localhost:8080/api/jobs/{jobId}
```

**Response:**
```json
{
  "createdAt": "2024-01-13T10:00:00",
  "updatedAt": "2024-01-13T10:00:30",
  "jobStatus": "SUCCEEDED",
  "validationResult": "SUCCEEDED",
  "logFile": "http://localhost:8080/api/logs/ilivalidatorws_xxx/xxx.log",
  "xtfLogFile": "http://localhost:8080/api/logs/ilivalidatorws_xxx/xxx.log.xtf",
  "csvLogFile": "http://localhost:8080/api/logs/ilivalidatorws_xxx/xxx.log.csv"
}
```

Details siehe [docs/rest-api-de.md](docs/rest-api-de.md).

## Architektur

### Komponenten

```
┌─────────────────┐
│   Frontend      │ ← REST API, Dashboard, Metrics
│   Container     │ ← Port 8080, 8000
└────────┬────────┘
         │
         ├──────────┐
         │          │
┌────────▼────┐ ┌──▼─────────┐
│  Worker 1   │ │  Worker N  │ ← Job Processing
└─────────────┘ └────────────┘
         │          │
         └────┬─────┘
              │
       ┌──────▼──────┐
       │   SQLite    │ ← Job Queue (shared)
       │ /work/*.db  │
       └─────────────┘
              │
       ┌──────▼──────┐
       │ Shared PVC  │ ← Upload/Log Files
       │   /work/    │
       └─────────────┘
```

### KEDA Autoscaling

KEDA nutzt die `jobrunr_pending_jobs` Metric für automatisches Scaling:

```yaml
triggers:
- type: prometheus
  metadata:
    query: jobrunr_pending_jobs
    threshold: "5"  # 1 Worker pro 5 Jobs
    activationThreshold: "0"  # Scale to zero bei 0 Jobs
```

**Vorteile:**
- ✅ Automatisches Scale-to-Zero bei Idle
- ✅ Automatisches Wake-up bei neuen Jobs
- ✅ Keine Kosten für ungenutzte Worker
- ✅ Funktioniert mit SQLite (kein PostgreSQL nötig)

## Troubleshooting

### Port bereits belegt

```bash
# Prozess finden
lsof -i :8080
lsof -i :8000

# Docker Container stoppen
docker-compose down
```

### KEDA Worker skalieren nicht

```bash
# KEDA Status prüfen
kubectl describe scaledobject ilivalidator-worker-scaler -n ilivalidator

# Metrics prüfen
kubectl exec -it deployment/ilivalidator-frontend -n ilivalidator -- \
  curl localhost:8080/actuator/prometheus | grep jobrunr_pending_jobs

# KEDA Operator Logs
kubectl logs -n keda deployment/keda-operator --tail=50
```

### Jobs werden nicht verarbeitet

```bash
# Worker Logs prüfen (Docker)
docker-compose logs worker | grep -i error

# Worker Logs prüfen (Kubernetes)
kubectl logs -f deployment/ilivalidator-worker -n ilivalidator

# SQLite DB prüfen
docker exec -it ilivalidator-frontend \
  sqlite3 /work/jobrunr_db.sqlite \
  "SELECT id, state FROM jobrunr_jobs ORDER BY createdAt DESC LIMIT 10;"

# Dashboard prüfen
open http://localhost:8000/dashboard
```

### Shared Volume funktioniert nicht (Kubernetes)

```bash
# PVC Status prüfen
kubectl get pvc -n ilivalidator
kubectl describe pvc ilivalidator-work-pvc -n ilivalidator

# Falls ReadWriteMany nicht unterstützt wird:
# - NFS Server aufsetzen
# - Anderen RWX Storage Provider nutzen
# - Auf PostgreSQL statt SQLite wechseln
```

## Performance Tuning

### Worker Resources

**Docker Compose:**
```yaml
worker:
  deploy:
    resources:
      limits:
        cpus: '4.0'
        memory: 8G
```

**Kubernetes:**
```yaml
resources:
  limits:
    memory: "8Gi"
    cpu: "4000m"
```

### KEDA Scaling

```yaml
# k8s/deployment-keda.yaml
spec:
  minReplicaCount: 0
  maxReplicaCount: 50  # Mehr Worker erlauben
  pollingInterval: 10   # Schneller reagieren

  triggers:
  - type: prometheus
    metadata:
      threshold: "2"  # Aggressiveres Scaling
```

### Jobrunr Settings

```yaml
env:
- name: JOBRUNR_POLL_INTERVAL
  value: "3"  # Schnelleres Polling
- name: HIKARI_MAX_POOL_SIZE
  value: "20"  # Größerer Connection Pool
```

## Interne Struktur

- **Spring Boot 3.3.4** - Framework
- **GWT 2.11** - Web UI
- **Jobrunr 6.3.4** - Job Queue & Scheduling
- **ilivalidator 1.14.3** - INTERLIS Validation
- **SQLite** - Default Job Storage (PostgreSQL möglich)
- **Micrometer + Prometheus** - Metrics

### Module

- `ilivalidator-web-service-shared` - Shared DTOs
- `ilivalidator-web-service-client` - GWT Web UI
- `ilivalidator-web-service-server` - Spring Boot Backend

### Custom Functions

Custom-Funktionen werden als normale Maven Dependencies definiert und via System Property registriert (siehe `Application.java`).

## Migration & Updates

### Image Update

```bash
# Docker Compose
docker-compose pull
docker-compose up -d

# Kubernetes Rolling Update
kubectl set image deployment/ilivalidator-frontend \
  ilivalidator=sogis/ilivalidator-web-service:3.1.0 \
  -n ilivalidator

kubectl rollout status deployment/ilivalidator-frontend -n ilivalidator
```

### Config Update

**Docker Compose:**
```bash
# docker-compose.yaml anpassen
vim docker-compose.yaml

# Neu starten
docker-compose up -d
```

**Kubernetes:**
```bash
# ConfigMap anpassen
kubectl edit configmap ilivalidator-config -n ilivalidator

# Pods neu starten
kubectl rollout restart deployment/ilivalidator-frontend -n ilivalidator
kubectl rollout restart deployment/ilivalidator-worker -n ilivalidator
```

## Sicherheit

### Credentials ändern

**Docker Compose:**
```yaml
environment:
  JOBRUNR_DASHBOARD_USER: "your-username"
  JOBRUNR_DASHBOARD_PWD: "secure-password"
```

**Kubernetes Secrets:**
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
```

## Externe Abhängigkeiten

- INTERLIS Modell-Repositories (geo.so.ch, models.interlis.ch, models.geo.admin.ch)
- Validierungs-Profile (ilidata-Repositories)

## Lizenz

Siehe [LICENSE](LICENSE)

## Support

- **Issues:** https://github.com/edigonzales/ilivalidator-web-service/issues
- **Documentation:** [docs/](docs/)
- **Kubernetes:** [k8s/README.md](k8s/README.md)

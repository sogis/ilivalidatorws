[![CI/CD](https://github.com/sogis/ilivalidatorws/actions/workflows/main.yml/badge.svg)](https://github.com/sogis/ilivalidatorws/actions/workflows/main.yml)

# ilivalidator-web-service

Spring Boot Webservice für die Validierung von INTERLIS-Transferdateien mit [ilivalidator](https://github.com/claeis/ilivalidator).

## Features

- Validierung von INTERLIS 1+2 Transferdateien
- Remote Config Files für Validierungs-Profile
- Multi-File Upload
- REST API
- Clustering für horizontale Skalierung

## Deployment

### Standalone

```bash
java -jar ilivalidator-web-service-server/target/ilivalidator-web-service-server-*.jar
```

### Docker

```bash
docker run -p 8080:8080 sogis/ilivalidator-web-service:3
```

### Docker Compose (Clustering)

```bash
docker-compose up -d

# Worker skalieren
docker-compose up -d --scale worker=3
```

Siehe [docker-compose.yaml](docker-compose.yaml) für Details.

### Kubernetes

```bash
# Standard Deployment
kubectl apply -f k8s/deployment-base.yaml

# Mit KEDA Autoscaling (Scale-to-Zero)
kubectl apply -f k8s/deployment-keda.yaml
```

## Entwicklung

### Option 1: Lokal mit Maven (Schnellste Iteration)

Für schnelle Entwicklungszyklen ohne Docker:

```bash
# Backend starten
./mvnw spring-boot:run -pl ilivalidator-web-service-server -am -Penv-dev
```

**URLs:**
- App: http://localhost:8080
- Dashboard: http://localhost:8000/dashboard (admin/admin)
- Metrics: http://localhost:8080/actuator/prometheus

**Frontend Hot Reload (optional):**
```bash
# In separatem Terminal
./mvnw gwt:codeserver -pl ilivalidator-web-service-client -am -nsu
```

**Vorteile:**
- Sehr schnelle Iteration
- Kein Docker Build nötig
- Hot Reload für Backend (DevTools)
- GWT Super Dev Mode für Frontend

**Nachteile:**
- Kein Clustering
- Lokale SQLite DB in `/tmp`

---

### Option 2: Lokal mit Docker Compose (Realistisches Setup)

Für realistische Tests mit Clustering und Worker-Pods:

#### 2a) Mit vorgebautem JAR (schnell)

```bash
# 1. JAR bauen
./mvnw clean package -DskipTests -Penv-prod

# 2. Docker Image bauen
docker build -f Dockerfile.local -t ilivalidator-web-service:local .

# 3. Starten
docker-compose -f docker-compose.local.yaml up -d

# 4. Logs verfolgen
docker-compose -f docker-compose.local.yaml logs -f

# 5. Worker skalieren
docker-compose -f docker-compose.local.yaml up -d --scale worker=3
```

**Rebuild bei Code-Änderungen:**
```bash
# Schneller Rebuild (nur Server-Modul)
./mvnw package -DskipTests -Penv-prod -pl ilivalidator-web-service-server

# Image neu bauen
docker build -f Dockerfile.local -t ilivalidator-web-service:local .

# Container neu starten
docker-compose -f docker-compose.local.yaml up -d
```

**Build-Zeit:** ~30-60 Sekunden

#### 2b) Complete Build from Source (vollständig)

```bash
# 1. Docker Image bauen (inkl. Maven Build im Container)
docker-compose -f docker-compose.dev.yaml up -d --build

# 2. Logs verfolgen
docker-compose -f docker-compose.dev.yaml logs -f
```

**Build-Zeit:** ~5 Minuten (beim ersten Mal, danach mit Cache schneller)

**Verfügbare docker-compose Files:**
- `docker-compose.yaml` - Production mit offiziellem Image `sogis/ilivalidator-web-service:3`
- `docker-compose.local.yaml` - Development mit lokalem Build (`Dockerfile.local`)
- `docker-compose.dev.yaml` - Development mit Complete Build (`Dockerfile.dev`)

**URLs:**
- App: http://localhost:8080
- Dashboard: http://localhost:8000/dashboard (admin/admin)

**Vorteile:**
- Realistische Produktionsumgebung
- Clustering mit Frontend + Worker
- Shared Volume für SQLite DB
- Worker-Skalierung testbar

**Nachteile:**
- Längere Build-Zeiten
- Mehr Resource-Verbrauch

**Cleanup:**
```bash
docker-compose -f docker-compose.local.yaml down -v
```

---

### Option 3: Lokal mit k3d (Kubernetes)
Für lokale Kubernetes-Tests mit/ohne KEDA Autoscaling:

> **k3d** https://k3d.io/
>
> k3d is a lightweight wrapper to run k3s (Rancher Lab’s minimal Kubernetes distribution) in docker.


#### 1. k3d Cluster erstellen

```bash
# Cluster mit Port-Forwarding
k3d cluster create ilivalidator-dev \
  --port "8080:30080@server:0" \
  --port "8000:30800@server:0"
```

**Port-Mapping:**
- `30080` (Kubernetes NodePort) → `8080` (localhost) → App
- `30800` (Kubernetes NodePort) → `8000` (localhost) → Dashboard

#### 2. Image bauen und in Cluster laden

```bash
# JAR bauen
./mvnw clean package -DskipTests -Penv-prod

# Docker Image bauen
docker build -f Dockerfile.local -t ilivalidator-web-service:local .

# Image in k3d Cluster importieren
k3d image import ilivalidator-web-service:local -c ilivalidator-dev
```

#### 3. Deployment

**Standard Deployment (feste Worker-Anzahl):**
```bash
kubectl apply -f k8s/deployment-base.yaml

# Status prüfen
kubectl get pods -n ilivalidator
kubectl get svc -n ilivalidator

# Logs
kubectl logs -f deployment/ilivalidator-frontend -n ilivalidator
```

**KEDA Autoscaling (Scale-to-Zero):**
```bash
# 1a) KEDA installieren ohne Helm
kubectl apply -f https://github.com/kedacore/keda/releases/download/v2.18.3/keda-2.18.3.yaml
# ODER 
# 1b) KEDA installieren mit Helm
helm repo add kedacore https://kedacore.github.io/charts
helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# 2) KEDA ScaledJob CRD fix (für KEDA v2.18.3)
curl -sL https://raw.githubusercontent.com/kedacore/keda/v2.18.3/config/crd/bases/keda.sh_scaledjobs.yaml | \
  kubectl apply --server-side=true -f -

# 3) Deployment mit KEDA
kubectl apply -f k8s/deployment-keda.yaml

# KEDA Status
kubectl get scaledobject -n ilivalidator
kubectl describe scaledobject ilivalidator-worker-scaler -n ilivalidator
```

#### 4. Zugriff

```bash
# Via NodePort (automatisch durch k3d Port-Mapping)
open http://localhost:8080
open http://localhost:8000/dashboard

# Oder via Port-Forward
kubectl port-forward -n ilivalidator svc/ilivalidator-frontend 8080:8080 8000:8000
```

#### 5. Image Update

```bash
# Code ändern und neu bauen
./mvnw package -DskipTests -Penv-prod -pl ilivalidator-web-service-server
docker build -f Dockerfile.local -t ilivalidator-web-service:local .

# Image neu laden
k3d image import ilivalidator-web-service:local -c ilivalidator-dev

# Pods neu starten
kubectl rollout restart deployment/ilivalidator-frontend -n ilivalidator
kubectl rollout restart deployment/ilivalidator-worker -n ilivalidator

# Status
kubectl rollout status deployment/ilivalidator-frontend -n ilivalidator
```

#### 6. Debugging

```bash
# Pod Details
kubectl describe pod -n ilivalidator -l app=ilivalidator

# Logs aller Worker
kubectl logs -n ilivalidator -l component=worker --tail=50

# Exec in Pod
kubectl exec -it -n ilivalidator deployment/ilivalidator-frontend -- /bin/sh

# PVC Status
kubectl get pvc -n ilivalidator
kubectl describe pvc ilivalidator-work-pvc -n ilivalidator
```

#### 7. Cleanup

```bash
# Deployment löschen
kubectl delete namespace ilivalidator

# KEDA entfernen (optional)
helm uninstall keda -n keda
kubectl delete namespace keda

# Cluster löschen
k3d cluster delete ilivalidator-dev
```

**Vorteile:**
- Vollständige Kubernetes-Umgebung lokal
- KEDA Autoscaling testbar
- PersistentVolumes, ConfigMaps, Secrets testbar
- Rolling Updates testbar

**Nachteile:**
- Längere Setup-Zeit
- Höherer Resource-Verbrauch
- Komplexere Debugging-Workflows

---

## Build & Tests

### Standard Build

```bash
# Production Build (mit GWT-Client)
./mvnw clean package -DskipTests -Penv-prod

# JAR Location
# ilivalidator-web-service-server/target/ilivalidator-web-service-server-*.jar
```

### Tests

```bash
# Unit Tests
./mvnw -Penv-test clean test -DexcludedGroups="docker"

# Docker Tests (erfordert Docker)
./mvnw -Penv-test clean test -Dgroups="docker"

# Einzelner Test
./mvnw -Penv-test test -Dtest=SpringJobControllerTests#validate_File_Interlis2_Ok
```

**Maven Profile:**
- `-Penv-prod` - Baut GWT-Client und packt ins Server-JAR (erforderlich für Docker/Production)
- `-Penv-dev` - Überspringt GWT-Build für schnelle Iteration (nur für `spring-boot:run`)
- `-Penv-test` - Baut Test-Docker-Images

## Konfiguration

### Umgebungsvariablen

| Name | Beschreibung | Standard |
|------|--------------|----------|
| `MAX_FILE_SIZE` | Max Upload Size (MB) | `200` |
| `WORK_DIRECTORY` | Verzeichnis für Uploads/Logs | `/work/` |
| `JOBRUNR_SERVER_ENABLED` | Background Job Server aktivieren | `true` |
| `JOBRUNR_WORKER_COUNT` | Jobs pro Worker | `1` |
| `JOBRUNR_DASHBOARD_ENABLED` | Dashboard aktivieren | `true` |
| `JOBRUNR_DASHBOARD_USER` | Dashboard Username | `admin` |
| `JOBRUNR_DASHBOARD_PWD` | Dashboard Password | `admin` |
| `REST_API_ENABLED` | REST API aktivieren | `true` |
| `CLEANER_ENABLED` | Alte Files automatisch löschen | `true` |
| `ILIDIRS` | INTERLIS Model Repositories | `https://geo.so.ch/models;https://models.interlis.ch;...` |
| `JDBC_URL` | SQLite/PostgreSQL Connection String | `jdbc:sqlite:/work/jobrunr_db.sqlite` |

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

### Module

- `ilivalidator-web-service-shared` - Shared DTOs
- `ilivalidator-web-service-client` - GWT Web UI
- `ilivalidator-web-service-server` - Spring Boot Backend

### Technologien

- **Spring Boot 3.3.4** - Framework
- **GWT 2.11** - Web UI
- **Jobrunr 6.3.4** - Job Queue & Scheduling
- **ilivalidator 1.14.3** - INTERLIS Validation
- **SQLite** - Default Job Storage (PostgreSQL möglich)
- **Micrometer + Prometheus** - Metrics

## Dokumentation

- **GUI:** [docs/user-manual-de.md](docs/user-manual-de.md)
- **REST-API:** [docs/rest-api-de.md](docs/rest-api-de.md)
- **Nutzungsplanung:** [docs/user-manual-de-nplso.md](docs/user-manual-de-nplso.md)

## Lizenz

Siehe [LICENSE](LICENSE)

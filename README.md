[![CI/CD](https://github.com/edigonzales/ilivalidator-web-service/actions/workflows/main.yml/badge.svg)](https://github.com/edigonzales/ilivalidator-web-service/actions/workflows/main.yml)

# ilivalidator-web-service

The ilivalidator web service is a [Spring Boot](https://projects.spring.io/spring-boot/) application and uses [ilivalidator](https://github.com/claeis/ilivalidator) for the INTERLIS transfer file validation.

## Beschreibung

* checks INTERLIS 1+2 transfer files: see [https://github.com/claeis/ilivalidator](https://github.com/claeis/ilivalidator) for all the INTERLIS validation magic of ilivalidator
* uses remote config files for validation tailoring
* user can upload multiple transfer files at once
* REST-API
* simple clustering for horizontal scaling

## Anleitungen

Der Benutzer wählt eine oder mehrere INTERLIS-Transferdateien aus und lädt sie hoch. Im Browser erscheint nach erfolgter Prüfung das Resultat und Links zu den Logdateien. Es stehen verschiedene Prüfprofile zur Verfügung, welche die Validierung zusätzlich konfigurieren (z.B. Warnung statt Error, zusätzliche Constraints, etc.).

Weitere Informationen:

- GUI: [docs/user-manual-de.md](docs/user-manual-de.md)
- Nutzungsplanung: [docs/user-manual-de-nplso.md](docs/user-manual-de-nplso.md)
- REST-API: [docs/rest-api-de.md](docs/rest-api-de.md)

## Komponenten

Die Anwendung besteht aus einer Komponente. Wird ein Datenbankserver für das Speichern der Jobqueue verwendet, gehört das Schema etc. auch als Komponente zur Anwendung. Standardmässig wird eine SQlite-Datenbank verwendet, welche automatisch erstellt wird, falls sie nicht vorhanden ist.

Die Prüfprofile (aka Zusatzkonfiguration) sind nicht Bestandteil der Komponente, sondern sie liegen in eigenen oder fremden ilidata-Repositories.

## Konfigurieren und Starten

Die Anwendung kann wie folgt gestartet werden:

```
java -jar ilivalidator-web-service-server/target/ilivalidator-web-service.jar 
```

respektive mit Docker:

```
docker run -p 8080:8080 sogis/ilivalidator-web-service
```

Konfiguration via _application.properties_ im Verzeichnis in dem der Service gestartet wird. Oder entsprechende alternative Konfigurationsmöglichkeiten von [Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).

Die Anwendung beinhaltet bereits eine _application.properties_-Datei. Siehe [application.properties](src/main/resources/application.properties), welche beim obigen Aufruf standardmässig verwendet wird.

Der Dockercontainer verwendet eine leicht angepasste Konfiguration ([application-docker.properties](src/main/resources/application-docker.properties)), damit das Mounten von Verzeichnissen hoffentlich einfacher fällt und weniger Fehler passieren:

- Die SQLite-Datenbank wird im _/work_-Verzeichnis angelegt, das im Dockerimage angelegt wird.
- Für `WORK_DIRECTORY` wird das _/work_-Verzeichnis verwendet, das im Dockerimage angelegt wird. 

Die allermeisten Optionen sind via Umgebungsvariablen exponiert und somit veränderbar. Im Extremfall kann immer noch ein neues Dockerimage erstellt werden mit einer ganz eigenen Konfiguration.

### Optionen (Umgebungsvariablen)

Mit Docker wird die Anwendung mit einem docker-Profil gestartet (siehe Dockerfile). Standardwerte gemäss diesem application-docker.properties:

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `MAX_FILE_SIZE` | Die maximale Grösse einer Datei, die hochgeladen werden kann in Megabyte. | `200` |
| `LOG_LEVEL` | Das Logging-Level des Spring Boot Frameworks. | `INFO` |
| `LOG_LEVEL_DB_CONNECTION_POOL` | Das Logging-Level des DB-Connection-Poolsocket. | `INFO` |
| `LOG_LEVEL_APPLICATION` | Das Logging-Level der Anwendung (= selber geschriebener Code). | `DEBUG` |
| `CONNECT_TIMEOUT` | Die Zeit in Millisekunden, die bis zu einem erfolgreichem Connect gewartet wird. Betrifft sämtliche Methoden, welche `sun.net.client.defaultConnectTimeout` berücksichtigen. Die Option dient dazu damit langsame INTERLIS-Modellablage schneller zu einem Timeout führen. | `5000` |
| `READ_TIMEOUT` | Die Zeit in Millisekunden, die bis zu einem erfolgreichem Lesen gewartet wird. Betrifft sämtliche Methoden, welche `sun.net.client.defaultConnectTimeout` berücksichtigen. Die Option dient dazu damit langsame INTERLIS-Modellablage schneller zu einem Timeout führen. | `5000` |
| `WORK_DIRECTORY` | Verzeichnis, in das die zu prüfenden INTERLIS-Transferdatei und die Logdateien kopiert werden (in ein temporäres Unterverzeichnis, das im `WORK_DIRECTORY` erstellt wird). Falls `local` Storage Service gewählt ist, muss das Verzeichnis, bei einem Betrieb mit mehreren Containern, zwingend geteilt werden muss. Sonst ist nicht sichergestellt, dass man die Logdatei(en) herunterladen kann. Falls `s3` Storage Service gewählt ist, muss der Name des Buckets gewählt werden, in den die Daten kopiert werden. | `/work/` |
| `FOLDER_PREFIX` | Für jede zu prüfende Datei wird im `WORK_DIRECTORY`-Verzeichnis ein temporäres Verzeichnis erstellt. Der Prefix wird dem Namen des temporären Verzeichnisses vorangestellt. | `ilivalidatorws_` |
| `CLEANER_ENABLED` | Dient zum Ein- und Ausschalten des Aufräumprozesses, der alte, geprüfte Dateien (INTERLIS-Transferdateien, Logfiles) löscht. | `true` |
| `REST_API_ENABLED` | Dient zum Ein- und Ausschalten des REST-API-Controllers und damit der eigentlichen Funktionalität (auch wenn Jobrunr trotzdem initialisiert wird). | `true` |
| `JDBC_URL` | Die JDBC-Url der Sqlite-Datei, die dem Speichern der Jobs dient, welche mittels REST-API getriggert wurden. Die Datei wird im Standard-`WORK`-Verzeichnis gespeichert, da dieses beim Multi-Container-Betrieb geteilt werden muss. Andere JDBC-fähige Datenbanken sind ebenfalls möglich. Dann müssten noch mindestens Login und Password als Option exponiert werden. Und die Anwendung müsste neu mit dem dazugehörigen JDBC-Treiber gebuildet werden. | `jdbc:sqlite:/work/jobrunr_db.sqlite` |
| `JOBRUNR_SERVER_ENABLED` | Dient die Instanz als sogenannter Background-Jobserver, d.h. werden mittels REST-API hochgeladene INTERLIS-Transferdateien validiert. Wird nur eine Instanz betrieben, muss die Option zwingen `true` sein, da sonst der Job nicht ausgeführt wird. | `true` |
| `JOBRUNR_POLL_INTERVAL` | Es wird im Intervall (in Sekunden) nach neuen Validierungsjobs geprüft. | `10` |
| `JOBRUNR_WORKER_COUNT` | Anzahl Jobs, die in einem "Worker" gleichzeitig durchgeführt werden können. Im Prinzip nicht sehr relevant, da der Validierungsjob synchronisiert ist (nicht thread safe). | `1` |
| `JOBRUNR_DASHBOARD_ENABLED` | Das Jobrunr-Dashboard wird auf dem Port 8000 gestartet. | `true` |
| `JOBRUNR_DASHBOARD_USER` | Username für Jobrunr-Dasboard. Achtung: Basic Authentication. | `admin` |
| `JOBRUNR_DASHBOARD_PWD` | Passwort für Jobrunr-Dasboard. Achtung: Basic Authentication. | `admin` |
| `TOMCAT_THREADS_MAX` | Maximale Anzahl Threads, welche die Anwendung gleichzeitig bearbeitet. | `20` |
| `TOMCAT_ACCEPT_COUNT` | Maximale Grösser der Queue, falls keine Threads mehr verfügbar. | `100` |
| `TOMCAT_MAX_CONNECTIONS` | Maximale Anzahl Threads des Servers. | `500` |
| `HIKARI_MAX_POOL_SIZE` | Grösse des DB-Connections-Pools | `10` |
| `ILIDIRS` | Modell- und Datenrepositorie, die verwendet werden sollen. | `https://geo.so.ch/models;https://models.interlis.ch;https://models.geo.admin.ch` |

Ein `docker-run`-Befehl könnte circa so aussehen:

```
docker run --rm -p8080:8080 -p8000:8000 -v /shared_storage/work:/work/ sogis/ilivalidator-web-service:3
```

Es werden zwei Ports gemapped. Der Port 8080 ist der Port der Anwendung und zwingend notwendig. Der Port 8000 dient dazu, dass das Jobrunr-Dashboard verfügbar ist.

Im lokalen Filesystem (oder Kubernetes-PV-Whatever etc.) muss das Verzeichnis _/shared_storage/work/_ vorhanden sein. Die SQLite-Datenbank, die dazu dient die (REST-API-)Jobs zu koordinieren, befindet sich im _/shared_storage/work/_-Verzeichnis. 

### Clean up

Ein Scheduler löscht jede halbe Stunde (momentan hardcodiert) alle temporären Verzeichnisse, die älter als 60x60 Sekunden sind.

### Ilivalidator custom functions

Custom-Funktionen können in zwei Varianten verwendet werden. Die Jar-Datei mit den Funktionen muss in einem Verzeichnis liegen und vor jeder Prüfung werden die Klassen dynamisch geladen. Das hat den Nachteil, dass man so kein Native-Image (GraalVM) mit Custom-Funktionen herstellen kann und man z.B. bei einem Webservice die Klassen nicht einfach als Dependency definierten kann, sondern die Jar-Datei muss in einem Verzeichnis liegen, welches beim Aufruf von _ilivalidator_ als Option übergeben wird. Bei der zweiten (neueren) Variante kann man die Custom-Funktionen als normale Dependency im Java-Projekt definieren. Zusätzlich müssen die einzelnen Klassen als Systemproperty der Anwendung bekannt gemacht werden. 

Im vorliegenden Fall wird die zweite Variante gewählt. Das notwendige Systemproperty wird in der `Application`-Klasse gesetzt. Falls man die erste Variante vorzieht oder aus anderen Gründen verwenden will, macht man z.B. ein Verzeichnis `src/main/resources/libs-ext/` und kopiert beim Builden die Jar-Datei in dieses Verzeichnis. Dazu wird eine Gradle-Konfiguration benötigt. Zur Laufzeit (also wenn geprüft wird) muss man die Jar-Datei auf das Filesystem kopieren und dieses Verzeichnis als Options _ilivalidator_ übergeben.

### Clustering

Sämtliche "Koordinationsaufgaben" wie z.B. das Entpacken der Config-Dateien, das Löschen von alten Files etc. sollte (und in einigen Fällen: darf) nur von einer Instanz ausgeführt werden. Als Beispiel eine einfache `docker-compose` Konfiguration:

```
version: '3'
services:
  frontend:
    image: sogis/ilivalidator-web-service:3
    restart: unless-stopped
    environment:
      TZ: Europe/Zurich
      JOBRUNR_SERVER_ENABLED: "false"
    ports:
      - 8080:8080
      - 8000:8000
    volumes:
      - type: volume
        source: work
        target: /work
  worker:
    image: sogis/ilivalidator-web-service:3
    restart: unless-stopped
    deploy:
      replicas: 2
    environment:
      TZ: Europe/Zurich
      JOBRUNR_DASHBOARD_ENABLED: "false"
      REST_API_ENABLED: "false"
      UNPACK_CONFIG_FILES: "false"
      CLEANER_ENABLED: "false"
    volumes:
      - type: volume
        source: work
        target: /work
volumes:
  docbase:
  work:
```

Es wird ein "frontend"-Service gestartet, welche als Schnittstelle gegen aussen dient. Es werden mehrere "worker"-Services gestartet (`replicas`), die nur für das Validieren einer INTERLIS-Transferdatei zuständig sind. Es sind keine Port exponiert, da keiner dieser Worker-Service von aussen verfügbar sein muss. Es werden verschiedene Applikationsfunktionen ausgeschaltet (sieh Umgebungsvariablen). Das Jobmanagement wird mit [Jobrunr](https://jobrunr.io) gemacht.

Achtung: Mit Docker Compose Version 3 kann im Nicht-Swarm-Mode keine CPU-Limits gesetzt werden.

## Externe Abhängigkeiten

Die Zusatzkonfigurationen - ini-Dateien für die Optionen --config und --metaConfig - müssen auf ilidata-Repositories liegen. Dies ist ein bewusster Entscheid (single source of truth). 

## Interne Struktur

TODO:
- spring boot / maven multimodule 
- GWT
- Jobrunr
- rest api
- Max file size: Stand heute an zwei Orten...
- Test und Dockertests
- Wie wird metaConfig etc. getestet? (Weiss ich noch nicht zu 100%: Idee neu wäre wohl mit kleinem lokalen Dockerimage mit ilidata.xml etc. Dann müsste preferred Ili Repo im Test noch anders gestetzt werden.)
- Registrierung Zusatzfunktionen
- --spring.profiles.active=docker
- ./mvnw versions:set -DnewVersion=3.0.1-SNAPSHOT -DprocessAllModules (noch nicht implementiert)
- git-commit-id-plugin -> inkl. Link: http://localhost:8080/actuator/info

## Entwicklung

### Run 

First Terminal:
```
./mvnw spring-boot:run -pl *-server -am -Penv-dev 
```

Second Terminal:
```
./mvnw gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:
```
./mvnw gwt:codeserver -pl *-client -am -nsu 
```

### Build

```
./mvnw -Penv-prod clean package -DexcludedGroups="docker"
```

In der Package-Phase werden die "Spring pur"-Tests durchgeführt und es wird am Ende ein Dockerimage für die Dockertests erstellt. Die definitiven Dockerimages werden wegen des Multi-Arch-Builds in der Pipeline erstellt und publiziert. Die Tests können auch separat ausgeführt werden.

Maven kennt Integrationtests in der Verify-Phase (nach Package). Wir verwenden jedoch nochmals eine separate Testphase, um die Dockertests durchzuführen (siehe nachfolgendes Kapitel).

### Tests

```
./mvnw -Penv-test clean test -DexcludedGroups="docker"
```

```
./mvnw -Penv-test clean test -Dgroups="docker"
```

Einzelner Test ausführen:

```
./mvnw -Penv-test test -Dtest=SpringJobControllerTests#validate_File_Interlis2_Ok -Dsurefire.failIfNoSpecifiedTests=false
```

### Lokales Modell- und Daten-Repository

Für die Durchführung der Tests wird ein INTERLIS-Modellrepository benötigt. Um zur Laufzeit der Tests nicht von fremden (dazu gehört auch unser eigenes) Repositories abhängig zu sein und Veränderungen in solchen (z.B. replaced Modelle, Änderungen in den ini-Konfigs), wird ein Dockerimage mit den für die Tests benötigten Modellen hergestellt. Die Modelle liegen im _*-server/src/test/docker/models_-Ordner. Die ilimodels.xml-Datei wird mit ilimanager hergestellt und sie muss im gleichen Ordner wie die Modelle zu liegen kommen (siehe Befehl unten). Das Dockerimage wird im Maven-Build erzeugt und in den Tests mit Testcontainers hochgefahren.

In Units, Time und CoordSys musste das Metaattribut "precursorVersion" gelöscht werden, weil es im Repo keine geben wird und ili2c solche Modell anschliessend ignoriert.

```
java -jar ilimanager-0.9.1.jar --createIliModels --repos models --out models/ilimodels.xml
```

Zusätzlich ist auch ein ilidata.xml notwendig für die ilivalidator-Konfigurationen (ini-Files). Die Datei wurde manuell angelegt und nachgeführt.

Docker image builden:

```
docker build -t sogis/interlis-repository-test .
```
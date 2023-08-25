[![CI/CD](https://github.com/sogis/ilivalidatorws/actions/workflows/main.yml/badge.svg)](https://github.com/sogis/ilivalidatorws/actions/workflows/main.yml)

# ilivalidatorws

The ilivalidator web service is a [Spring Boot](https://projects.spring.io/spring-boot/) application and uses [ilivalidator](https://github.com/claeis/ilivalidator) for the INTERLIS transfer file validation.

## Beschreibung

* checks INTERLIS 1+2 transfer files: see [https://github.com/claeis/ilivalidator](https://github.com/claeis/ilivalidator) for all the INTERLIS validation magic of ilivalidator
* checks CSV files
* uses server saved config files for validation tailoring
* user can deploy and/or upload own config files for validation tailoring
* user can upload multiple transfer files at once
* REST-API
* simple clustering for horizontal scaling

## Anleitungen

Folgende Einstellungen werden standardmässig verwendet:

- `allObjectsAccessible`: `true` (muss mit einer _ini_-Datei überschrieben werden)
- Es wird immer der _config/ili_-Ordner als zusätzlicher Model-Repo-Ordner verwendet.
- Es wird immer nach einer passenden _ini_-Datei im _config/ini_-Ordner gesucht.

In der Anwendung vorhandene _ili_- und _ini_-Dateien sind unter http://localhost:8080/config/ili resp. http://localhost:8080/config/ini einsehbar.

Weitere Informationen:

- GUI: [docs/user-manual-de.md](docs/user-manual-de.md)
- Nutzungsplanung: [docs/user-manual-de-nplso.md](docs/user-manual-de-nplso.md)
- REST-API: [docs/rest-api-de.md](docs/rest-api-de.md)

## Komponenten

Die Anwendung besteht aus einer Komponente. Wird ein Datenbankserver für das Speichern der Jobqueue verwendet, gehört das Schema etc. auch als Komponente zur Anwendung. Standardmässig wird eine SQlite-Datenbank verwendet, welche automatisch erstellt wird, falls sie nicht vorhanden ist.

## Konfigurieren und Starten
 
Die Anwendung kann als gewöhnliche Spring Boot Anwendung gestartet werden:

```
java -jar ilivalidatorws-server/target/ilivalidatorws-exec.jar
```

Konfiguration via _application.yml_ im Verzeichnis in dem der Service gestartet wird. Oder entsprechende alternative Konfigurationsmöglichkeiten von [Spring Boot](https://docs.spring.io/spring-boot/docs/2.7.12/reference/htmlsingle/#features.external-config).

Die Anwendung beinhaltet bereits eine _application.yml_-Datei. Siehe [application.yml](src/main/resources/application.yml), welche beim obigen Aufruf standardmässig verwendet wird.

Das Dockerimage wird wie folgt gestartet:

```
docker run -p8080:8080 sogis/ilivalidator-web-service:<VERSION>
```

Der Dockercontainer verwendet eine leicht angepasste Konfiguration ([application-docker.yml](src/main/resources/application-docker.yml)), damit das Mounten von Verzeichnissen hoffentlich einfacher fällt und weniger Fehler passieren:

- Die SQLite-Datenbank wird im _/work_-Verzeichnis angelegt, das im Dockerimage angelegt wird.
- Für `DOC_BASE` wird das _/docbase_-Verzeichnis verwendet, das im Dockerimage angelegt wird.
- Für `WORK_DIRECTORY` wird das _/work_-Verzeichnis verwendet, das im Dockerimage angelegt wird. 

Die allermeisten Optionen sind via Umgebungsvariablen exponiert und somit veränderbar. Im Extremfall kann immer noch ein neues Dockerimage erstellt werden mit einer ganz eigenen Konfiguration.

### Optionen (Umgebungsvariablen)

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `MAX_FILE_SIZE` | Die maximale Grösse einer Datei, die hochgeladen werden kann in Megabyte. | `300` |
| `LOG_LEVEL` | Das Logging-Level des Spring Boot Frameworks. | `INFO` |
| `LOG_LEVEL_DB_CONNECTION_POOL` | Das Logging-Level des DB-Connection-Poolsocket. | `INFO` |
| `LOG_LEVEL_APPLICATION` | Das Logging-Level der Anwendung (= selber geschriebener Code). | `DEBUG` |
| `CONNECT_TIMEOUT` | Die Zeit in Millisekunden, die bis zu einem erfolgreichem Connect gewartet wird. Betrifft sämtliche Methoden, welche `sun.net.client.defaultConnectTimeout` berücksichtigen. Die Option dient dazu damit langsame INTERLIS-Modellablage schneller zu einem Timeout führen. | `5000` |
| `READ_TIMEOUT` | Die Zeit in Millisekunden, die bis zu einem erfolgreichem Lesen gewartet wird. Betrifft sämtliche Methoden, welche `sun.net.client.defaultConnectTimeout` berücksichtigen. Die Option dient dazu damit langsame INTERLIS-Modellablage schneller zu einem Timeout führen. | `5000` |
| `DOC_BASE` | Verzeichnis auf dem Filesystem, das als Root-Verzeichnis für das Directory-Listing des Webservers dient. Das Root-Verzeichnis selber ist nicht sichtbar. | `/tmp/` |
| `CONFIG_DIRECTORY_NAME` | Unterverzeichnis im `DOC_BASE`-Verzeichnis, welches die _ini_- und _ili_-Verzeichnisse enthält. Dieses Verzeichnis ist unter http://localhost:8080/config erreichbar. Es muss nicht manuell erstellt werden. Es wird beim Starten der Anwendung erstellt. Das Verzeichnis muss bei einem Betrieb mit mehreren Containern geteilt werden, falls zusätzliche _ini_- und _ili_-Dateien in die entsprechenden Verzeichnisse kopiert werden. | `config` |
| `UNPACK_CONFIG_FILES` | In der Anwendung enthaltene _ini_- und _ili_-Dateien werden bei jedem Start der Anwendung in die entsprechenden Verzeichnisse kopiert. | `true` |
| `STORAGE_SERVICE` | Speicherort, der hochgeladenen Dateien und der Logfiles: `local`, `s3`. | `local` |
| `WORK_DIRECTORY` | Verzeichnis, in das die zu prüfenden INTERLIS-Transferdatei und die Logdateien kopiert werden (in ein temporäres Unterverzeichnis, das im `WORK_DIRECTORY` erstellt wird). Falls `local` Storage Service gewählt ist, muss das Verzeichnis, bei einem Betrieb mit mehreren Containern, zwingend geteilt werden muss. Sonst ist nicht sichergestellt, dass man die Logdatei(en) herunterladen kann. Falls `s3` Storage Service gewählt ist, muss der Name des Buckets gewählt werden, in den die Daten kopiert werden. | `/tmp/` |
| `FOLDER_PREFIX` | Für jede zu prüfende Datei wird im `WORK_DIRECTORY`-Verzeichnis ein temporäres Verzeichnis erstellt. Der Prefix wird dem Namen des temporären Verzeichnisses vorangestellt. | `ilivalidatorws_` |
| `CLEANER_ENABLED` | Dient zum Ein- und Ausschalten des Aufräumprozesses, der alte, geprüfte Dateien (INTERLIS-Transferdateien, Logfiles) löscht. | `true` |
| `REST_API_ENABLED` | Dient zum Ein- und Ausschalten des REST-API-Controllers und damit der eigentlichen Funktionalität (auch wenn Jobrunr trotzdem initialisiert wird). | `true` |
| `JDBC_URL` | Die JDBC-Url der Sqlite-Datei, die dem Speichern der Jobs dient, welche mittels REST-API getriggert wurden. Die Datei wird im Standard-`WORK`-Verzeichnis gespeichert, da dieses beim Multi-Container-Betrieb geteilt werden muss. Andere JDBC-fähige Datenbanken sind ebenfalls möglich. Dann müssten noch mindestens Login und Password als Option exponiert werden. Und die Anwendung müsste neu mit dem dazugehörigen JDBC-Treiber gebuildet werden. | `jdbc:sqlite:/tmp/jobrunr_db.sqlite` |
| `JOBRUNR_SERVER_ENABLED` | Dient die Instanz als sogenannter Background-Jobserver, d.h. werden mittels REST-API hochgeladene INTERLIS-Transferdateien validiert. Wird nur eine Instanz betrieben, muss die Option zwingen `true` sein, da sonst der Job nicht ausgeführt wird. | `true` |
| `JOBRUNR_POLL_INTERVAL` | Es wird im Intervall (in Sekunden) nach neuen Validierungsjobs geprüft. | `10` |
| `JOBRUNR_WORKER_COUNT` | Anzahl Jobs, die in einem "Worker" gleichzeitig durchgeführt werden können. Im Prinzip nicht sehr relevant, da der Validierungsjob synchronisiert ist (nicht thread safe). | `1` |
| `JOBRUNR_DASHBOARD_ENABLED` | Das Jobrunr-Dashboard wird auf dem Port 8000 gestartet. | `true` |
| `JOBRUNR_DASHBOARD_USER` | Username für Jobrunr-Dasboard. Achtung: Basic Authentication. | `admin` |
| `JOBRUNR_DASHBOARD_PWD` | Passwort für Jobrunr-Dasboard. Achtung: Basic Authentication. | `admin` |
| `TOMCAT_THREADS_MAX` | Maximale Anzahl Threads, welche die Anwendung gleichzeitig bearbeitet. | `20` |
| `TOMCAT_ACCEPT_COUNT` | Maximale Grösser der Queue, falls keine Threads mehr verfügbar. | `100` |
| `TOMCAT_MAX_CONNECTIONS` | Maximale Threads des Servers. | `500` |
| `HIKARI_MAX_POOL_SIZE` | Grösse des DB-Connections-Pools | `10` |
| `AWS_ACCESS_KEY_ID` | AWS Access Key |  |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key |  |

Ein `docker-run`-Befehl könnte circa so aussehen:

```
docker run --rm -p8080:8080 -p8000:8000 -v /shared_storage/docbase:/docbase/ -v /shared_storage/work:/work/ sogis/ilivalidator-web-service:2
```

Es werden zwei Ports gemapped. Der Port 8080 ist der Port der Anwendung und zwingend notwendig. Der Port 8000 dient dazu, dass das Jobrunr-Dashboard verfügbar ist.

Im lokalen Filesystem (oder Kubernetes-PV-Whatever etc.) müssen die beiden Verzeichnisse _/shared_storage/docbase/_ und _/shared_storage/work/_ vorhanden sein. Die beiden Verzeichnisse _/docbase/_ und _/work/_ werden unter den lokalen Verzeichnissen gemountet. Im _docbase_-Verzeichnis wird das Verzeichnis _config_ erstellt, falls es nicht existiert. Im selbigen wiederum werden die beiden Verzeichnisse _ili_ und _ini_ erstellt und in diese einige Dateien kopiert. Die SQLite-Datenbank, die dazu dient die REST-API-Jobs zu koordinieren, befindet sich im _/shared_storage/work/_-Verzeichnis. 

### Clean up

Ein Scheduler löscht jede Stunde (momentan hardcodiert) alle temporären Verzeichnisse, die älter als 60x60 Sekunden sind.

### Additional models

Ilivalidator needs an ini file if you want to apply an additional model for your additional checks. The ini file must be all lower case, placed in the _src/main/resources/ini_ folder and named like the base model itself, e.g. `SO_Nutzungsplanung_20171118` -> _so_nutzungsplanung_20171118.ini_. The additional model can be placed in the _src/main/resources/ili_ folder or in any model repository that ilivalidator finds out-of-the-box.

You can also upload and ini file and model file together with the transfer file you want to validate. 

### Ilivalidator custom functions

Custom-Funktionen können in zwei Varianten verwendet werden. Die Jar-Datei mit den Funktionen muss in einem Verzeichnis liegen und vor jeder Prüfung werden die Klassen dynamisch geladen. Das hat den Nachteil, dass man so kein Native-Image (GraalVM) mit Custom-Funktionen herstellen kann und man z.B. bei einem Webservice die Klassen nicht einfach als Dependency definiert kann, sondern die Jar-Datei muss in einem Verzeichnis liegen, welches beim Aufruf von _ilivalidator_ als Option übergeben wird. Bei der zweiten (neueren) Variante kann man die Custom-Funktionen als normale Dependency im Java-Projekt definieren. Zusätzlich müssen die einzelnen Klassen als Systemproperty der Anwendung bekannt gemacht werden. 

Im vorliegenden Fall wird die zweite Variante gewählt. Das notwendige Systemproperty wird in der `Application`-Klasse gesetzt. Falls man die erste Variante vorzieht oder aus anderen Gründen verwenden will, macht man z.B. ein Verzeichnis `src/main/resources/libs-ext/` und kopiert beim Builden die Jar-Datei in dieses Verzeichnis. Dazu wird eine Gradle-Konfiguration benötigt. Zur Laufzeit (also wenn geprüft wird) muss man die Jar-Datei auf das Filesystem kopieren und dieses Verzeichnis als Options _ilivalidator_ übergeben. Siehe dazu Code vor dem "aot"-Merge (welches Repo?).

### Clustering

Sämtliche "Koordinationsaufgaben" wie z.B. das Entpacken der Config-Dateien, das Löschen von alten Files etc. sollte (und in einigen Fällen: darf) nur von einer Instanz ausgeführt werden. Als Beispiel eine einfache `docker-compose` Konfiguration:

```
version: '3'
services:
  frontend:
    image: sogis/ilivalidator-web-service:2
    restart: unless-stopped
    environment:
      TZ: Europe/Zurich
    ports:
      - 8080:8080
      - 8000:8000
    volumes:
      - type: volume
        source: docbase
        target: /docbase
      - type: volume
        source: work
        target: /work
  worker:
    image: sogis/ilivalidator-web-service:2
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
        source: docbase
        target: /docbase
      - type: volume
        source: work
        target: /work
volumes:
  docbase:
  work:
```

Es wird ein "frontend"-Service gestartet, welche als Schnittstelle gegen aussen dient. Es werden mehrere "worker"-Services gestartet (`replicas`), die nur für das Validieren einer INTERLIS-Transferdatei zuständig sind. Es sind keine Port exponiert, da keiner dieser Service von aussen verfügbar sein muss. Es werden verschiedene Applikationsfunktionen ausgeschaltet (sieh Umgebungsvariablen). Das Jobmanagement wird mit [Jobrunr](https://jobrunr.io) gemacht.

Achtung: Mit Docker Compose Version 3 kann im Nicht-Swarm-Mode keine CPU-Limits gesetzt werden.

## Externe Abhängigkeiten

Keine.

## Konfiguration und Betrieb in der GDI

- https://github.com/sogis/openshift-templates/path/to/directory
- https://github.com/sogis/openshift-templates/blob/master/api-gateway/resources.yaml

## Interne Struktur

TODO:
- spring boot / maven multimodule 
- GWT
- Jobrunr
- rest api
- Max file size: Stand heute an zwei Orten...

## Entwicklung

### Run 

First Terminal:
```
./mvnw spring-boot:run -Penv-dev -pl *-server -am
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

```
docker build -t sogis/ilivalidator-web-service:latest -f Dockerfile.jvm .
```

```
./mvnw test -Dgroups="docker"
```

### Tests

```
./mvnw test -DexcludedGroups="docker"
```

```
./mvnw test -Dtest=SpringApiTests -DfailIfNoTests=false
```

```
./mvnw test -Dtest=SpringApiTests#validation_Ok_Interlis2Files -DfailIfNoTests=false
```


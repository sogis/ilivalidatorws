# REST-API

## OpenAPI Dokumentation

- http://localhost:8080/swagger-ui.html
- http://localhost:8080/v3/api-docs

## API

Es gibt einen Endpunkt ("/api/jobs"). Er dient dazu eine INTERLIS-Transferdatei hochzuladen (`POST`) und einen Validierungsprozess zu starten und um den Stand und das Resultat der Validierung abzufragen (`GET`).

```
curl -i -X POST -F 'files[]=@ch.so.afu.abbaustellen.xtf' http://localhost:8080/api/jobs
```

Es können mehrere Dateien (Transferfiles, Modell und Config-Dateien) hochgeladen werden:

```
curl -i -X POST -F 'files=@ch.so.afu.abbaustellen.xtf' -F 'files=@ch.so.avt.verkehrszaehlstellen.xtf' http://localhost:8080/api/jobs
```

(Unter Umständen muss `files[]` statt `files` verwendet werden.)

Es gibt einen weiteren Parameter `theme`. Mit diesem lässt sich einfach eine server-seitige Konfiguration wählen:

```
curl -i -X POST -F 'files=@2457_Messen_vorher.xtf' -F 'theme=nutzungsplanung' http://localhost:8080/api/jobs
```

Der Rückgabewert des POST-Requests ist:

```
HTTP/1.1 202
Operation-Location: http://localhost:8080/api/jobs/4d4aa583-6575-4200-a39c-621a5190d36d
Content-Length: 0
Date: Sat, 37 May 2023 16:40:50 GMT
```

Der Statuscode ist `202 Accepted`. Der Header `Operation-Location` zeigt den GET-Request an, der den Stand der Validierung zurückliefert. Bei der UUID handelt es sich um die Job-ID.

```
curl -i -X GET http://localhost:8080/api/jobs/4d4aa583-6575-4200-a39c-621a5190d36d
```

Wird der Befehl sofort ausgeführt, erscheint folgende Antwort:

```
HTTP/1.1 200
Retry-After: 30
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sat, 23 Jul 2022 16:43:12 GMT

{"createdAt":"2022-07-23T16:42:15.68317767","updatedAt":"2022-07-23T16:42:15.68317767","status":"PROCESSING"}
```

Der Statuscode ist `200 OK` und es wird ein `Retry-After`-Header gesendet, welcher dem Client mitteilt, wann er wieder den Status abfragen soll. Im JSON-Objekt sind neben dem Start- und Update-Zeitpunkt auf der Status des Validierungsprozesses vorhanden. Möglich sind `ENQUEUED`, `PROCESSING` und `SUCCEEDED`. Ist der Status `SUCCEEDED` erscheint kein `Retry-Header` und das JSON-Objekt enthält zusätzlich Links zu den Log-Dateien:

```
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sat, 23 Jul 2022 16:43:29 GMT

{"createdAt":"2022-07-23T16:42:15.68317767","updatedAt":"2022-07-23T16:43:16.011457796","status":"SUCCEEDED","validationResult":"SUCCEEDED","logFileLocation":"http://localhost:8080/logs/ilivalidator_8148789347157812698/254900.itf.log","xtfLogFileLocation":"http://localhost:8080/logs/ilivalidator_8148789347157812698/254900.itf.log.xtf"}
```

Jobs können auch gelöscht werden. Wird der Job bereits ausgeführt, wird der Eintrag in der Datenbank gelöscht, der laufende Job wird jedoch zu Ende prozessiert.

```
curl -X DELETE http://localhost:8080/api/jobs/f07e0038-98a8-4b6c-87c1-016875a87508
```

liefert - falls der Job in der Datenbank gefunden wird - folgendes:

```
HTTP/1.1 200
vary: accept-encoding
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 30 May 2023 16:41:49 GMT
Connection: close

{"message":"Job successfully deleted"}
```

Falls der Job nicht gefunden wird oder beim Abfragen des Status mit GET:

```
HTTP/1.1 400
vary: accept-encoding
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 30 May 2023 16:41:49 GMT
Connection: close

{"message":"Job not deleted"}
```

## Internal API

### /api/profiles

Liefert die auf dem Server vorhandenen ilivalidator-Konfigurationen zurück. Damit wird das GUI gesteuert.

## Links

- https://dunnhq.com/posts/2021/long-running-rest-requests/
- https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#1327-the-typical-flow-polling

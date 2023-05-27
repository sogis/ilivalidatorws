# Benutzerhandbuch

## Bedienung des Webservices

Der *ilivalidator web service* stellt eine einfache Art dar INTERLIS-Daten gegenüber einem INTERLIS-Modell  zu prüfen (= Modellkonformität). Die zu prüfenden INTERLIS-Daten werden mittels Webformular auf einen Server hochgeladen, wo sie anschliessend automatisch geprüft werden. Das Prüfresultat wird direkt im Browser angezeigt. 

Beim Aufruf des *ilivalidator web services* erscheint folgendes Webformular:

![ilivalidator Startseite](./images/ilivalidator01.png)

Im Browserfenster unter dem "Submit"-Knopf muss zwingend "Connected" stehen. 

Die zu prüfende Datei kann durch Knopfdruck auf "Browse" ausgewählt werden. Die Datei darf *nicht* gezippt sein und höchstens 50 Megabyte gross sein.

![ilivalidator Dateidialog](./images/ilivalidator02.png)

Im Feld "Choose file" erscheint neu der Name der ausgewählten Datei.

![ilivalidator Datei ausgewählt](./images/ilivalidator03.png)

Die Validierung kann durch Knopfdruck auf "Submit" gestartet werden. Der Knopf wird ausgegraut und es können keine weiteren Aktionen auf der Webseite vorgenommen werden. Im Browserfenster erscheinen die meldungen "Received: <Dateiname>" und "Validating...". 

![ilivalidator Upload gestartet](./images/ilivalidator04.png)

Die Prüfung kann - je nach Grösse der Datei resp. des Dateiinhaltes - ein paar Sekunden bis zu einigen Minuten dauern. Nach der Prüfung erscheint im Browser je nach Prüfresultat eine unterschiedliche Meldung. War die Prüfung erfolgreich, ist die Meldung "...validation done:" und ein Link zu der Log-Datei:

![ilivalidator Validierung Ok](./images/ilivalidator05.png)

Wurden bei der Validierung der Datei Fehler gefunden, ist die Meldung "...validation failed:" und ein Link zu der Log-Datei:

![ilivalidator Validierung failed](./images/ilivalidator06.png)

Unter gewissen Umständen kann es zu Unterbrüchen zwischen Browser und Validierungsserver kommen. In diesem Fall erscheint bei einer Aktion (z.B. "Submit"-Knopf drücken) die Meldung "Connection closed. Refresh Browser". In diesem Fall muss die Seite neu geladen werden und es sollte wieder die Melung "Connected" erscheinen.

![ilivalidator connection closed](./images/ilivalidator07.png)

## Visualisierung der Fehler

Neben der normalen Log-Datei steht ebenfalls die XTF-Log-Datei zum Download zur Verfügung ("Download XTF log file."). Diese kann dazu verwendet werden die Fehler im Erfassungssystem besser zu visualisieren. Achtung: Nicht jeder gefundende Fehler weist eine Koordinate auf, welche für die Visualisierung benötigt wird. Die XTF-Datei kann mit _ili2gpkg_ in eine GeoPackage-Dateiumgewandelt werden. _Ili2gpgk_ steht als Webservcie zur Verfügung: [https://geo.so.ch/ili2gpkg](https://geo.so.ch/ili2gpkg). Zusätzlich steht in QGIS ein Plugin zur Verfügung, welches die XTF-Datei direkt einlesen kann:

![XTFLog Checker](./images/qgisplugin01.png)

![QGIS XTF Log](./images/qgisplugin02.png)

## REST-Schnittstelle


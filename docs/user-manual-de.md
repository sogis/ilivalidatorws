# Benutzerhandbuch

## Bedienung des Webservices

Der *ilivalidator web service* stellt eine einfache Art dar INTERLIS-Daten gegenüber einem INTERLIS-Modell zu prüfen (= Modellkonformität). Die zu prüfenden INTERLIS-Daten werden mittels Webformular auf einen Server hochgeladen, wo sie anschliessend automatisch geprüft werden. Das Prüfresultat wird als Logdatei (.log und .xtf) zum Download bereitgestellt. 

Man kann die Validierung als Benutzer auf zwei Arten steuern:

- Man wählt das Thema mittels URL-Parameter (`?t=nutzungsplanung`). Der Service sucht in seinem Config-Verzeichnis ein _nutzungsplanunug.ini_-Datei und verwendet diese, falls vorhanden.
- Der Benutzer lädt eine _ini_-Datei zusammen mit der zur prüfenden Datei hoch.

Der URL-Parameter überschreibt die hochgeladenen ini-Datei.

Beim Aufruf des *ilivalidator web services* erscheint folgendes Webformular:

![ilivalidator Startseite](./images/ilivalidator01.png)

Die zu prüfenden Dateien können durch Knopfdruck auf "Choose Files" ausgewählt werden. Die Dateien dürfen *nicht* gezippt sein und zusammen höchstens 300 Megabyte gross sein.

![ilivalidator Dateidialog](./images/ilivalidator02.png)

Im Feld neben "Choose Files" erscheint neu der Name der ausgewählten Datei.

![ilivalidator Datei ausgewählt](./images/ilivalidator03.png)

Die Validierung kann durch Knopfdruck auf "Senden" gestartet werden. Der Knopf wird ausgegraut und es können keine weiteren Aktionen auf der Webseite vorgenommen werden. Im Browserfenster erscheinen die meldungen "<Dateiname> wird hochgeladen ..." und "Die Datei wird validiert ...". 

![ilivalidator Upload gestartet](./images/ilivalidator04.png)

Die Prüfung kann - je nach Grösse der Datei resp. des Dateiinhaltes - ein paar Sekunden bis zu einigen Minuten dauern. Nach der Prüfung erscheint im Browser je nach Prüfresultat eine unterschiedliche Meldung. Wurden keine Fehler in der Datei gefunden, erscheinen die Meldung "Die Validierung ist abgeschlossen. Es wurden keine Fehler gefunden." und Links zu den beiden Logdateien.

![ilivalidator Validierung Ok](./images/ilivalidator05.png)

Falls Fehler gefunden wurden, erscheint die Meldung "Die Validierung ist abgeschlossen. Es wurden Fehler gefunden."

![ilivalidator Validierung failed](./images/ilivalidator06.png)

## Visualisierung der Fehler

Neben der normalen Log-Datei steht ebenfalls die XTF-Log-Datei zum Download zur Verfügung ("Download XTF log file."). Diese kann dazu verwendet werden die Fehler im Erfassungssystem besser zu visualisieren. Achtung: Nicht jeder gefundende Fehler weist eine Koordinate auf, welche für die Visualisierung benötigt wird. Die XTF-Datei kann mit _ili2gpkg_ in eine GeoPackage-Dateiumgewandelt werden. _Ili2gpgk_ steht als Webservcie zur Verfügung: [https://geo.so.ch/ili2gpkg](https://geo.so.ch/ili2gpkg). Zusätzlich steht in QGIS ein Plugin zur Verfügung, welches die XTF-Datei direkt einlesen kann:

![XTFLog Checker](./images/qgisplugin01.png)

![QGIS XTF Log](./images/qgisplugin02.png)



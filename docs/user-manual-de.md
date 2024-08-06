# Benutzerhandbuch

## Bedienung des Webservices

Der *ilivalidator web service* stellt eine einfache Art dar INTERLIS-Daten gegenüber einem INTERLIS-Modell zu prüfen (= Modellkonformität). Die zu prüfenden INTERLIS-Daten werden mittels Webformular auf einen Server hochgeladen, wo sie anschliessend automatisch geprüft werden. Das Prüfresultat wird als Logdatei (.log, .xtf und .csv) zum Download bereitgestellt. 

Man kann die Validierung als Benutzer auf beeinflussen, indem man ein vorgegebens Prüfprofil wählt. Diese vordefinierten Prüfprofile sind .ini-Dateien. Siehe dazu die Optionen --config und --metaConfig von [ilivalidator](https://github.com/claeis/ilivalidator/blob/master/docs/ilivalidator.rst#aufruf-syntax).

Das Prüfprofil kann ebenfalls mittels URL-Parameter (z.B. `p=Nutzungsplanung`) gewählt werden. Die Auswahlbox im Browser wird automatisch nachgeführt.

Beim Aufruf des *ilivalidator web services* erscheint folgendes Webformular:

![ilivalidator Startseite](./images/ilivalidator01.png)

Die zu prüfenden Dateien können durch Knopfdruck auf "Choose Files" ausgewählt werden. Die Dateien dürfen *nicht* gezippt sein und zusammen höchstens 200 Megabyte gross sein.

![ilivalidator Dateidialog](./images/ilivalidator02.png)

Im Feld neben "Choose Files" erscheint neu der Name der ausgewählten Datei.

![ilivalidator Datei ausgewählt](./images/ilivalidator03.png)

Die Validierung kann durch Knopfdruck auf "Submit" gestartet werden. Der Knopf wird ausgegraut und es können keine weiteren Aktionen auf der Webseite vorgenommen werden. Im Browserfenster erscheinen die meldungen "<Dateiname> hochladen ..." und "Datei wird validiert ...". 

![ilivalidator Upload gestartet](./images/ilivalidator04.png)

Die Prüfung kann - je nach Grösse der Datei resp. des Dateiinhaltes - ein paar Sekunden bis zu einigen Minuten dauern. Nach der Prüfung erscheint im Browser je nach Prüfresultat eine unterschiedliche Meldung. Wurden keine Fehler in der Datei gefunden, erscheinen die Meldung "Es wurden keine Fehler gefunden." und Links zu den beiden Logdateien.

![ilivalidator Validierung Ok](./images/ilivalidator05.png)

Falls Fehler gefunden wurden, erscheint die Meldung "Es wurden Fehler gefunden."

![ilivalidator Validierung failed](./images/ilivalidator06.png)

## Visualisierung der Fehler

Neben der normalen Log-Datei steht ebenfalls die XTF-Log-Datei zum Download zur Verfügung ("Download XTF log file."). Diese kann dazu verwendet werden die Fehler im Erfassungssystem besser zu visualisieren. Achtung: Nicht jeder gefundende Fehler weist eine Koordinate auf, welche für die Visualisierung benötigt wird. Die XTF-Datei kann mit _ili2gpkg_ in eine GeoPackage-Dateiumgewandelt werden. _Ili2gpgk_ steht als Webservcie zur Verfügung: [https://geo.so.ch/ili2gpkg](https://geo.so.ch/ili2gpkg).

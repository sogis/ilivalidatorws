## Zusätzliche Tests
Bei den zusätzlichen Tests handelt es sich entweder um Verbesserungen im Kern der [Validierungsbibliothek](https://github.com/claeis/ilivalidator) oder um vom Kanton selber definierte und programmierte Tests. Diese zusätzlichen Tests sind notwendig, damit die Daten in späteren Prozessierungsschritten nicht zu falschen Aussagen und Interpretationen führen.

### Doppelte Koordinaten
Es dürfen keine doppelten Koordinaten in einer Geometrie vorkommen.

Fehlermeldung:
```
Error: line 412: SO_Nutzungsplanung_20171118.Nutzungsplanung.Grundnutzung: tid 2d285daf-a5ab-4106-a453-58eef2e921ab: duplicate coord at (2599932.281, 1216063.38, NaN)
```

### Überflüssige OID in Assoziationen
Ein Fehler von ili2db beim Exportieren von Assoziationen. Siehe [https://github.com/claeis/ili2db/issues/292](https://github.com/claeis/ili2db/issues/292)

Fehlermeldung:
```
Error: line 196: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Grundnutzung_Dokument: tid 14: Association SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Grundnutzung_Dokument must not have an OID (14)
```

### Falsche URL
URL müssen korrekt erfasst werden. Ausgenommen sind die Links zu den Dokumenten. Von diesen Links wird gemäss Datenmodell nur der stabile Teil erfasst. Es werden in diesem Fall keine Fehler gemeldet (sondern Warnungen).

Fehlermeldung:
```
Error: line 479: SO_Nutzungsplanung_20171118.TransferMetadaten.Amt: tid 6b6b2da3-c10b-4a37-bbbc-f640d860577f: invalid format of INTERLIS.URI value <www.firma-xy.ch> in attribute AmtImWeb
```

### Nicht-optionale Attribute
Einige Attribute (insbesondere) in der Dokumentenklasse müssen - obwohl als optional im Datenmodell beschrieben - erfasst werden. Siehe [Validierungsmodell](https://github.com/edigonzales/ilivalidator-web-service-nplso/blob/master/src/main/resources/ili/SO_Nutzungsplanung_20171118_Validierung_20190129_UTF8.ili#L35).

Fehlermeldung:
```
Error: line 430: SO_Nutzungsplanung_20171118.Rechtsvorschriften.Dokument: tid 6ADC5519-8FD6-441C-8975-726DD8E52612: Attribut 'Rechtsvorschrift' muss definiert sein.
```

### Dokumentenressource
Es wird geprüft, ob das erfasste Dokumente tatsächlich in der [Dokumentenablage](https://geo.so.ch/docs/ch.so.arp.zonenplaene/Zonenplaene_pdf/) des ARP vorhanden ist. 

Fehlermeldung:
```
Error: line 192: SO_Nutzungsplanung_20171118.Rechtsvorschriften.Dokument: tid 9C185FF7-B78F-445D-8868-905A569BA16C: Dokument 'https://geo.so.ch/docs/ch.so.arp.zonenplaene/Zonenplaene_pdf/78-Niederbuchsiten/Entscheide/78-10-E.pdf' wurde nicht gefunden.
```

### Kommunaler Code gemäss Objektkatalog
Der kommunale Code entspricht nicht den Vorgaben im Objektkatalog:

Fehlermeldung:
```
Error: line 139: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Ueberlagernd_Flaeche: tid 8f447eb5-86e6-4aa0-b3e6-650cbb99954b: Kommunaler Code ist nicht im Wertebereich gemäss Objektkatalog: 'N510_ueberlagernde_Ortsbildschutzzone' - '9299'
```

### Falsche Verbindlichkeit
Die Verbindlichkeit eines Typs entspricht nicht den Vorgaben im Objektkatalog:

Fehlermeldung:
```
Error: line 298: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Ueberlagernd_Flaeche: tid ef9368d4-f7ed-409c-89b2-5ca443a5f92b: Attributwert Verbindlichkeit ist nicht identisch zum Objektkatalog: 'orientierend' - '6110'
```

### Falsche Bezeichnung
Der Wert des Attributes 'Bezeichnung' entspricht nicht den Vorgaben im Objektkatalog:

Fehlermeldung:
```
Error: line 5631: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Grundnutzung: tid 7e5bd2f9-48ec-446c-aaac-305d0759b3a7: Attributwert Bezeichnung ist nicht identisch zum Objektkatalog: 'Wasser' - 'N320_Gewaesser'
```

### Verknüpfung der Typen mit den Dokumenten
Verschiedene Typen müssen zwingend mit einem Dokument verknüpft sein.

Fehlermeldung:
```
Error: line 46065: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Grundnutzung: tid F8DE04B4-2E51-4B53-97DD-959A2B47242C: Typ 'N169_weitere_eingeschraenkte_Bauzonen' (Typ_Grundnutzung) ist mit keinem Dokument verknüpft.
```

### Cycledetector 
In der Assoziation `HinweisWeitereDokumente` können beliebige Dokumente miteinander verknüpft werden. Falsch sind Verknüpfungen, die eine Endlosschlaufe bilden. Z.B. Dokument A -> Dokument B -> Dokument C -> Dokument B. Da in solchen Fällen nicht klar ist, welche Verknüpfung falsch ist, kann nur festgestellt werden, dass ein Fehler vorliegt.

Fehlermeldung:
```
Error: line 451: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: (325CA1F1-17F5-4F19-A2E4-ECD942DB6DCA <-> E9597D3A-90CD-4175-97B5-CFEAE56CB7BE) is part of a cycle: E9597D3A-90CD-4175-97B5-CFEAE56CB7BE,325CA1F1-17F5-4F19-A2E4-ECD942DB6DCA.
Error: line 451: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: Set Constraint SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente.isValidDocumentsCycle is not true.
Error: line 457: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: (E9597D3A-90CD-4175-97B5-CFEAE56CB7BE <-> 325CA1F1-17F5-4F19-A2E4-ECD942DB6DCA) is part of a cycle: E9597D3A-90CD-4175-97B5-CFEAE56CB7BE,325CA1F1-17F5-4F19-A2E4-ECD942DB6DCA.
```

### AREA-Bedingung für Lärmempfindlichkeit
Die Geometrien sämtliche Lärmempfindlichkeitstypen müssen zusammen eine AREA bilden. Achtung: Löcher werden keine detektiert, nur Überlappungen. Häufig sind die Fehler dort zu finden, wo man die Grundnutzung kopierte und die Geometrien verschneiden musste.  

Fehlermeldung:
```
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection overlap 3.2508012350263016E-4, coord1 (2612419.901, 1248771.194), coord2 (2612428.532, 1248767.551),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612419.220, 1248771.482),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612419.012, 1248771.573),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612375.134, 1248774.377),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612103.954, 1248963.905),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection overlap 1.261583826789453E-4, coord1 (2612155.987, 1248879.452), coord2 (2612158.396, 1248881.354),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612158.781, 1248881.590),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612103.954, 1248963.905),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612158.781, 1248881.590),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection overlap 4.7300282660083307E-4, coord1 (2612158.997, 1248881.716), coord2 (2612164.438, 1248883.562),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612364.145, 1248774.882),
Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: Intersection coord1 (2612375.134, 1248774.377),
Error: Set Constraint SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche.laermempfindlichkeitsAreaCheck is not true.
```

### Fläche ist kleiner 0.01 m2
Für die Grundnutzung und die überlagernden Flächen wird geprüft, ob die Fläche grösser 0.01 m2 ist. Kleinere Flächen weisen auf "Restflächen" von Verschnitten hin. Es wird eine Warnung geloggt:

```
Warning: line 3207: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: tid 68D23F2B-3035-4CAC-8B8F-DD20A3CEFDC7: Fläche der Geometrie ist kleiner 0.01 m2.
```

### Linie ist kürzer 0.1 m
Für die überlagernden Linien und die Linienobjekte der Erschliessung wird geprüft, ob die Länge der Geometrie grösser 0.1 m ist. Es wird eine Warnung geloggt:

```
Warning: line 1054: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Linie: tid 3484b06f-0e3f-4e50-8afd-34cf6904f19f: Länge der Geometrie ist kleiner 0.1 m.
```


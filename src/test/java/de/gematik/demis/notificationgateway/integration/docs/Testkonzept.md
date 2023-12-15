# Testkonzept: Notification Gateway API
Testart, Stufe: API, Systemtest

## Zielgruppe
- Test- und Architekten
- Softwareentwickler
- Testautomatisierer


## Motivation




## Begleitende Aspekte

### Interoperabilität mit anderen Diensten und Services
- Authenticationprozess muss berücksichtigt werden, in dem einen validen Bearer geholte werden muss, damit die Absetzung des Requests
  erfolgreich durchgeführt werden kann.
- Die initiale Testimplementierung und Ausführung findet am|von Entwicklerrechner und außerhalb der Räumlichkeiten der gematik statt.
  Eine VPN-Verbindung benötigt wird, um mit der Gateway API kommunizieren zu können.



## Bestandaufnahme
  • Keine Entwickler- oder Regressionstestfälle vorhanden

## Technologien und Methoden
Vorgesehen:
 - Serenity BDD
    - Cucumber
    - Rest assured
    - (Tiger Framework)
    - …
Methoden(Implementierung):
  -  Notification Gateway Yaml -> POJO

## Testobjektabgrenzung
Ausgangsmaterial: yaml-Datei
Spezifikation: nicht vorliegend
Kritikalität: mittel|gering
Fordert hoher Testintensität: nein
Testfokus: 
  • Auf Felder, die ausschlaggebende Bedeutung für das System haben

### Zu testende Eigenschaften
- Funktion der Notification Gateway API
    - Labormeldungen
    - Hospitalisierungsmeldungen
    - Fehlerbehandlung für o.g. Meldungsarten

- Als Testbasis könnte die `Notification.yaml`-[Datei](https://gitlab.prod.ccs.gematik.solutions/git/demis/notification-gateway/-/blob/master/src/main/resources/Notification.yaml) dienen
  und die darin in JSON-Format beschriebenen Felder und ihren Constrains


### Nicht zu testende Eigenschaften
- Sicherheitstests sind nicht vorgesehen
- Tests, welche in Verbindung mit dem Authorisierungsprozess stehen, werden nicht getestet
- Es ist keine Implementierung von Unittests vorgesehen

Testeingangskriterien
  1. Testobjekt ist auf einer dedizierten stabil laufenden Testumgebung vorhanden
  2. Testobjekt ist durch einer Versionsnummer identifizierbar
  3. …

### Testfallermittlung
Je nach Testbasis oder Spezifikation

### Testarchitektur
Begleitende Aspekte
  - (Tiger)
  - Proxies zur TI
  - Authentifizierung an TI
  - …
### Anforderungen an Testdurchführung
  - Lauffähigkeit in CI
  - Nightly builds?
  - Support bei Hotfixes?
  - Integration | Migration ins Entwicklungsprojekt?


### Testende
  - Tests sind ausgeführt worden
  - Fehlgeschlagene Tests sind analysiert und klassifiziert|priosiert
  - Bericht


***

## Review des Testkonzeptes
Das Review des Testkonzeptes erfolgt durch die Teilnehmende: PO, PK, TK, betroffene Testteams und wird in einer Reviewsitzung
besprochen und abgenommen. Sobald Überarbeitungen notwendig sind, sollen diese nach der Sitzung von den Autoren vorgenommen werden.


## Autoren und Mitwirkende
- Ghislain Mbogos
- Reza Aledawud
- Yavor Vasilev

## Projektstatus
In Bearbeitung



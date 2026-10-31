# FYLO 0.2.2 – Assistenz-Kern

Native Android-Basis in Kotlin und Jetpack Compose. Technischer versionCode 14; applicationId und Signaturschlüssel bleiben erhalten.

## Startbildschirm

- Analoge Uhr mit Sekundenzeiger, gegenüber 0.1.1 um weitere 25 % im Durchmesser verkleinert.
- Die gesamte Uhr hat 60 % Deckkraft (40 % transparent).
- Datum und kompakte Akku-/Ladeanzeige (14 sp) rechts neben der Uhr.
- Direkte KI-Zugriffszeile. Sie öffnet ein installiertes Modul mit der Rolle assistant; ohne KI-Modul erscheint ein klarer Hinweis.
- Benannte Screens mit unlabeled Kacheln oberhalb des Statusfensters und horizontalem Wischen.
- Die Systemsteuerung legt Screens an, zeigt Bezeichnung und automatisch vergebene vierstellige Signatur und löscht nur Screens ohne Modulzuordnungen.
- Module lassen sich über ihre Screen-Zuordnung auf einen Screen setzen oder davon entfernen. Nicht verfügbare Module behalten ihre Zuordnung, bis sie ausdrücklich entfernt wird.
- Festes Statusfenster direkt unter den Screen-Kacheln. Es zeigt Bereitschaft, gespeicherte Änderungen, Modulstarts und Fehler. Noch kein externes Modulmeldungsprotokoll und kein Berichtsarchiv.
- Der wechselbare Modulbereich liegt vollständig unterhalb des Statusfensters und erhält den restlichen verfügbaren Bildschirmplatz. Module erscheinen als Icon-Kacheln mit Modulbezeichnung; bei vielen Modulen scrollt nur das Icon-Raster vertikal. Das Statusfenster bleibt beim Wischen und Scrollen stehen.
- Zahnrad oben rechts öffnet das Einstellungen-Popup.

Die CPU-Funktion ist vollständig entfernt: keine Messungen, kein Diagramm, keine Peak-Werte und kein CPU-Abschnitt in den Einstellungen. Uhr und Batterieanzeige aktualisieren sich im Vordergrund; im Hintergrund pausieren die Abfragen.

## Screen-Daten und Migration

Bei der ersten Nutzung von 0.2.0 wird atomar die Datei screens-v1.json (Schema 1) im privaten App-Verzeichnis angelegt. Der erste Screen heißt Start und bekommt 0001. Aus 0.1.2 müssen keine Screen-Daten migriert werden; installierte Module werden nicht verändert und zunächst keinem Screen zugeordnet. Namen, Signaturen, Modulzuordnungen und die KI-Modulauswahl bleiben über Neustarts erhalten. Defekte/unbekannte Daten werden gemeldet und nicht überschrieben.

Die Signaturen werden unter Schreibsperre fortlaufend von 0001 bis 9999 vergeben und nie wiederverwendet. Der Zähler bleibt auch nach dem Löschen bestehen. Nach 9999 vergebenen Signaturen wird die Erstellung mit einem Hinweis abgelehnt. Die Eindeutigkeit gilt innerhalb der gespeicherten FYLO-Installation; das Löschen sämtlicher App-Daten setzt auch diesen Verlauf zurück. Auch der letzte Screen darf gelöscht werden, wenn er leer ist. Die Systemsteuerung bleibt erreichbar.

## Module und KI

Separate FYLO-Modul-APKs lassen sich über das Einstellungen-Popup mit Androids Installer nachinstallieren und anschließend öffnen. Das separat installierbare [Notizmodul 1.0.0](docs/NOTES.md) ist enthalten; es wird als eigene APK ausgeliefert. Die neun gemeinsamen [Aktionsicons](docs/ICONS.md) sind den vorhandenen Funktionen zugeordnet. Die [Modulschnittstelle](docs/MODULES.md) ist dokumentiert. Das ist der Assistenz-Kern; ein KI-Modul/Sprachmodell ist noch nicht enthalten. Kein API-Schlüssel und kein kostenpflichtiger Dienst vorkonfiguriert.

## Bauen und prüfen

JDK 17, Gradle 8.9, Android SDK 35:

```sh
gradle --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :notes:lintDebug :notes:testDebugUnitTest :notes:assembleDebug
```

Der GitHub-Workflow führt diese Checks aus und erzeugt ein **unsigniertes, nicht direkt installierbares** internes Build-Artefakt. Installierbare Entwicklungs-APKs werden ausschließlich separat mit dem bestehenden privaten Schlüssel via tools/sign_apk.py signiert und geprüft. Keine Schlüssel im Repository.

## Gerätezustand

Ein Code-Neustart löscht keine Daten auf dem Handy. Für den gewünschten vollständigen Neustart die Daten von FYLO/FYLO Neu in Androids App-Info löschen und gegebenenfalls gemeinsame FYLO-Ordner im Dateimanager entfernen. Diese Version liest die alten Daten nicht und führt keine versteckte Löschung gemeinsamer Dateien durch. Der alte Projektstand bleibt in backup/v0.7.0-before-reset-2026-10-31 erhalten.

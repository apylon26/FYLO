# FYLO 0.1.2 – Assistenz-Kern

Native Android-Basis in Kotlin und Jetpack Compose. Technischer versionCode 11; applicationId und Signaturschlüssel bleiben erhalten.

## Startbildschirm

- Analoge Uhr mit Sekundenzeiger, gegenüber 0.1.1 um weitere 25 % im Durchmesser verkleinert.
- Die gesamte Uhr hat 60 % Deckkraft (40 % transparent).
- Datum, Akkustand und Ladezustand rechts neben der Uhr.
- Statusfenster unterhalb der Uhr für spätere Anwendungen und Funktionen.
- Zahnrad oben rechts öffnet das Einstellungen-Popup.

Die CPU-Funktion ist vollständig entfernt: keine Messungen, kein Diagramm, keine Peak-Werte und kein CPU-Abschnitt in den Einstellungen. Uhr und Batterieanzeige aktualisieren sich im Vordergrund; im Hintergrund pausieren die Abfragen.

## Module und KI

Separate FYLO-Modul-APKs lassen sich über das Einstellungen-Popup mit Androids Installer nachinstallieren und anschließend öffnen. Noch keine Module vorinstalliert. Die [Modulschnittstelle](docs/MODULES.md) ist dokumentiert. Das ist der Assistenz-Kern; ein KI-Modul/Sprachmodell ist noch nicht enthalten. Kein API-Schlüssel und kein kostenpflichtiger Dienst vorkonfiguriert.

## Bauen und prüfen

JDK 17, Gradle 8.9, Android SDK 35:

```sh
gradle --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

Der GitHub-Workflow führt diese Checks aus und erzeugt ein **unsigniertes, nicht direkt installierbares** internes Build-Artefakt. Installierbare Entwicklungs-APKs werden ausschließlich separat mit dem bestehenden privaten Schlüssel via tools/sign_apk.py signiert und geprüft. Keine Schlüssel im Repository.

## Gerätezustand

Ein Code-Neustart löscht keine Daten auf dem Handy. Für den gewünschten vollständigen Neustart die Daten von FYLO/FYLO Neu in Androids App-Info löschen und gegebenenfalls gemeinsame FYLO-Ordner im Dateimanager entfernen. Diese Version liest die alten Daten nicht und führt keine versteckte Löschung gemeinsamer Dateien durch. Der alte Projektstand bleibt in backup/v0.7.0-before-reset-2026-10-31 erhalten.

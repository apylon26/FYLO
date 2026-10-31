# FYLO 0.1.1 – neuer Assistenz-Kern

Native Android-Basis in Kotlin und Jetpack Compose. Neu aufgebaut nach dem vollständigen Projektreset; keine alten Module oder Daten werden importiert. applicationId und Signaturschlüssel bleiben für kompatible Installation erhalten; die Versionsnummer startet neu bei 0.1.0; dieses Update ist 0.1.1 mit technischem versionCode 10.

## Startbildschirm

- Analoge Uhr mit Stunden-, Minuten- und orangefarbenem Sekundenzeiger, jede Sekunde an die aktuelle Systemzeit angepasst.
- Uhr mit halbem Durchmesser gegenüber 0.1.0. Datum, Akkustand und Ladezustand stehen rechts neben der Uhr.
- Statusfenster unterhalb der Uhr als ausdrücklich gekennzeichneter Platzhalter für spätere Anwendungen und Funktionen.
- CPU-Spinnennetz mit drei Achsen (Minimum, aktuell, Maximum), automatisch skalierter Messfläche, Min-Peak und Max-Peak. Der orange Indikator läuft in acht Sekunden innerhalb des Dreiecks um; seine Entfernung zur Mitte richtet sich nach der aktuellen Auslastung. Die Rotation ist eine Darstellungsanimation und kein weiterer Messwert. Bei 0 % bleibt er in der Mitte. Die Messfläche und Zahlen zeigen die echten Messwerte. Gültige Messwerte aktualisieren sich jede Sekunde, während FYLO sichtbar ist. Die Uhr und das Sampling pausieren im Hintergrund.
- Blaues rundes Zahnrad mit orangefarbenem Symbol oben rechts öffnet ein scrollbar angelegtes Einstellungen-Popup.

Die CPU-Quelle ist beschriftet: `CPU · Gerät`, wenn Android das Lesen von /proc/stat erlaubt; sonst `CPU · FYLO`, gemessen über Process.getElapsedCpuTime und normiert auf alle verfügbaren Prozessorkerne. Kein erfundener Gerätewert. Minimum/Maximum gelten für den laufenden App-Prozess und dieselbe Quelle. Ungültige Messungen erscheinen als Strich. Bei einem Quellenwechsel werden die Extremwerte zurückgesetzt.

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

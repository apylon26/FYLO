# FYLO – persönlicher Android-Assistent

Native Android-Haupt-App für Andreas, Kotlin + Jetpack Compose. Version 0.2.0.

## Vier wischbare Screens

1. **Start:** Übersicht und Symbole für aktive Module.
2. **Assistent:** lokaler Befehl `Aufgabe: dein Text`; noch keine freie KI-Unterhaltung.
3. **Module:** integrierte Aufgaben und Notizen aktivieren/deaktivieren.
4. **System:** Versions-, Datenschutz- und Verbindungsstatus.

Wie bei einem Handy-Startbildschirm wechseln horizontale Wischgesten die Seiten.
Zusätzlich gibt es eine feste Navigation und vier Seitenpunkte. Module öffnen als eigener
Dialog; Zurück führt wieder zur Haupt-App. FYLO ist eine eigene App, derzeit kein Android-Launcher.

## Enthalten

Aufgaben anlegen, abhaken und löschen; Notizen mit Speichern; lokale Datenhaltung;
Aktivierung der Module bleibt beim Neustart erhalten. Neue Module werden zunächst durch
App-Updates ergänzt. Siehe [Modulentwicklung](docs/MODULES.md).

Noch nicht enthalten: KI-Modell, Kalender, Dateien, Sprache, Hintergrundagent,
Erinnerungen und separat herunterladbare Plugins. Kein API-Schlüssel erforderlich.

## APK auf dem Handy

GitHub → FYLO → Actions → Android APK → erfolgreicher Lauf → Artifacts → FYLO-debug-N.
ZIP herunterladen, entpacken und `app-debug.apk` öffnen. Installation auf dem Android-Gerät
selbst bestätigen; gegebenenfalls GitHub im Browser in Desktopansicht öffnen.

Die CI führt Android Lint, Unit-Tests und Debug-APK-Build aus. Der erste erfolgreiche CI-Lauf
ist Voraussetzung für eine getestete APK. Die Erstellungsumgebung hat kein Android SDK oder
Gradle; hier wurden nur Quellstruktur, XML und Workflow geprüft, kein Emulator bedient.

## Entwicklung

JDK 17, Gradle 8.9, Android SDK 35 und Build Tools 35.0.0.
CI installiert Gradle explizit; ein Gradle Wrapper ist noch nicht enthalten.

```sh
gradle --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

## Daten und Signierung

Nutzdaten bleiben im privaten App-Bereich, ohne eigene Verschlüsselung. Kein Netzwerkzugriff,
keine Tokens oder Zugangsdaten. Deinstallation kann Daten löschen. Ein Export ist noch nicht vorhanden.
CI verwendet den Android-Debug-Schlüssel, der zwischen CI-Läufen wechseln kann. Vor dauerhafter
Nutzung stabile Signierung einrichten, damit Folgeversionen zuverlässig als Updates installierbar sind.
Keine Schlüssel oder privaten Informationen ins öffentliche Repository übertragen.

## Arbeit mit ChatGPT

Änderungswünsche hier beschreiben, Code im freigegebenen Repository bearbeiten, CI-Build prüfen
und APK auf dem Handy testen. Zugang und tatsächliche Schreibmöglichkeiten sind jeweils zu prüfen.

## FYLO 0.3.0 – animierter Kern

Auf der Startseite kreisen vier Satelliten um eine blau-orange Kugel. Einfache Canvas-Verläufe statt aufwendiger Echtzeit-3D-Effekte; maximal etwa 30 Aktualisierungen pro Sekunde. Gedrückt halten beschleunigt die Bewegung und verstärkt das Licht. Beim Wischen, geöffnetem Modul, außerhalb des sichtbaren Bereichs sowie bei pausierter App oder ausgeschaltetem Display stoppt die Animation. Die Android-Einstellung zum Deaktivieren von Animationen wird berücksichtigt. Aufgaben, Notizen und Moduleinstellungen bleiben unverändert. Der Entwurf ist nativ nachgebildet; Kalender, Dateien und freie KI-Gespräche werden damit nicht ergänzt.

Der konkrete Akkuverbrauch wurde noch nicht auf einem Gerät gemessen.

## Installationskorrektur 0.3.1

Die bisherigen CI-Läufe erzeugten unterschiedliche temporäre Debug-Schlüssel. Ein Update der alten App ist ohne deren ursprünglichen privaten Schlüssel nicht möglich. FYLO Neu (com.apylon26.fylo.app) wird deshalb parallel installiert; die alte App und ihre Daten bleiben erhalten. Es gibt keine automatische Datenübernahme.

CI erzeugt nun ausdrücklich nicht installierbare, unsignierte Entwicklungs-APKs. Vor der Weitergabe werden diese mit tools/sign_apk.py und dem privat gesicherten festen Schlüssel signiert und geprüft. Der private Schlüssel liegt ausschließlich im Backup FYLO-Android-Signierung.zip außerhalb dieses Repositorys. Der SHA-256-Fingerabdruck des Zertifikats ist im Skript festgehalten. Keine zufälligen Ersatzschlüssel verwenden. Der Installationsdialog von Play Protect für unbekannte Entwickler kann weiterhin erscheinen.

## Oberfläche 0.4.0

Die numerische Seitenzählung entfällt. Der blaue/orange Kern wurde aus dem ursprünglichen Design als transparentes Bildelement aufbereitet; Satelliten werden weiterhin mit etwa 30 Aktualisierungen pro Sekunde davor und dahinter bewegt. Rechts daneben stehen lokale Uhrzeit und Akkustand sowie CPU FYLO.

CPU FYLO ist die gemessene Prozess-CPU-Zeit (Process.getElapsedCpuTime) geteilt durch die vergangene Echtzeit und die Anzahl logischer Prozessoren, in Prozent der gesamten logischen CPU-Kapazität. Es ist ausdrücklich keine Messung der gesamten Systemauslastung. Aktualisierung einmal pro Sekunde, ausschließlich bei sichtbarer Startseite und aktiver App. Min und Max beziehen sich auf die gültigen Messungen während der Lebensdauer des Statusfensters; Hintergrundintervalle werden nicht einbezogen. Der erste Wert erscheint nach einer Sekunde. Min/aktuell/Max stehen auf einer festen Skala von 0 bis 100 Prozent und haben identisch große rechteckige Marker (blau/orange/blau). Bei gleichen Werten überlagern sich die Marker; die drei Zahlen bleiben separat lesbar.

App-Kennung, privater Signaturschlüssel und Datenformat bleiben unverändert.

## Notizen ab 0.5.0
Einzelne Notizen mit Titel, Text und Dateianhängen. Blauer Haken im Kreis oben rechts speichert;
das rote X schließt den Editor. Ungespeicherte Änderungen erfordern eine Verwerfen-Entscheidung.
Beim ersten Anhang zunächst „Ablage einrichten“ wählen: Android fragt nach einem beschreibbaren
Ordner auf dem Handy (z. B. Dokumente). Darin legt FYLO `FYLO/Dateien/<Notiz-ID>/` an.
Ist der ausgewählte Ordner bereits FYLO, wird nur Dateien ergänzt. Es werden Kopien angelegt;
Originaldateien bleiben unverändert. Gleiche Dateinamen werden vom Dokumentanbieter aufgelöst.
Anhänge sind per Antippen mit einer passenden installierten App zu öffnen.
Beim Verwerfen werden neu importierte Kopien nach Möglichkeit entfernt; gespeicherte Anhänge bleiben erhalten.
Eine geänderte Ablage betrifft neue Importe, bestehende Anhänge behalten ihren Speicherort.

Migration: Der bisherige SharedPreferences-Schlüssel `note` bleibt unverändert als Sicherung.
Fehlt `notes_v1`, wird sein gesamter Inhalt als „Bisherige Notiz“ angezeigt. Erst beim expliziten
Speichern schreibt FYLO die Liste mit IDs und Anhängen nach `notes_v1`. Aufgaben bleiben unverändert.
Ungültiges Notiz-JSON blockiert das Speichern, statt Daten mit einer leeren Liste zu überschreiben.
Dateien und Berechtigungen bleiben über App-Neustarts erhalten; entfernte Dateien oder entzogene
Ordnerberechtigungen werden als Fehler gemeldet. Nach Deinstallation müssen vorhandene Kopien
bei Bedarf neu zugeordnet werden. Die App kann keinen Ordner vor Installation und Auswahl auf dem Handy erstellen.

## Notiz-Werkzeugleiste ab 0.6.0
Oben stehen fünf blaue runde Schaltflächen mit Verlauf, Lichtkante und Schatten;
die Zeichen sind orange: ✓ neue Notiz, + Datei hinzufügen, − Zuordnung entfernen,
! Dateikopie löschen, X gesamtes Modul schließen. Unter den Symbolen stehen kurze Beschriftungen.
Für − und ! zunächst einen Anhang über seinen Auswahlkreis markieren; der Dateiname öffnet ihn.
Speichern bleibt als beschriftete Schaltfläche im Editor. Vor einer neuen Notiz
müssen Änderungen gespeichert oder über „Zur Notizliste“ verworfen werden.
Entfernen ändert ausschließlich die Zuordnung. Löschen erfordert eine Bestätigung
und wird erst nach erfolgreichem Speichern an der FYLO-Kopie ausgeführt.
Fehlgeschlagene Löschungen werden gemeldet und können durch erneutes Speichern wiederholt werden.
Die ursprüngliche importierte Quelldatei bleibt erhalten. Datenformat, App-ID und Signatur bleiben gleich.

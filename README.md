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

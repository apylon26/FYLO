# FYLO Notizen 1.0.0

Ein eigenständig installierbares Android-Modul mit applicationId com.apylon26.fylo.notes und FYLO-Modulschnittstelle 1. Es arbeitet offline und enthält kein KI-Modell. In FYLO unter Systemsteuerung → Module öffnen oder einem Screen zuordnen. Das Modul hat außerdem einen eigenen Android-Launcher-Eintrag FYLO Notizen.

## Bedienung

- In der Übersicht mit Plus eine neue Notiz anlegen oder eine vorhandene Notiz antippen.
- Titel ist optional; Text oder mindestens ein Anhang muss vorhanden sein.
- Obere Leiste: Haken speichert und kehrt zur Übersicht zurück. Papierkorb löscht nach Bestätigung die Notiz. Plus öffnet Androids Dateiauswahl für einen oder mehrere Anhänge. Minus entfernt den per Radiobutton ausgewählten Anhang aus dem Entwurf. X schließt den Editor; bei Änderungen bietet es Speichern, Verwerfen oder Abbrechen an. X in der Übersicht schließt das Modul.
- Der Dateiname öffnet einen Anhang über eine geeignete installierte App.
- Anhänge und Entfernungen werden mit der Notiz durch den Haken gespeichert. Unmittelbar nach dem Entfernen kann der Entwurf noch verworfen werden, ohne den gespeicherten Anhang zu verlieren.

## Daten und Dateien

Gespeicherte Notizen liegen als atomar geschriebener JSON-Index notes-v1.json (Schema 1) im privaten App-Verzeichnis. Notiz- und Anhangkennungen sind UUIDs; Titel, Text, Erstellungs-/Änderungszeit und Anhang-Metadaten werden zusammen gespeichert. Defekte oder unbekannte Indexdaten werden gemeldet und nicht überschrieben. In diesem Fall wird auch keine Dateibereinigung gestartet.

Anhangdateien werden beim Import in den privaten Ordner attachments kopiert. Unterstützt sind alle über Androids Dokumentauswahl verfügbaren Dateitypen, bis 512 MB pro Datei. Eine UUID als Dateiname verhindert die Übernahme fremder Pfade. Ein abgebrochener Kopiervorgang entfernt seine Teilkopie. Die Originaldateien werden weder verändert noch gelöscht. Beim Speichern, Verwerfen oder Löschen sowie beim nächsten Start werden nur nicht mehr referenzierte app-eigene Anhangkopien bereinigt. Beim Öffnen erhält die Viewer-App eine vorübergehende Lesefreigabe für genau diese Kopie.

Entwürfe bleiben bei Bildschirmdrehung im ViewModel erhalten. Dauerhaft gespeichert wird mit dem Haken; ein nicht gespeicherter Entwurf ist nach Beendigung des Android-Prozesses nicht garantiert verfügbar. Bei einer Deinstallation des Notizmoduls werden dessen private gespeicherte Daten durch Android entfernt.

Es gibt keine automatische Migration aus dem alten, vor dem Projektreset entwickelten Notizmodul. Die neuen Notizen sind unabhängig von der Screen-Platzierung: Beim Verschieben des Moduls bleiben sie erhalten. Die Screen-Signatur wird vom Host übergeben, trennt diese erste Notizversion aber nicht in mehrere Notizbücher.

## Prüfung und Signierung

```sh
gradle --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :notes:lintDebug :notes:testDebugUnitTest :notes:assembleDebug
```

CI erzeugt zwei unsignierte Entwicklungs-APKs. Beide werden danach ausschließlich mit dem vorhandenen privaten FYLO-Schlüssel signiert und gegen dessen Fingerabdruck geprüft. Kein Schlüssel liegt im Repository.

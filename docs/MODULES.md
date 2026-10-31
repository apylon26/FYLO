# FYLO-Modulkern

Die Haupt-App besitzt vier konstante Seiten: Start, Assistent, Module und System.
`FyloShell` verwaltet den Pager, die Navigation und das Öffnen von Modulen.
`ModuleRegistry` enthält stabile IDs, Titel, Symbole und Beschreibungen.
`FyloStore` hält die gemeinsamen Aufgaben, Notizen und Aktivierungszustände.
`ModuleDialog` öffnet die integrierten Funktionen oberhalb der Hauptoberfläche.

## Modul ergänzen

1. Stabile ModuleId und Registry-Metadaten ergänzen.
2. Fachlogik und eigene Compose-Oberfläche unter einem feature-Paket implementieren.
3. Persistenz und Aktivierungszustand im Store ergänzen bzw. für größere Module eigene Repositories verwenden.
4. Modul in ModuleDialog aufrufen; neue Dienste für lokale Befehle getrennt anbinden.
5. Berechtigungen erst hinzufügen, wenn die Funktion sie benötigt.
6. Registry-Test und relevante fachliche Tests ausführen.

Diese Version verwendet mitkompilierte Module. Externe Plugins können noch nicht heruntergeladen
oder unabhängig installiert werden. Kalender, Dateien, Sprache, KI, Hintergrundagent und
Erinnerungen sind zukünftige Erweiterungen. FYLO ersetzt derzeit nicht den Android-Launcher.

## Daten

SharedPreferences-Datei `fylo`, Schlüssel `tasks` (JSON), `note` (String),
`module_TASKS` und `module_NOTES` (Boolean). Bestehende Aufgaben-/Notizschlüssel bleiben erhalten.
Deaktivieren blendet das Modul aus, löscht aber keine Nutzdaten.

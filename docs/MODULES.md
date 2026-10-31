# FYLO-Modulschnittstelle 1

Module sind eigenständige, signierte Android-APKs mit eigener Datenhaltung und Oberfläche. FYLO erkennt sie über eine exportierte Activity und öffnet sie explizit. Es lädt keinen fremden Code in seinen eigenen Prozess. Installation und Deinstallation erfolgen über Android. Android verlangt die Bestätigung des Nutzers; Samsung kann die Installation abhängig von seinen Geräteeinstellungen einschränken.

Ein Modul deklariert an seiner Start-Activity:

```xml
<activity android:name=".ModuleActivity" android:exported="true" android:label="Mein FYLO-Modul">
    <intent-filter>
        <action android:name="com.apylon26.fylo.MODULE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data android:name="com.apylon26.fylo.API_VERSION" android:value="1" />
</activity>
```

Die Activity muss aktiviert sein und darf keine zusätzliche Aufrufer-Berechtigung verlangen. Das Modul muss eine eigene applicationId verwenden. Der Installationsdialog prüft APK-Format und API-Metadaten; Android prüft die Signatur bei der Installation. Die Metadaten sind keine Vertrauensbescheinigung. Nur bekannte Moduldateien installieren. Es gibt derzeit keinen Katalog und keine Hintergrundinstallation.

Einstellungen → Modul installieren öffnet zunächst bei Bedarf Androids Freigabe für Installationen aus FYLO. Danach erneut Modul installieren wählen und eine Modul-APK auswählen. FYLO kopiert sie mit einer Obergrenze von 512 MB in einen eng begrenzten Cacheordner und reicht sie an den Android-Installer weiter. Nach der Rückkehr wird die Modulliste aktualisiert. Module werden aus dem Einstellungen-Popup geöffnet. Über Androids App-Info können sie entfernt werden; beim nächsten Öffnen/Resume aktualisiert FYLO seine Liste.

Für ein künftiges KI-Modul sind Modellwahl und die erforderlichen Modell-Dateien bzw. der Dienstzugang gesondert festzulegen. Version 0.1.0 enthält noch kein Sprachmodell und behauptet keine KI-Fähigkeiten. Das Protokoll überträgt keine Notizen, Dateien, Zugangsdaten oder Chatdaten zwischen Modulen. Eine spätere Datenschnittstelle muss gesondert versioniert werden.


## Screens und direkter KI-Zugriff (ab 0.2.0)

FYLO speichert pro Activity-Komponente eine optionale Screen-Signatur. Die Zuordnung ist über die Systemsteuerung wählbar. Beim Öffnen eines Moduls übergibt FYLO den optionalen String-Intent-Extra `com.apylon26.fylo.SCREEN_SIGNATURE`, zum Beispiel `0002`. Die automatische vierstellige Signatur ist unveränderlich; ein Modul sollte sie als Kontextkennung und nicht als Authentifizierung verwenden. Die Systemsteuerung zeigt Bezeichnung und Signatur. Kacheln auf dem Startbildschirm bleiben optisch unbeschriftet, haben aber zugängliche Beschreibungen.

Ein KI-Modul ergänzt an derselben exportierten Start-Activity:

```xml
<meta-data android:name="com.apylon26.fylo.ROLE" android:value="assistant" />
```

Die KI-Zeile öffnet das gewählte KI-Modul direkt. Ist genau eines vorhanden, kann es automatisch geöffnet werden. Bei mehreren KI-Modulen ohne Auswahl erscheint eine Auswahl. Ohne KI-Modul zeigt FYLO einen Installationshinweis; ein Sprachmodell ist weiterhin nicht eingebaut. Für den KI-Direktzugriff wird die Signatur des aktuell sichtbaren Screens übergeben.

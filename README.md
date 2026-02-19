# Bäume in Wien – F-Droid Variante

Dies ist die **F-Droid-kompatible Version** von „Bäume in Wien".

## Unterschiede zur Google Play Version

| Feature | Play Store | F-Droid |
|---------|-----------|---------|
| Interaktive Karte (MapLibre) | ✅ | ✅ |
| Baum-Suche & Detailansichten | ✅ | ✅ |
| Blattscan (ONNX, offline) | ✅ | ✅ |
| Favoriten, Notizen, Fotos | ✅ | ✅ |
| Rally-Spiel | ✅ | ✅ |
| Community-Bäume hinzufügen | ✅ | ✅ |
| Wikipedia-Integration | ✅ | ✅ |
| Supabase Backend | ✅ | ✅ |
| **AR-Ansicht** | ✅ | ❌ (ARCore = proprietär) |
| **Google Location** | FusedLocationProvider | Android LocationManager |

## Was wurde geändert

### Entfernte Google-Abhängigkeiten

- `com.google.android.gms:play-services-location` – ersetzt durch `android.location.LocationManager`
- `com.google.ar:core` (ARCore) – entfernt, AR-Screen zeigt Hinweistext
- `io.github.sceneview:arsceneview` – entfernt (ARCore-abhängig)

### Geänderte Dateien

| Datei | Änderung |
|-------|----------|
| `app/build.gradle.kts` | Google-Deps entfernt |
| `app/src/main/AndroidManifest.xml` | ARCore-Metadata und `android.hardware.camera.ar` entfernt |
| `ui/screens/ar/ArScreen.kt` | Durch Hinweis-Screen ersetzt (kein ARCore) |
| `ui/screens/map/MapScreen.kt` | `FusedLocationProviderClient` → `LocationManager` |
| `ui/screens/community/AddTreeViewModel.kt` | `FusedLocationProviderClient` → `LocationManager` |
| `ui/screens/community/AddTreeScreen.kt` | Kein `LocationServices`-Import mehr |
| `ui/screens/rally/SoloExplorerScreen.kt` | `FusedLocationProviderClient` → `LocationManager` |
| `ui/screens/rally/RallyPlayScreen.kt` | `FusedLocationProviderClient` → `LocationManager` |

## Warum diese Struktur?

F-Droid baut Apps direkt aus dem Source Code.
Sobald proprietäre Libraries wie `play-services-location` oder `arcore` im Build enthalten sind,
wird die App abgelehnt – selbst wenn die Features optional sind.

Diese Variante ist daher ein **eigener, sauberer Ordner** ohne jegliche Google-Abhängigkeiten
(außer `com.google.android.material` – das ist Open Source und von F-Droid erlaubt).

## Bauen

```bash
./gradlew assembleRelease
```

## Ursprüngliches Projekt

Das originale Projekt mit vollständiger AR-Unterstützung liegt im Ordner `../android/`.

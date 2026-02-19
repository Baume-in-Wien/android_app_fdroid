# Bäume in Wien — Android (F-Droid)

The F-Droid-compatible build of Bäume in Wien. Identical to the Play Store version in every way except augmented reality, which depends on ARCore and is therefore unavailable. All other features — the map, leaf classifier, rallies, community trees, achievements — are fully included.

This is a separate repository rather than a product flavor so F-Droid can build it cleanly from source with no proprietary dependencies.

## What it does

**Interactive tree map** — Displays around 200,000 trees from Vienna's open data, with species-specific icons and clustering so the map stays usable. Tap any tree to see details like height, trunk circumference, year planted, and location.

**Solo explorer** — Generates missions to find specific trees within a configurable radius (500m to 5km). Walk there, snap a photo, complete the mission.

**Multiplayer rallies** — Solo rallies, student rallies for classrooms, and teacher rallies with a dashboard. Join via QR code, compete on real-time leaderboards, collect species, and build a digital herbarium along the way.

**Leaf classifier** — Point your camera at a leaf and an ONNX model tries to identify the species. Covers 200+ tree species with confidence scoring. Runs fully offline.

**Achievements** — 24+ trophies across categories like species discovery, exploration distance, rally wins, and community contributions.

**Search and favorites** — Full-text search by species, location, or district. Save your favorite trees.

**Community** — Submit new trees, upload photos, and go through a verification workflow. User profiles with role-based permissions.

**Statistics** — Track trees discovered, unique species collected, distance walked, and rally performance.

## Differences from the Play Store version

| Feature | Play Store | F-Droid |
|---|---|---|
| Tree map (MapLibre) | ✅ | ✅ |
| Search & tree details | ✅ | ✅ |
| Leaf classifier (ONNX, offline) | ✅ | ✅ |
| Favorites, notes, photos | ✅ | ✅ |
| Multiplayer rallies | ✅ | ✅ |
| Community tree submissions | ✅ | ✅ |
| Wikipedia integration | ✅ | ✅ |
| Achievements | ✅ | ✅ |
| Augmented reality | ✅ | — (requires ARCore) |
| Location provider | Google Play Services | Android LocationManager |

## Tech stack

| Area | Technology |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Font | Host Grotesk |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Persistence | Room + DataStore |
| Backend | Supabase (PostgreSQL, Auth, Realtime) |
| Tree data | Vienna Open Data WFS API + Cloudflare R2 CDN cache |
| Maps | MapLibre GL |
| Location | Android LocationManager |
| ML | ONNX Runtime (ConvNeXt Large, 366 classes) |
| AR | — (removed) |
| Networking | Retrofit + Moshi |
| Concurrency | Kotlin Coroutines + Flow |
| Navigation | Jetpack Navigation Compose |

## What was changed

Three proprietary dependencies were removed. Everything else is identical to the Play Store build.

| Removed | Replaced with |
|---|---|
| `com.google.android.gms:play-services-location` | `android.location.LocationManager` |
| `com.google.ar:core` | AR screen shows an unavailability notice |
| `io.github.sceneview:arsceneview` | — (depends on ARCore) |

Files touched: `build.gradle.kts`, `AndroidManifest.xml`, `ArScreen.kt`, `MapScreen.kt`, `AddTreeViewModel.kt`, `AddTreeScreen.kt`, `SoloExplorerScreen.kt`, `RallyPlayScreen.kt`.

## Getting started

1. Clone the repo
2. Open in Android Studio (Hedgehog or newer)
3. Set up your Supabase credentials in `SupabaseInstance.kt`
4. Build and run on a device or emulator running Android 8.0+

```bash
./gradlew assembleRelease
```

## Play Store version

The full version including AR is at [Baume-in-Wien/android_app](https://github.com/Baume-in-Wien/android_app).

## License

All rights reserved.
Made with ❤️ by Paulify Dev.

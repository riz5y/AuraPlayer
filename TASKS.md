# AuraPlayer — Task Tracker

> Apple Music-inspired Android player · Kotlin · Jetpack Compose · Media3 · AdMob

---

## ✅ Phase 0 — Project Setup
- [x] Define architecture (MVVM + Clean Architecture)
- [x] Folder structure designed
- [x] Dependency list finalized
- [ ] Create Android project in Android Studio (open folder in AS)
- [x] Configure `libs.versions.toml` (version catalog)
- [x] Add all Gradle dependencies
- [x] Set `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- [x] Enable `buildFeatures { compose = true }`
- [ ] Add `google-services.json` (Firebase/AdMob) ← **manual step**

---

## ✅ Phase 1 — Data Layer
- [x] `Song.kt` domain model
- [x] `SongEntity.kt` Room entity + mapper
- [x] `AuraDatabase.kt` Room DB singleton
- [x] `SongDao.kt`
- [x] `SongRepositoryImpl.kt` (MediaStore scan + Room cache)
- [x] Hilt `DatabaseModule.kt` + `RepositoryModule.kt`

---

## ✅ Phase 2 — Playback Service
- [x] `AuraMediaSessionService.kt` (Media3 + ExoPlayer)
- [x] `AuraMediaControllerManager.kt` (UI bridge)
- [x] `NowPlayingViewModel.kt` + `PlayerState` + `DynamicColors`
- [x] `MediaModule.kt` Hilt wiring
- [x] Manifest: foreground service, permissions
- [x] Audio focus + noisy audio (auto via ExoPlayer)

---

## 🔲 Phase 3 — ViewModel / State
- [ ] `NowPlayingViewModel.kt` + `PlayerState` data class
- [ ] `DynamicColors` state + `DynamicColorExtractor.kt`
- [ ] `LibraryViewModel.kt` (song list + search)

---

## ✅ Phase 4 — Theme & Screens
- [x] `AuraTheme.kt`, `Color.kt`, `Typography.kt`
- [x] `BounceButton.kt`, `SpringAlbumArt.kt`, `AuraSeekBar.kt`
- [x] `LibraryScreen.kt` + `LibraryViewModel.kt`
- [x] `MiniPlayerBar.kt` (progress bar + 28dp corners + opaque gradient)
- [x] `ExpandablePlayerSheet.kt`
- [x] `NowPlayingScreen.kt`
- [x] `AuraNavigation.kt` (3-tab bottom nav)
- [x] `MainActivity.kt`

---

## 🔲 Phase 5 — Screens
- [ ] `LibraryScreen.kt` (LazyColumn song list)
- [ ] `MiniPlayerBar.kt` (floating, 72dp peek)
- [ ] `ExpandablePlayerSheet.kt` (progress interpolation)
- [ ] `NowPlayingScreen.kt` (full-screen, dynamic BG)
- [ ] `TransportControls.kt`

---

## ✅ Phase 6 — AdMob Integration
- [x] Add AdMob dependency + App ID to manifest
- [x] `AdManager.kt` singleton (load/show logic)
- [x] **Splash Interstitial** — shown once on cold start via `SplashScreen.kt`
- [x] **Post-Splash Interstitial** — preloads after splash dismissal
- [x] **Banner Ad** — anchored bottom of LibraryScreen (`BannerAdView.kt`)
- [x] Preload next interstitial after show
- [x] Respect `isPlaying` — never show interstitial mid-song

---

## ✅ Phase 7 — Polish & Permissions
- [x] ProGuard/R8 rules for Media3, AdMob, Room, Coil, Hilt (`proguard-rules.pro`)
- [x] Runtime Audio & Storage permission handling (`AudioPermissionGate.kt`)
- [x] Edge-to-edge system bars configuration
- [x] Dynamic Palette background extraction optimization
- [x] High-performance reactive SQLite storage engine (`AuraDatabaseHelper.kt`)

---

## ✅ Phase 8 — Release & Build Verification
- [x] Root build scripts and Gradle wrapper configured
- [x] Clean architecture manual DI (`AuraContainer.kt`)
- [x] Debug APK assembled successfully (`app-debug.apk`)
- [x] Pushed to GitHub repository

---

## 💰 AdMob Placement Strategy

### Placement Map
```
App Cold Start
    │
    ▼
SplashScreen (1.5s logo + lottie)
    │
    ▼
[Interstitial Ad #1] ← full-screen, shown ONCE per cold start
    │
    ▼
MainActivity → LibraryScreen
    │                │
    │         [Banner Ad] ← anchored bottom, always visible in Library
    │                        hidden when sheet is fully expanded
    ▼
User taps song → playback starts
    │
    ▼
After ~3 song skips OR 20 min session
    │
    ▼
[Interstitial Ad #2] ← shown between songs only (never during playback)
```

### Rules
| Rule | Reason |
|---|---|
| Never interrupt active playback | UX + AdMob policy |
| Interstitial only on pause/skip | Natural break points |
| Banner hidden on full NowPlaying | Avoid visual clutter |
| Max 1 interstitial per 5 min | Avoid policy violations |
| Preload next ad immediately after show | Zero wait time |

# AuraPlayer 🎵

> A modern, offline Android music player inspired by Apple Music — built with Kotlin, Jetpack Compose, and Media3.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Playback:** AndroidX Media3 (ExoPlayer) + MediaSessionService
- **Storage:** MediaStore API + Room
- **Images:** Coil 3
- **DI:** Hilt
- **Ads:** Google AdMob

## Architecture
MVVM + Clean Architecture (Domain / Data / UI layers)

## Setup
1. Clone repo
2. Add your `google-services.json` to `/app`
3. Replace AdMob App ID in `AndroidManifest.xml`
4. Build & run (minSdk 26)

## Progress
See [TASKS.md](./TASKS.md)

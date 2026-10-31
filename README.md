# Streak Keeper 🔥

An Android app for maintaining TikTok streaks with your friends through personalized custom stickers.

## Features

- **Per-contact stickers** — assign a different emoji sticker to every person (preset grid + custom emoji + notes)
- **Daily streak tracking** — individual streak counts with automatic reset if a day is missed
- **Scheduled reminders** — background notifications via WorkManager, reliable even when the app is closed or the screen is locked
- **Fast TikTok access** — one tap from the app or notification opens TikTok directly
- **Honest state tracking** — every task shows one of three states: *Pending → Opened in TikTok → Confirmed sent*
- **Fully local** — all data stays on your device (Room database), no backend, no cloud, no accounts

## Tech stack

Kotlin • Jetpack Compose • Material 3 (dark theme) • Room • WorkManager • Android Notifications

## Build

Requires Android Studio (or JDK 17 + Android SDK 35).

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # → app/build/outputs/apk/release/app-release-unsigned.apk
```

## Honest TikTok limitation

TikTok Personal accounts have **no official API** for opening a specific private conversation or automatically sending Custom Stickers — especially while the device is locked. Streak Keeper does not fake this. Instead it:

1. Reminds you on schedule (background WorkManager notifications)
2. Opens the TikTok app in one tap from the app or notification
3. Requires your explicit "Sent ✓" confirmation before recording a streak day

Nothing is ever marked as sent automatically.

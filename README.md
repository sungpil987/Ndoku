# 냥도쿠 (Nyangdoku)

A cozy Android cat logic puzzle. Place one cat in each row, column, and colored region. Cats may not touch, including diagonally.

## Build

The project targets Android 16 (API 36), requires JDK 17 and Gradle 8.11.1, and has no third-party runtime dependencies.

```sh
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds and uploads this APK as an artifact on pushes to `main`.

## Current slice

- First-run tutorial, playable 4×4 puzzle, X notes, rule checks, limited hints, undo, score and local save.
- The two cat photos supplied by the project owner are used as in-game cat portraits.
- Rewarded hints and other monetization are planned for a later update after the initial game loop is established.

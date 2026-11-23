# Hexyn Launcher (glass / iOS-inspired)

An ad-free Android launcher in Kotlin + Jetpack Compose with a frosted-glass look.

## Features
- Home pages with a glass clock widget, squircle icons, page dots, Search pill and glass Dock
- App Library page (last page): apps grouped into glass category cards, tap to open
- Dynamic Island style pill at the top: tap it, or plug in a charger, and it expands
- iOS-style status bar: time, Wi-Fi / mobile data, battery with charging colour
- Control Center: swipe down on the RIGHT side of the home screen
  - Real: flashlight, brightness, volume
  - Shortcuts to system screens: Airplane, Mobile data, Wi-Fi, Bluetooth, Sound, Camera,
    Calculator, Timer, Alarm, Display, Settings
- Spotlight search: swipe down on the LEFT side of the home screen (or tap Search)
- Long-press an icon: Add to Dock / App info / Remove app
- Zoom-from-icon app launch, press animation, wallpaper blur behind overlays (Android 12+)
- No ads, no tracking, no internet permission

## Honest limits (Android vs iOS)
- The real Android status bar can't be replaced by an app. This launcher hides it and draws its own.
  Swipe from the very top edge to peek at the real one (notifications).
  Set `CUSTOM_STATUS_BAR = false` in `MainActivity.kt` to keep the normal one.
- The Dynamic Island is a drawn pill. It does not follow your camera hole. Adjust
  `ISLAND_WIDTH_DP`, `ISLAND_HEIGHT_DP`, `ISLAND_TOP_DP` in `MainActivity.kt` to line it up.
- Apps can't toggle Wi-Fi / Bluetooth / Airplane mode on modern Android, so those buttons open
  the system screen. Brightness asks for the "Modify system settings" permission once.
- Fonts are the phone's default font (Apple's font is licensed). Icons are your apps' own icons.

## Build WITHOUT a laptop (GitHub builds the APK for you)
1. On your phone, create a free GitHub repo (for example `hexyn-launcher`).
2. Put all files of this project into the repo root (the `.github` folder must keep its path).
3. Open the repo > Actions tab. "Build APK" runs on every push (or press "Run workflow").
4. After about 5-8 minutes: repo > Releases > latest build > download `app-debug.apk`.
5. Open the APK, allow "install unknown apps" for your browser, install.
6. Press Home > Hexyn Launcher > Always.

If the build fails, open the failed run, copy the red error lines, and send them to Claude.

## Build with Android Studio (if you get a laptop)
File > Open > the project folder > wait for Gradle sync > Run.

## Customize
- `MainActivity.kt`: `Cfg` settings (status bar, island, grid, dock size)
- `Glass.kt`: the glass material
- `Home.kt`: home layout, dock, widget, status bar, island, icon look (`IconShape`)
- `ControlCenter.kt`, `Spotlight.kt`, `AppLibrary.kt`

## New in 0.6
- Glass: dark or light glass theme, bottom reflection band, and a "Reduce effects" switch for slow phones.
- Icons: size up to 76, 4/5/6 columns (icons auto-fit their cell).
- Floating dock with a stronger glass look.
- Folders open with a spring zoom animation.

## New in 0.5
- Swipe down on the LEFT side = Notification Center, RIGHT side = Control Center. Both are full layers (home screen hidden). Spotlight: Search pill or swipe down in the lower 40%.
- Long-press the calendar or clock widget to edit sizes, colours and style.
- Dynamic Island shows incoming notifications (app icon), the music app icon, "on call", and camera/mic/speaker activity.
- Notification Center needs Notification access (Settings > Music in the island).

## New in 0.4
- Fixed signing key (`app/debug.keystore`): new builds install over the old app. Keep your repo PRIVATE.
- Glassier panels: diagonal specular highlight and brighter rim light.
- Real app icon, and Settings > Wallpapers: 6 built-in gradient wallpapers for home and lock screen.

## New in 0.3
- Launcher Settings: long-press the Calendar or Clock widget, or tap the gear in Control Center.
  Change icon shape/size, columns, glass strength, widgets, status bar, and the Dynamic Island size/position.
- Music in the Dynamic Island (album art + bouncing bars; tap for play/pause/next/previous).
  Needs Notification access: Settings > Open notification access > turn on Hexyn Launcher.
  On Android 13+: Settings > Apps > Hexyn Launcher > three dots > Allow restricted settings first.
- More iOS-like look: squircle icons with gloss, calendar widget, analog clock widget.

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

# 📱 Virtual Alert Slider — App Overview & Architecture Guide

> **Context for AI Models:** This document describes the purpose, UI design, core audio/system mechanics, and software architecture of **Virtual Alert Slider**, an open-source Android utility built with Kotlin and Jetpack Compose. Use this file to understand the system and suggest features, debug issues, or refactor components.

---

## 1. 🎯 What is Virtual Alert Slider?

### The Problem It Solves:
On devices with a physical 3-stage alert slider (notably **OnePlus / OxygenOS**):
1. **Physical Jamming:** Dust, lint, or mechanical drops often jam the slider in the **SILENT** position.
2. **Kernel Audio Lock:** When the physical switch is jammed in Silent, vendor Android kernels continually mute the audio stream (`STREAM_RING` and `STREAM_NOTIFICATION`). Standard volume keys cannot unmute the phone.
3. **Devices Missing an Alert Slider:** Non-OnePlus devices (**Samsung Galaxy, Google Pixel, Xiaomi, Motorola**) have no physical slider and require navigating deep menus or volume panels to switch ringer profiles.

### The Solution:
**Virtual Alert Slider** provides a digital, software-driven replacement and bypass engine. It:
- Gives users a tactile, 3-position vertical slider on screen.
- Overcomes stuck hardware switches by programmatically forcing audio stream unmutes, resetting Do Not Disturb (DND) filters, and updating OEM system tables.
- Provides 1-tap toggling through a **Desktop Home Screen Widget** and a **Quick Settings Notification Shade Tile**.

---

## 2. 🧰 Tech Stack & Prerequisites

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0+ (Coroutines, StateFlow, Flow) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **Persistence** | Jetpack DataStore Preferences (Lightweight reactive key-value storage) |
| **System APIs** | Android `AudioManager`, `NotificationManager` (Notification Policy / DND), `TileService`, `AppWidgetProvider` |
| **Target SDK** | Android 15 (API 35), Min SDK: Android 8.0 (API 26) |

---

## 3. 📂 Project File Structure & Responsibilities

```text
com.virtualalertslider.app/
│
├── domain/                                 # Pure Business Logic & Domain Models
│   ├── model/
│   │   ├── AlertMode.kt                   # Enum: RING, VIBRATE, SILENT (+ helper methods)
│   │   ├── SliderSettings.kt              # Data class: haptics, theme, startup mode, bypass flag
│   │   └── ThemePreference.kt             # Enum: SYSTEM, LIGHT, DARK
│   └── usecase/
│       ├── GetAlertModeUseCase.kt         # Gets current ringer mode
│       ├── SetAlertModeUseCase.kt         # Sets ringer mode via AudioModeManager
│       ├── ObserveAlertModeUseCase.kt     # Reactive Flow of system + override mode
│       ├── GetSettingsUseCase.kt          # Reads settings from DataStore
│       └── SaveSettingsUseCase.kt         # Updates settings in DataStore
│
├── system/                                 # Low-Level Android System Bridges
│   ├── AudioModeManager.kt                # Direct manipulation of AudioManager & NotificationManager
│   ├── NotificationPolicyManager.kt       # Checks & requests Do Not Disturb (DND) policy access
│   └── RingerModeReceiver.kt              # BroadcastReceiver for system ringer & DND changes
│
├── data/                                   # Data Layer
│   ├── preferences/
│   │   └── PreferencesManager.kt         # Jetpack DataStore wrapper (saves bypass, theme, haptics)
│   └── repository/
│       ├── AlertSliderRepository.kt       # Repository interface
│       └── AlertSliderRepositoryImpl.kt   # Bridges system events, user overrides, and DataStore
│
├── tile/
│   └── VirtualAlertSliderTile.kt          # Android Quick Settings TileService for notification shade
│
├── widget/
│   └── VirtualAlertSliderWidget.kt        # AppWidgetProvider for Home Screen desktop widget
│
└── ui/                                     # Jetpack Compose Presentation Layer
    ├── components/
    │   ├── VerticalAlertSlider.kt         # Interactive 3-stop draggable/tappable physical-feel slider
    │   ├── AlertModeCard.kt               # Active mode card with dynamic colored badge & status text
    │   ├── HeaderCard.kt                  # App title, DND permission status chip, and settings button
    │   └── PermissionBanner.kt            # Warning banner shown when DND permission is missing
    ├── screens/
    │   ├── main/
    │   │   ├── MainScreen.kt              # Primary screen containing slider, mode card, and quick chips
    │   │   └── MainViewModel.kt           # StateFlow UI state holder for MainScreen
    │   └── settings/
    │       ├── SettingsScreen.kt          # Settings: DND check, haptics, bypass switch, theme, startup mode
    │       └── SettingsViewModel.kt       # StateFlow UI state holder for SettingsScreen
    └── theme/
        ├── Color.kt                       # Mode accents (RingBlue, VibrateAmber, SilentPink, Slate darks)
        ├── Theme.kt                       # Material 3 dark/light dynamic theme setup
        └── Type.kt                        # Typography scale definitions
```

---

## 4. 🎨 UI Structure & Visual Experience

The UI uses a **Dark Obsidian / Slate** aesthetic inspired by modern Android flagships, featuring mode-specific color accents:
- **Ring Mode:** Electric Blue (`#3B82F6`)
- **Vibrate Mode:** Amber Gold (`#F59E0B`)
- **Silent Mode:** Rose Pink (`#EC4899`)

### 4.1. Screens:

#### A. Main Screen (`MainScreen.kt`)
1. **Header Card (`HeaderCard.kt`):** Displays the app name, an indicator badge showing whether DND permission is granted, and an icon button navigating to `SettingsScreen`.
2. **Permission Warning Banner (`PermissionBanner.kt`):** Appears automatically if Android's Do Not Disturb permission is missing. Tapping opens Android's native Notification Policy Settings.
3. **Active Mode Showcase Card (`AlertModeCard.kt`):** Displays the active ringer profile with an animated icon badge, mode title ("RING", "VIBRATE", "SILENT"), and description (e.g., *"Calls & notifications sound loud"*). Smooth cross-fade animation transitions between modes.
4. **Vertical Alert Slider (`VerticalAlertSlider.kt`):**
   - **Height / Dimensions:** 380dp tall x 280dp wide elevated container card.
   - **Track:** Vertical gradient track with 3 snap nodes (`RING` at top 0%, `VIBRATE` at middle 50%, `SILENT` at bottom 100%).
   - **Draggable Knob:** Floating metallic radial knob with icon that tracks user touch gestures (`detectDragGestures` and `detectTapGestures`).
   - **Physics:** Uses Jetpack Compose `animateFloatAsState` with bouncy spring specs (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`).
   - **Haptic Feedback:** Vibrates the device (`HapticFeedbackConstants.CONTEXT_CLICK`) on knob snap.
5. **Quick Switch Chips:** Horizontal row of 3 `FilterChip` buttons underneath the slider allowing direct 1-tap mode switching without dragging.

#### B. Settings Screen (`SettingsScreen.kt`)
1. **System & Permissions:** Real-time DND access check with green checkmark / red lock icon. Tapping opens system settings.
2. **Preferences:**
   - **Haptic Feedback:** Switch to enable/disable snap vibration.
   - **Bypass Stuck Slider:** Master toggle for the hardware bypass engine.
   - **Theme Preference:** Dropdown to switch between System Default, Dark, and Light themes.
   - **Default Startup Mode:** Dropdown to select which mode loads on fresh boot (`RING`, `VIBRATE`, `SILENT`).
3. **Integrations & Shortcuts Info:** Step-by-step setup cards for the Quick Settings Tile and Home Screen Widget.
4. **About:** Version info and architecture summary.

---

## 5. ⚡ External UI Surfaces (Widget & Tile)

### 5.1. Desktop Home Screen Widget (`VirtualAlertSliderWidget.kt`)
- Built using Android `AppWidgetProvider` and XML `RemoteViews` (`widget_alert_slider.xml`).
- Renders an interactive 3-button bar on the user's home screen.
- Active mode button lights up with a rounded highlight background; inactive buttons remain dimmed.
- Tapping any button sends a broadcast intent (`ACTION_SET_MODE_PREFIX + mode.id`), instantly switching the sound profile in the background without needing to open the app.
- Listens to system broadcasts to refresh its visual state if the user changes volume via hardware keys.

### 5.2. Quick Settings Tile (`VirtualAlertSliderTile.kt`)
- Extends Android `TileService` (`android.service.quicksettings.TileService`).
- Placed directly in the Android pull-down notification shade.
- **1-Tap Cyclic Toggle:** Tapping the tile cycles through:
  $$\text{RING} \longrightarrow \text{VIBRATE} \longrightarrow \text{SILENT} \longrightarrow \text{RING}$$
- Updates tile label, subtitle, and icon in real time.

---

## 6. ⚙️ Core System Logic & Audio Engine

All low-level system calls reside in `AudioModeManager.kt`.

### 6.1. The 3 Alert Modes (`AlertMode.kt`)

| Mode | System Ringer Mode | Do Not Disturb (DND) Filter | Audio Streams |
|---|---|---|---|
| **`RING`** | `AudioManager.RINGER_MODE_NORMAL` | `INTERRUPTION_FILTER_ALL` (Off) | Unmutes `STREAM_RING` & `STREAM_NOTIFICATION`; sets volume to 75% |
| **`VIBRATE`** | `AudioManager.RINGER_MODE_VIBRATE` | `INTERRUPTION_FILTER_ALL` (Off) | Mutes `STREAM_RING`; allows haptic motor vibrations |
| **`SILENT`** | `AudioManager.RINGER_MODE_SILENT` | `INTERRUPTION_FILTER_ALARMS` | Mutes ring & notification streams; allows morning alarms to still sound |

### 6.2. How the Hardware Slider Bypass Engine Operates
When a physical slider is jammed mechanically on Silent:
1. **Unmuting Suppressed Streams:** Android audio streams are forcibly unmuted using `audioManager.adjustStreamVolume(STREAM_RING, ADJUST_UNMUTE, 0)`.
2. **Clearing DND Filter:** Resets DND from Alarms/Total Silence back to `INTERRUPTION_FILTER_ALL`.
3. **OnePlus Global Settings Hook:** The app writes to internal OEM settings keys:
   - `three_Key_mode`: `1` (Silent), `2` (Vibrate), `3` (Ring).
   - `alert_slider_mode` and `oplus_mode_ringer`.
4. **State Override Priority (`AlertSliderRepositoryImpl.kt`):**
   When `bypassStuckSlider` is enabled in settings, the repository combines system ringer broadcasts with the user's last virtual selection. If a stuck hardware switch triggers an unwanted fallback broadcast to Silent, the app retains and enforces the user's digital selection.

---

## 7. 🔐 Permissions & System Integration

1. **`ACCESS_NOTIFICATION_POLICY`:**
   - Declared in `AndroidManifest.xml`.
   - Required by Android (API 23+) to programmatically toggle Do Not Disturb (`setInterruptionFilter`).
   - The user must grant this in: *Settings > Apps > Special App Access > Do Not Disturb Access*.
2. **Optional ADB System Privilege (`WRITE_SECURE_SETTINGS`):**
   - For OnePlus/OxygenOS devices with deeply rooted kernel locks, users can optionally grant:
     ```bash
     adb shell pm grant com.virtualalertslider.app android.permission.WRITE_SECURE_SETTINGS
     ```
   - Allows writing directly to `Settings.Global.putInt(contentResolver, "three_Key_mode", value)` to satisfy OEM hardware daemons.

---

## 8. 🔄 Reactive Data Flow Summary

```text
[Hardware Volume Keys / System Events] 
           │
           ▼
[RingerModeReceiver] ────────┐
                             │
[User Drag / Tap on UI]      │
           │                 ▼
           ▼            [AlertSliderRepositoryImpl]
[SetAlertModeUseCase]        │
           │                 ▼
           ▼            [AudioModeManager] ──► [Android AudioManager & DND Policy]
[PreferencesManager]         │
  (DataStore: selectedMode)  ▼
           │            [StateFlow / Flow]
           └────────────► [MainViewModel / SettingsViewModel]
                             │
                             ▼
                        [Jetpack Compose UI] + [Widget] + [QS Tile]
```

---

## 9. 💡 Key Takeaways for Any AI Working on this Codebase
- **Never mutate audio streams without checking DND access:** Muting/unmuting or changing ringer modes on Android 8+ can throw a `SecurityException` if DND access is missing. Always handle through `AudioModeManager.kt` or `NotificationPolicyManager.kt`.
- **Maintain 3-point synchronization:** Whenever alert mode changes, three visual surfaces must reflect the state:
  1. Compose `MainScreen` (`VerticalAlertSlider` knob & chips)
  2. Desktop Home Screen Widget (`VirtualAlertSliderWidget.updateAllWidgets(context)`)
  3. Quick Settings Tile (`VirtualAlertSliderTile`)
- **Clean Architecture separation:** UI screens talk only to ViewModels; ViewModels talk to Use Cases; Use Cases talk to `AlertSliderRepository`; low-level hardware actions are encapsulated in `system/AudioModeManager.kt`.

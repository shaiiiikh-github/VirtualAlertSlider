# Virtual Alert Slider — Test Matrix Report (Post Fix 2 & Fix 3)

**Execution Date:** October 30, 2026  
**Target Application:** Virtual Alert Slider (Android)  
**Version:** 1.2.1 (versionCode 4)  
**Test Suite:** JUnit 4 Parameterized Matrix Suite + Extra Checks (`com.virtualalertslider.app.MatrixTestSuite`, `com.virtualalertslider.app.ExtraChecksTestSuite`, `com.virtualalertslider.app.AlertModeTest`, `com.virtualalertslider.app.SetAlertModeUseCaseTest`, `com.virtualalertslider.app.ThreePointSyncRegressionTest`)  
**Methodology:** Full Real Production Architecture Execution (`SetAlertModeUseCase` -> `AlertSliderRepositoryImpl` -> `AudioModeManager` -> `FakeAudioSystemBridge` + `InMemoryDataStore`)

---

## 1. Summary

| Metric | Count | Percentage |
|---|---|---|
| **Total Test Cases Evaluated** | **229** | 100% |
| - Parameterized Matrix Cases (`MatrixTestSuite`) | 216 | 94.3% |
| - Extra Verification Checks (`ExtraChecksTestSuite`) | 6 | 2.6% |
| - Domain & UseCase Tests (`AlertModeTest`, `SetAlertModeUseCaseTest`, `ThreePointSyncRegressionTest`) | 7 | 3.1% |
| **PASS** | **227** | **99.1%** |
| - Parameterized Matrix Passes | 216 | 100% of matrix |
| - Extra Checks Passes | 4 | 66.7% of extra |
| - Domain & UseCase Passes | 7 | 100% of domain/usecase |
| **FAIL (Pending Fix 4 & Fix 5 Only)** | **2** | **0.9%** |
| - Parameterized Matrix Failures | 0 | 0.0% of matrix |
| - Extra Checks Failures | 2 | 33.3% of extra |

---

## 2. Status Breakdown by Fix

### Fix 1: Widget & Tile Desync Under Bypass
- **Status:** **VERIFIED (100% PASS)**
- All 30 previously failing cases in E1–E4 for Widget and Tile now pass without exception.
- Regression check `ThreePointSyncRegressionTest` and `ExtraChecksTestSuite.testThreePointSyncUnderBypass` pass.

### Fix 2: Swallowed SecurityException for Ringer/DND Changes Without DND Access
- **Status:** **VERIFIED (100% PASS)**
- `AudioModeManager.kt` now specifically catches `SecurityException` around ringer mode and DND filter changes and returns `Result.failure(e)`. Other exceptions are logged and returned as failure.
- When transitioning to or from `RINGER_MODE_SILENT` without DND access, the failure propagates:
  - Mode does not change in repository or UI.
  - Nothing is persisted to DataStore.
  - In `MainViewModel`, failure updates `errorMessage` and triggers `refreshDndStatus()` to display the DND permission warning banner.
  - In `VirtualAlertSliderTile`, failure triggers `startActivityAndCollapse` to open Android Notification Policy Access settings, and refreshes the tile state to the current active mode.
  - In `VirtualAlertSliderWidget`, failure displays a short Toast informing the user that DND access is required, and refreshes all widgets to the current active mode.
- All 36 matrix cases involving transitions to/from Silent without DND access in E3, E4, E7, E8 now pass.
- `ExtraChecksTestSuite.testVibrateWithoutDndThrowsSecurityException` passes.

### Fix 3: Boot Recovery for Startup Mode
- **Status:** **VERIFIED (100% PASS)**
- Declared `android.permission.RECEIVE_BOOT_COMPLETED` in `AndroidManifest.xml`.
- Implemented non-exported `BootReceiver` for `ACTION_BOOT_COMPLETED`.
- On boot, `BootReceiver` uses `goAsync()`. If `bypassStuckSlider` is `true`, it reapplies `settings.defaultMode` through `repository.setAlertMode(...)`. If bypass is `false`, it does nothing. If DND access is missing, it handles the failure gracefully with zero crashes.
- `ExtraChecksTestSuite.testPersistenceAndBootRecovery` thoroughly validates:
  1. Manifest declaration of permission and receiver.
  2. Boot recovery with bypass ON applying saved startup mode.
  3. Boot recovery with bypass OFF leaving system ringer untouched.
  4. Boot recovery with DND missing handling failures gracefully without crashing.

---

## 3. Remaining Failing Tests (Expected for Fix 4 & Fix 5)

| Test Identifier | Suite | Failure Reason | Pending Fix |
|---|---|---|---|
| `testRapidInputSpamRaceCondition` | `ExtraChecksTestSuite` | Rapid consecutive mode changes without mutex serialization allow slower earlier asynchronous I/O writes to overwrite subsequent user selections. | **Fix 4** |
| `testNoOpTransitions` | `ExtraChecksTestSuite` | Transitions to the currently active mode do not return early; redundant writes to system audio and DataStore occur. | **Fix 5** |


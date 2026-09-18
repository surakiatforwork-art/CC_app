# Verification

## Automated checks

- `GeometryTest`: 1,500 randomized placements across document and scene aspect ratios; no corners outside the scene. Reject crossed corners. Keep extreme gestures visible.
- `PipelineTest` (Android instrumentation): detect a synthetic long slip using native OpenCV, correct perspective, composite, save JPEG and PNG through MediaStore, decode both outputs and check 1800 × 2400 dimensions.
- `EditorFlowTest` (Compose UI): load an image into the real ViewModel, confirm crop, randomize scene, open export and save using UI buttons. Capture the final screen.
- `lintDebug`: Android static analysis.

Test environment: JDK 17, Android SDK 35 build tools, Android 16 / API 36.1 emulator. App minimum API 29.

Android 16 test compatibility: Espresso 3.7.0 avoids the removed reflective `InputManager.getInstance` API. See [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test).

## Still needs physical-device verification

- Real camera capture, flash, tap-to-focus, permission denial and orientation changes.
- Accuracy across the user's real reference photos, shadows, multiple overlapping papers and narrow extensions.
- Full-resolution memory use on low-RAM devices.
- Long editing sessions and interrupted exports.
- Two-finger gesture feel and corner precision on a phone.

The automated images are synthetic fixtures; passing these tests does not establish detection accuracy for every shop scene.

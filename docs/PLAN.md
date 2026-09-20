# OrderSlipScene

Source: ChatGPT conversation “ออกแบบแอปสแกนภาพ”, read 2026-09-18.

## First usable MVP
- Native Kotlin / Compose; Android 10+; offline, no accounts or upload.
- CameraX camera and system photo picker.
- OpenCV multiple document candidates, editable four corners, rotate, reset, perspective correction.
- Image + JSON scene repository, category filter, constrained random placement.
- Drag / pinch / rotate and four-corner perspective editor.
- Full scene resolution JPEG / PNG export through MediaStore.
- Import local backgrounds (reference photos contain existing slips and timestamps; they are references, not clean scene assets).

## Follow-up
- Blur and grain matching with independent toggles; brightness, white balance and contact-shadow matching are now available offline.
- Visual template anchor/range setup, template pack import/export.
- Process-death session recovery and large-image tiled processing.
- Broader device, camera and reference-image detection validation.

## Layers
UI and CameraX → EditorViewModel → CV / geometry / scene repository / renderer / storage.
Detection works on a downsampled copy. Normalized corners map to the original at crop time.
Preview is independent from final export. Final output dimensions equal the original background.

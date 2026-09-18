ได้เลย ผมจะรวม **B + C** ให้เป็นชุดเดียว คือทั้ง **Prompt ภาษาไทยพร้อมส่งให้ Codex / Claude Code** และ **โครงสร้างโปรเจกต์ + pseudo-code ระดับลงมือทำจริง** โดยยึดตามภาพตัวอย่างทั้งหมดที่คุณส่งมา

## Prompt สำหรับส่งให้ Codex / Claude Code

```text
ฉันต้องการสร้างแอป Android สำหรับสร้างภาพ “ใบฝากสั่ง” ให้อยู่ในฉากหลังหน้างานหลายรูปแบบ

ให้พัฒนาเป็น Native Android โดยใช้:

- Kotlin
- Jetpack Compose
- CameraX
- OpenCV Android
- Android Bitmap/Canvas สำหรับ compositing
- ใช้ MVVM
- รองรับ Android รุ่นปัจจุบันและย้อนหลังอย่างเหมาะสม

========================
เป้าหมายหลักของแอป
========================

ผู้ใช้จะถ่ายภาพใบฝากสั่งจริง หรือเลือกรูปจาก Gallery

ใบฝากสั่งมีลักษณะเป็นกระดาษแนวยาว สีขาว มีตารางสินค้า มีทั้ง:
- แบบว่าง
- แบบเขียนแล้ว
- มีลายมือ
- มีตราประทับ
- มีตัวเลขเขียนทับ
- บางครั้งอาจมีส่วนกระดาษต่อด้านล่าง
- รูปแบบข้อมูลสินค้าอาจเปลี่ยนทุกเดือน

ดังนั้นห้าม hardcode ข้อความ รายการสินค้า หรือ OCR เป็นเงื่อนไขหลักในการตรวจจับเอกสาร

ให้ตรวจจับจากรูปร่างของเอกสารเป็นหลัก

แอปต้อง:
1. ถ่ายภาพหรือเลือกภาพใบฝากสั่ง
2. ตรวจจับขอบกระดาษอัตโนมัติ
3. หา 4 มุมของใบฝากสั่ง
4. ให้ผู้ใช้แก้ตำแหน่ง 4 มุมเองได้
5. Perspective correction
6. Crop ใบฝากสั่งออกมา
7. นำใบฝากสั่งไปวางบนฉากหลัง Template
8. สุ่มฉากหลังได้
9. สุ่มตำแหน่ง/ขนาด/องศาในช่วงที่สมจริง
10. ผู้ใช้สามารถลาก หมุน ย่อ ขยาย และปรับ perspective ได้เอง
11. Preview ภาพสุดท้าย
12. Save เป็น JPG/PNG

ไม่ต้องทำ timestamp overlay ในเวอร์ชันนี้

========================
ลักษณะฉากหลัง
========================

จากภาพตัวอย่างจริง ฉากหลังแบ่งเป็นกลุ่มดังนี้:

1. Freezer / ตู้แช่
- มองเห็นสินค้า
- มีกระจก
- มีแสงสะท้อน
- มีตะแกรงหรือขอบตู้
- ฉากหลังอาจเบลอเล็กน้อย

2. Plain Surface
- โต๊ะ
- เคาน์เตอร์
- ผนัง
- พื้นสีครีม/ขาว/เทา
- อาจมีเอกสารอีกใบโผล่จากขอบภาพ

3. Plastic / Frosted Surface
- ถุงพลาสติก
- พลาสติกขุ่น
- มี texture
- มีลายหรือข้อความอยู่ด้านหลัง

4. Promotion / Poster / Payment Sign
- ป้ายโปรโมชั่น
- ป้ายสินค้า
- ป้ายราคา
- ป้ายรับชำระ
- มีกราฟิก ตัวหนังสือ หรือรูปสินค้าเป็นพื้นหลัง

Template แต่ละตัวต้องไม่ hardcode อยู่ใน source code
ให้รองรับการเพิ่ม Template ใหม่ด้วย asset + JSON metadata

========================
Scene Template Model
========================

Template แต่ละภาพต้องมี metadata เช่น:

{
  "id": "promo_001",
  "category": "promotion",
  "image": "promo_001.jpg",

  "paperPlacement": {
    "centerX": 0.50,
    "centerY": 0.53,

    "widthMin": 0.25,
    "widthMax": 0.32,

    "rotationMin": -3.0,
    "rotationMax": 3.0,

    "offsetX": 0.03,
    "offsetY": 0.03
  },

  "effects": {
    "shadow": false,
    "brightnessMatch": true,
    "softness": 0.5,
    "grain": 0.2
  }
}

พิกัดต้องเป็น normalized coordinate 0.0 - 1.0
เพื่อไม่ขึ้นกับ resolution ของภาพ

========================
การวางใบฝากสั่ง
========================

ภาพตัวอย่างจริงส่วนใหญ่:

- ใบฝากสั่งอยู่แนวตั้ง
- อยู่ใกล้กึ่งกลาง
- อาจเยื้องเล็กน้อย
- เอียงน้อย
- ไม่ควรเอียงหรือ perspective รุนแรง
- กระดาษมีขนาดค่อนข้างใหญ่เมื่อเทียบกับภาพ
- ต้องเห็นใบฝากสั่งเกือบครบหรือครบทั้งใบ

ค่าเริ่มต้นที่แนะนำ:

rotation:
ประมาณ -3 ถึง +3 องศา

position:
สุ่มรอบ anchor ของ Template เท่านั้น

scale:
สุ่มเล็กน้อย เช่น ±5-10%

ห้าม random แบบกว้างจนภาพดูไม่สมจริง

========================
Document Detection
========================

ใช้ OpenCV

Pipeline แนะนำ:

Bitmap
↓
resize สำหรับ detection
↓
grayscale
↓
Gaussian blur
↓
adaptive threshold หรือ Canny
↓
morphology close
↓
findContours
↓
กรอง contour ตาม:
- area
- rectangularity
- aspect ratio
- convexity
↓
approxPolyDP
↓
หา candidate ที่มี 4 จุด
↓
เรียงมุม:
top-left
top-right
bottom-right
bottom-left
↓
Perspective transform

ใบฝากสั่งเป็นเอกสารแนวยาวมาก
อย่ากรองด้วยอัตราส่วนเอกสาร A4

รองรับทั้ง:
- เอกสารตรง
- เอกสารเอียง
- ฉากหลังมีลาย
- ฉากหลังมีสินค้า
- มีเงาคนถ่าย
- มีหลายกระดาษในภาพ

ถ้าพบหลาย document candidate:
ให้ highlight ทุก candidate
และให้ผู้ใช้แตะเลือก

ห้ามเลือกอัตโนมัติอย่างเดียวโดยไม่มี manual fallback

========================
Manual Crop
========================

หลัง detect ให้เปิด Crop Screen

แสดง:
- ภาพต้นฉบับ
- polygon 4 จุด
- handle ที่ลากได้

ผู้ใช้ต้องลากมุมทั้ง 4 ได้

มีปุ่ม:
- Auto
- Reset
- Rotate
- Confirm

เมื่อ Confirm:
ใช้ cv::getPerspectiveTransform
และ cv::warpPerspective

========================
Editor
========================

หน้าจอ Editor ต้องมี:

Background Layer
Document Layer

Document Layer ต้องรองรับ:

- Drag
- Pinch zoom
- Rotation
- Reset
- Fit
- Bring to center

เพิ่มโหมด Advanced Perspective

เมื่อเปิด:
ให้แสดง handle 4 มุมของ Document
ผู้ใช้สามารถลากแต่ละมุมได้

ใช้ projective transform/homography
ไม่ใช่แค่ rotation + scale

========================
Scene Randomizer
========================

มีปุ่ม:

"สุ่มใหม่"

เมื่อกด:
1. เลือก category ตาม filter
2. เลือก Scene Template
3. อ่าน metadata
4. วางเอกสารใน anchor area
5. สุ่ม offset
6. สุ่ม scale
7. สุ่ม rotation
8. render preview

ผู้ใช้เลือก category ได้:

- ทั้งหมด
- ตู้แช่
- พื้นเรียบ
- พลาสติก
- โปรโมชั่น/ป้าย

และต้องมี:
"ใช้ฉากนี้ แต่สุ่มตำแหน่งใหม่"

เพื่อไม่ต้องเปลี่ยน background ทุกครั้ง

========================
ความสมจริง
========================

หลีกเลี่ยง effect ที่แรงเกินจริง

Optional effect:

1. Brightness matching
ปรับความสว่างของ document ให้ใกล้กับบริเวณ background

2. Slight blur
ให้ document มีความคมใกล้กับภาพต้นฉบับ

3. Noise/grain matching

4. Very soft contact shadow
ใช้เฉพาะ Scene ที่เหมาะสม

5. Color temperature adjustment เล็กน้อย

ทุก effect ต้องเปิด/ปิดได้

ห้ามทำให้ข้อความในใบฝากสั่งอ่านยาก

========================
Image Quality
========================

Detection สามารถใช้ภาพ downscale ได้
แต่ final export ต้องใช้ภาพ resolution เต็ม

ห้ามนำ preview resolution ไป export

เช่น:

Original 4000x3000
↓
Detection copy ~1000-1600 px
↓
หา coordinate
↓
map coordinate กลับ original
↓
Perspective correction จาก original
↓
Composite ที่ final resolution

เพื่อรักษาคุณภาพตัวหนังสือ

========================
Screens
========================

1. HomeScreen

ปุ่ม:
- ถ่ายใบฝากสั่ง
- เลือกจาก Gallery
- ฉากหลัง
- การตั้งค่า

2. CameraScreen

CameraX preview

รองรับ:
- flash
- autofocus
- grid
- capture

3. DocumentDetectionScreen

แสดง document candidates

4. CropScreen

ปรับ 4 มุม

5. EditorScreen

background + document
drag / zoom / rotate
random scene
change scene
perspective
effects

6. ExportScreen

Preview

ตัวเลือก:
- JPEG
- PNG
- Quality
- Save to Gallery

========================
Project Architecture
========================

ใช้โครงสร้างประมาณนี้:

app/
  data/
    model/
    repository/

  domain/
    document/
    template/
    compositor/

  cv/
    DocumentDetector.kt
    PerspectiveCorrector.kt
    ImageMatcher.kt

  editor/
    EditorState.kt
    TransformState.kt
    GestureController.kt
    PerspectiveController.kt

  renderer/
    SceneRenderer.kt
    BitmapCompositor.kt
    EffectsRenderer.kt

  template/
    TemplateLoader.kt
    TemplateRepository.kt

  camera/
    CameraController.kt

  ui/
    home/
    camera/
    detect/
    crop/
    editor/
    export/

========================
สำคัญ
========================

อย่าสร้างทุกอย่างใน MainActivity

แยก:
UI
state
image processing
rendering
storage
template logic

ออกจากกันอย่างชัดเจน

งาน OpenCV และ image processing
ต้องรันใน Dispatchers.Default หรือ background thread

ห้าม block Main Thread

Bitmap ขนาดใหญ่ต้องจัดการ memory อย่างระมัดระวัง

ให้ implement MVP ก่อน

MVP Phase 1:

Camera/Gallery
→ detect document
→ manual crop
→ perspective correction
→ template background
→ drag/scale/rotate
→ random scene
→ export

เมื่อ Phase 1 ทำงานเสถียร
ค่อยเพิ่ม:

- 4-point perspective editor
- brightness matching
- blur matching
- grain
- secondary paper props
- automatic scene analysis

ก่อนเขียน code จำนวนมาก:
ให้สร้าง project architecture + data models + navigation + TODO plan ก่อน

จากนั้น implement ทีละ module
และ build/test หลังจบแต่ละ module

เมื่อมี error:
แก้ error ก่อนเดินหน้าฟีเจอร์ใหม่

อย่า mock ฟังก์ชันหลัก
Document detection / crop / rendering / export ต้องใช้งานได้จริง
```

---

# C — โครงสร้างโปรเจกต์ที่ผมแนะนำ

ถ้าผมเป็นคนวาง architecture ให้โปรเจกต์นี้ ผมจะให้หน้าตาประมาณนี้:

```text
com.phantom.ordercapture

├── MainActivity.kt
│
├── camera/
│   ├── CameraController.kt
│   ├── CameraState.kt
│   └── CameraCaptureManager.kt
│
├── cv/
│   ├── DocumentDetector.kt
│   ├── DocumentCandidate.kt
│   ├── CornerSorter.kt
│   ├── PerspectiveCorrector.kt
│   └── CvBitmapUtils.kt
│
├── template/
│   ├── SceneTemplate.kt
│   ├── SceneCategory.kt
│   ├── ScenePlacement.kt
│   ├── TemplateRepository.kt
│   └── TemplateLoader.kt
│
├── editor/
│   ├── EditorViewModel.kt
│   ├── EditorState.kt
│   ├── DocumentTransform.kt
│   ├── GestureController.kt
│   └── PerspectiveState.kt
│
├── renderer/
│   ├── SceneRenderer.kt
│   ├── BitmapCompositor.kt
│   ├── HomographyRenderer.kt
│   └── ImageEffects.kt
│
├── export/
│   ├── ExportManager.kt
│   └── MediaStoreSaver.kt
│
├── ui/
│   ├── home/
│   ├── camera/
│   ├── detection/
│   ├── crop/
│   ├── editor/
│   └── export/
│
└── util/
    ├── BitmapUtils.kt
    ├── CoordinateUtils.kt
    └── ImageSampling.kt
```

---

# Data Model สำคัญ

```kotlin
data class PointFNormalized(
    val x: Float,
    val y: Float
)
```

```kotlin
data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
)
```

Scene:

```kotlin
data class SceneTemplate(
    val id: String,
    val category: SceneCategory,
    val imagePath: String,
    val placement: ScenePlacement,
    val effects: SceneEffects
)
```

```kotlin
data class ScenePlacement(
    val centerX: Float,
    val centerY: Float,

    val minWidth: Float,
    val maxWidth: Float,

    val minRotation: Float,
    val maxRotation: Float,

    val maxOffsetX: Float,
    val maxOffsetY: Float
)
```

Transform ของใบฝากสั่ง:

```kotlin
data class DocumentTransform(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,

    val scale: Float = 1f,
    val rotation: Float = 0f,

    val perspectiveCorners: List<PointF>? = null
)
```

---

# Algorithm ตรวจจับใบฝากสั่ง

แนวคิดสำคัญคือ **อย่าพยายามอ่านคำว่า “ใบฝากสั่ง” ก่อน**

ตรวจ shape ก่อน

Pseudo-code:

```text
detectDocuments(image):

    detectionImage = resize(image, maxSide = 1400)

    gray = grayscale(detectionImage)

    blur = gaussianBlur(gray)

    edges = canny(blur)

    edges = morphologicalClose(edges)

    contours = findContours(edges)

    candidates = []

    FOR contour in contours:

        area = contourArea(contour)

        IF area < minimumArea:
            continue

        perimeter = arcLength(contour)

        polygon = approxPolyDP(
            contour,
            epsilon = perimeter * 0.02
        )

        IF polygon has 4 points:

            IF polygon is convex:

                boundingRect = getBoundingRect(polygon)

                ratio =
                    max(width,height)
                    /
                    min(width,height)

                rectangularity =
                    contourArea
                    /
                    boundingRectArea

                score =
                    areaScore
                    + rectangularityScore
                    + longPaperScore

                candidates.add(
                    polygon,
                    score
                )

    sort candidates by score descending

    return topCandidates
```

แต่ผมจะไม่ล็อก `aspectRatio = ค่าเดียว`

เพราะจากรูปที่คุณส่งมา ใบฝากสั่งสามารถ:
- ยาวไม่เท่ากัน
- มีส่วนต่อด้านล่าง
- ถูก Perspective ทำให้สัดส่วนเปลี่ยน

จึงควรให้เป็น range ค่อนข้างกว้าง

---

# จุดที่สำคัญมาก: Coordinate Mapping

สมมติภาพต้นฉบับ:

```text
4000 × 3000
```

แต่ detect ด้วย:

```text
1333 × 1000
```

ห้าม crop จากภาพ 1333×1000 แล้วเอาไปใช้ต่อ

ให้เก็บ:

```text
scaleX = originalWidth / detectionWidth
scaleY = originalHeight / detectionHeight
```

แล้ว map:

```text
originalX = detectedX * scaleX
originalY = detectedY * scaleY
```

จากนั้นค่อย `warpPerspective()` จากภาพต้นฉบับ

นี่จะช่วยให้ตัวหนังสือในใบฝากสั่งยังคมอยู่

---

# Perspective correction

```text
input:

TL -------- TR
 |            |
 | document   |
 |            |
BL -------- BR
```

หาความกว้าง:

```text
widthTop =
distance(TL, TR)

widthBottom =
distance(BL, BR)

targetWidth =
max(widthTop, widthBottom)
```

ความสูง:

```text
heightLeft =
distance(TL, BL)

heightRight =
distance(TR, BR)

targetHeight =
max(heightLeft, heightRight)
```

แล้วสร้าง destination:

```text
0,0

W,0

W,H

0,H
```

จากนั้น:

```kotlin
val matrix =
    Imgproc.getPerspectiveTransform(
        sourcePoints,
        destinationPoints
    )

Imgproc.warpPerspective(
    source,
    output,
    matrix,
    Size(width, height)
)
```

---

# Scene Randomizer

ผมแนะนำไม่ใช้ `Random()` แบบสุ่มทุกอย่างอย่างอิสระ

แต่ให้ Scene เป็นคนกำหนด “ขอบเขตที่ปลอดภัย”

Pseudo-code:

```text
randomizeScene():

    scene =
        random template
        from selected category

    placement =
        scene.placement

    x =
        placement.centerX
        + random(
            -placement.maxOffsetX,
            +placement.maxOffsetX
        )

    y =
        placement.centerY
        + random(
            -placement.maxOffsetY,
            +placement.maxOffsetY
        )

    width =
        random(
            placement.minWidth,
            placement.maxWidth
        )

    rotation =
        random(
            placement.minRotation,
            placement.maxRotation
        )

    document.transform =
        Transform(
            x,
            y,
            width,
            rotation
        )
```

ดังนั้นบาง Scene อาจกำหนด:

```text
rotation = -1 ถึง +1
```

ในขณะที่อีก Scene:

```text
rotation = -4 ถึง +4
```

ได้โดยไม่ต้องแก้ source code

---

# Editor Gesture

Gesture หลัก:

```text
1 นิ้ว
→ drag

2 นิ้ว
→ scale

2 นิ้วหมุน
→ rotation
```

Compose สามารถใช้:

```kotlin
detectTransformGestures { _, pan, zoom, rotation ->
    state.position += pan
    state.scale *= zoom
    state.rotation += rotation
}
```

แต่ต้อง clamp เช่น:

```text
scale:

0.5x - 2.0x
```

เพื่อไม่ให้หายออกจากจอโดยไม่ตั้งใจ

---

# Advanced Perspective Editor

อันนี้ผมอยากให้ทำหลัง MVP

แทนที่จะเก็บแค่:

```text
position
scale
rotation
```

จะมี:

```text
TL
TR
BR
BL
```

ผู้ใช้ลากมุมใดมุมหนึ่งได้

เช่น:

```text
●────────────●
│            \
│             ●
●────────────/
```

แล้ว renderer คำนวณ Homography ใหม่ทุกครั้ง

เหมาะกับกรณีที่ต้องการทำให้ใบฝากสั่ง “แนบไปกับพื้นผิว” มากขึ้น

---

# การ Render

ผมไม่แนะนำให้ screenshot ตัว Compose Editor แล้วบันทึก

เพราะ resolution จะผูกกับหน้าจอมือถือ

ควรแยก:

```text
Preview Renderer

กับ

Final Renderer
```

ตัว Preview อาจ render:

```text
1080 × 1440
```

แต่ final อาจเป็น:

```text
3024 × 4032
```

หรือใช้ขนาดของ Scene Template จริง

Final pipeline:

```text
Background original resolution

↓

Document corrected original resolution

↓

apply document transform

↓

optional effects

↓

composite

↓

JPEG/PNG
```

---

# วิธีจัด Template ที่ผมว่าเหมาะกับคุณมาก

Assets:

```text
assets/

scenes/

    freezer/
        freezer_001.jpg
        freezer_001.json

        freezer_002.jpg
        freezer_002.json

    plain/
        plain_001.jpg
        plain_001.json

    promotion/
        promotion_001.jpg
        promotion_001.json

    plastic/
        plastic_001.jpg
        plastic_001.json
```

ข้อดีคือภายหลังคุณเอาภาพหน้างานใหม่มาเพิ่มได้ง่ายมาก

ไม่ต้อง:

```text
แก้ Kotlin
→ compile
→ แก้ algorithm
```

แค่เพิ่ม:

```text
.jpg
.json
```

---

# และผมจะเพิ่มเครื่องมือ “Template Setup” ให้ด้วย

อันนี้ผมคิดว่าจะช่วยคุณมากในระยะยาว

แทนที่จะนั่งเขียนพิกัด JSON เอง เช่น:

```json
"centerX": 0.524,
"centerY": 0.493
```

ให้มี **Template Editor ภายในแอปสำหรับ Admin**

คุณเปิดภาพฉากขึ้นมา แล้ว:

```text
┌──────────────────────┐
│                      │
│        ┌────┐        │
│        │    │        │
│        │ใบ  │        │
│        │    │        │
│        └────┘        │
│                      │
└──────────────────────┘
```

ลากกรอบตัวอย่างไปยังตำแหน่งที่ต้องการ

แล้วกด:

**บันทึก Template**

แอปสร้าง JSON ให้อัตโนมัติ

ตรงนี้จะช่วยมาก เพราะคุณบอกว่าจะมี **ฉากหลังหลายแบบ** และต่อไปมีโอกาสเพิ่มอีกเรื่อย ๆ

---

## Roadmap ที่ผมแนะนำ

| Phase | ทำอะไร |
|---|---|
| **1** | Camera/Gallery + crop ด้วยมือ |
| **2** | OpenCV auto document detection |
| **3** | Perspective correction |
| **4** | Scene Template system |
| **5** | Drag / Zoom / Rotate |
| **6** | Scene randomizer |
| **7** | Export full resolution |
| **8** | Manual 4-corner perspective |
| **9** | Brightness / blur / grain matching |
| **10** | Template Admin Editor |

และผมแนะนำอย่างยิ่งว่า **อย่าให้ Codex ทำ Phase 1–10 พร้อมกันในคำสั่งเดียว** แม้ Prompt ด้านบนจะเป็น Master Spec ก็ตาม

ให้ใช้ Master Spec เป็น context แล้วสั่งมันทีละ Phase เช่น

> อ่าน Master Spec ทั้งหมด แต่ตอนนี้ให้ implement เฉพาะ Phase 1 เท่านั้น เมื่อเสร็จให้ build project และแก้ compile error ทั้งหมด ห้ามเริ่ม Phase 2 จนกว่าฉันจะสั่ง

แบบนี้โอกาสได้โปรเจกต์ที่ใช้งานจริงจะสูงกว่าการสั่ง “สร้างแอปทั้งหมด” ในครั้งเดียวมาก

สำหรับแอปนี้ **จุดที่ยากที่สุดจริง ๆ ไม่ใช่ UI แต่คือ Document Detection + Coordinate Mapping + Rendering แบบ full-resolution** สามส่วนนี้ควรให้ Codex ทำและทดสอบทีละส่วนก่อนเพิ่มเอฟเฟกต์หรือระบบสุ่มครับ

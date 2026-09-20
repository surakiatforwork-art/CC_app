# ใบฝากสั่ง • Scene

แอป Native Android ตามบทสนทนา “ออกแบบแอปสแกนภาพ” ใช้ Kotlin, Jetpack Compose, CameraX, OpenCV และ Android Canvas ทำงานออฟไลน์

## ใช้งานรุ่นแรก

1. ถ่ายภาพหรือเลือกภาพใบฝากสั่งจากแกลเลอรี
2. เลือกกรอบที่ระบบตรวจพบ หรือเลื่อนจุดทั้ง 4 เองให้ตรงขอบกระดาษ แล้วกดยืนยัน
3. เลือกฉาก / สุ่มฉาก ลากด้วยนิ้วเดียว ใช้สองนิ้วย่อขยายและหมุน หรือเปิดปรับ perspective 4 มุม
4. ดูตัวอย่าง เลือก JPG/PNG และบันทึกลง `Pictures/OrderSlipScene`

แอปไม่ได้อ่านข้อความหรือเติมข้อมูลในใบ จึงรองรับรูปแบบใบฝากสั่งที่เปลี่ยนรายเดือน ไม่มี timestamp overlay

## ฉากหลัง

มีฉากเริ่มต้น 6 ฉาก ได้แก่ พื้นเรียบ 2 แบบ, เคาน์เตอร์หน้าร้าน, ตู้แช่เครื่องดื่ม, พลาสติกขุ่น และป้ายโปรโมชั่น เลือกหมวดแล้วกดเพิ่มภาพฉากหลังเพื่อนำเข้าภาพจริง ภาพและ metadata เก็บในพื้นที่ภายในแอปและยังอยู่หลังปิดเปิดแอป

ภาพอ้างอิงในแชทมีใบฝากสั่งและ timestamp อยู่แล้ว จึงไม่ใช้เป็นฉากสำเร็จรูปโดยตรง ควรนำเข้าภาพพื้นหลังที่สะอาด หมวดตู้แช่ พลาสติก และโปรโมชั่นมีตัวกรองพร้อม แต่ยังไม่มีภาพแถมให้

นักพัฒนาเพิ่มไฟล์ภาพพร้อม JSON ใน `app/src/main/assets/scenes` ได้ โดยไม่แก้ Kotlin:

```json
{
  "id": "counter_01",
  "name": "เคาน์เตอร์ 01",
  "category": "plain",
  "image": "counter_01.jpg",
  "paperPlacement": {
    "centerX": 0.5, "centerY": 0.5,
    "widthMin": 0.28, "widthMax": 0.36,
    "rotationMin": -3, "rotationMax": 3,
    "offsetX": 0.03, "offsetY": 0.03
  }
}
```

หมวด: `plain`, `freezer`, `plastic`, `promotion` พิกัดและความกว้างเป็นสัดส่วน 0–1 เทียบกับฉาก การเพิ่มใน assets ต้อง build APK ใหม่ ส่วนการนำเข้าจากแอปไม่ต้อง build ใหม่

## Build

ต้องมี JDK 17 และ Android SDK 35 แก้ `local.properties` ให้ตรงกับเครื่อง แล้วรัน:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk` รองรับ Android 10 ขึ้นไป

## คุณภาพและขอบเขตรุ่นแรก

- Detection ใช้ภาพย่อ แต่ครอปจากต้นฉบับที่แก้ EXIF orientation แล้ว
- Export เรนเดอร์ใหม่ที่ขนาดฉากต้นฉบับ ไม่บันทึก screenshot ของ preview
- รองรับภาพไม่เกิน 32 ล้านพิกเซล และแจ้งข้อผิดพลาดเมื่อหน่วยความจำไม่พอ
- ตรวจอัตโนมัติอาจพลาดในภาพพื้นหลังซับซ้อน มี manual fallback เสมอ
- สถานะงานคงอยู่เมื่อหมุนจอ แต่ยังไม่กู้คืนงานเมื่อระบบฆ่า process
- มีการจับคู่ความสว่างและ white balance ของใบกับบริเวณฉากที่วาง พร้อมเงาสัมผัสที่ปรับระดับหรือปิดได้ หน้าแก้ anchor metadata แบบ visual และ grain/blur matching ยังเป็นงานระยะถัดไป
- ไม่มี backend, analytics หรือการส่งภาพออกอินเทอร์เน็ต

ดู `docs/MASTER_SPEC.md` และ `docs/PLAN.md` สำหรับสเป็คและงานระยะถัดไป

อ้างอิงการติดตั้ง OpenCV: https://docs.opencv.org/4.11.0/d5/df8/tutorial_dev_with_OCV_on_Android.html

# 📱 TrueMoney Listener (Android Background Service)

แอปพลิเคชัน Android ที่พัฒนาด้วย Kotlin ทำหน้าที่เป็น **Background Service** เพื่อดักจับการแจ้งเตือน (Notification) จากแอปพลิเคชัน **TrueMoney Wallet** แบบเรียลไทม์ เมื่อตรวจพบการโอนเงินเข้า แอปจะทำการสกัดยอดเงินและส่งข้อมูลผ่าน API (Webhook) ไปยังเซิร์ฟเวอร์ของคุณโดยอัตโนมัติ

แอปพลิเคชันนี้ถูกออกแบบมาเพื่อใช้งานร่วมกับ [**DonateWeb**](https://donate.nattapat2871.me) (ระบบโดเนทสตรีมเมอร์ส่วนตัว) ทำให้คุณมีระบบ Payment Gateway สำหรับสแกน QR Code แบบอัตโนมัติ 100% โดยไม่ต้องพึ่งพาคนกลาง

---

## 🌟 ฟีเจอร์หลัก (Key Features)

- **Event-Driven Notification Listener:** ทำงานเบื้องหลังแบบไม่กินแบตเตอรี่ (No Polling) โดยแอปจะตื่นขึ้นมาทำงานเฉพาะตอนที่มีการแจ้งเตือนเด้งเข้ามาเท่านั้น
- **Smart Regex Extraction:** กรองและดึงเฉพาะ "ตัวเลขยอดเงิน" ออกจากข้อความแจ้งเตือนของ TrueMoney ได้อย่างแม่นยำ
- **Secure API Transmission:** ส่งข้อมูลไปยังเซิร์ฟเวอร์ด้วย HTTP POST พร้อมแนบ `X-API-Key` ใน Header เพื่อป้องกันการถูกยิง API ปลอม (Spam)
- **Local Notifications:** แจ้งเตือนสถานะการส่งข้อมูล (สำเร็จ / ล้มเหลว) ให้ผู้ใช้ทราบผ่านแถบการแจ้งเตือนของมือถือ
- **Built-in Test Tool:** มีหน้าจอ UI สำหรับกรอกตัวเลขและกดปุ่มทดสอบยิง API ไปยังเซิร์ฟเวอร์ได้โดยไม่ต้องรอให้มีการโอนเงินจริง

---

## ⚙️ หลักการทำงาน (Workflow)

1. ผู้ใช้สแกน QR Code โดเนทจากหน้าเว็บและทำการโอนเงิน
2. แอป **TrueMoney Wallet** ในมือถือ (เครื่องที่ติดตั้งแอปนี้) เด้งการแจ้งเตือนว่า *"คุณได้รับเงิน..."*
3. Service `TrueMoneyNotificationListener` ตรวจจับพบ Package Name `th.co.truemoney.wallet`
4. แอปทำการดึงข้อความ (Text) และใช้ Regex ตัดเอาเฉพาะตัวเลขยอดเงิน (เช่น `50.14`)
5. แอปส่งข้อมูลรูปแบบ JSON ไปยัง Endpoint ของเซิร์ฟเวอร์ (เช่น `https://donate.nattapat2871.me/api/payment/notify`) พร้อมแนบ `X-API-Key`
6. เซิร์ฟเวอร์รับข้อมูล ตรวจสอบยอดเงิน และสั่งแจ้งเตือนขึ้นหน้าจอสตรีมมิ่ง (OBS)

---

## 📂 โครงสร้างไฟล์ที่สำคัญ (Project Structure)

- `MainActivity.kt`: หน้าจอหลักของแอป ทำหน้าที่ขอสิทธิ์เข้าถึงการแจ้งเตือน (Notification Access) และมีหน้าต่างจำลองการยิง API สำหรับทดสอบ
- `TrueMoneyNotificationListener.kt`: หัวใจหลักของแอป เป็น Background Service ที่คอยฟังและอ่านข้อความจากการแจ้งเตือน พร้อมโค้ดยิง HTTP POST ไปยังเซิร์ฟเวอร์
- `AndroidManifest.xml`: ไฟล์ตั้งค่าสิทธิ์ต่างๆ ของแอป เช่น การขอสิทธิ์อินเทอร์เน็ต (`INTERNET`) และการประกาศเปิดใช้งาน Notification Service

---

## 🚀 การติดตั้งและตั้งค่า (Installation & Setup)

### 1. การตั้งค่าในโค้ด (Configuration)
ก่อนทำการ Build ลงมือถือ ให้เข้าไปตรวจสอบและแก้ไขข้อมูลในไฟล์ต่อไปนี้ให้ตรงกับเซิร์ฟเวอร์ของคุณ:

**ในไฟล์ `TrueMoneyNotificationListener.kt` และ `MainActivity.kt`**
ค้นหาฟังก์ชันที่ใช้ส่งข้อมูล และอัปเดตตัวแปร 2 ตัวนี้:
```kotlin
// 1. เปลี่ยน URL เป็นโดเมนเว็บ Backend ของคุณ
val url = URL("[https://donate.nattapat2871.me/api/payment/notify](https://donate.nattapat2871.me/api/payment/notify)")

// 2. เปลี่ยนรหัส API Key ให้ตรงกับฝั่ง FastAPI ของคุณ
connection.setRequestProperty("X-API-Key", "RIMURUSAMAISTHEBEST")
```

### 2. การ Build ลงมือถือ (Deployment)
1. นำมือถือ Android ของคุณเสียบสาย USB เข้ากับคอมพิวเตอร์
2. เปิด **โหมดนักพัฒนาซอฟต์แวร์ (Developer Options)** และเปิด **การแก้ไขจุดบกพร่องผ่าน USB (USB Debugging)** ในมือถือ
3. กดปุ่ม **Run** ใน IntelliJ IDEA หรือ Android Studio เพื่อติดตั้งแอปพลิเคชันลงในมือถือ
4. **สำคัญมาก:** หลังจากแอปติดตั้งเสร็จแล้ว ให้เข้าไป **"ปิดโหมดนักพัฒนาซอฟต์แวร์"** ในตั้งค่ามือถือ (เนื่องจากแอป TrueMoney จะไม่ยอมเปิดทำงานหากตรวจพบโหมดนี้)

---

## ⚠️ การตั้งค่าสิทธิ์ในมือถือ (Required Permissions)

เพื่อให้แอปทำงานได้อย่างสมบูรณ์และไม่ถูกระบบ Android ฆ่าทิ้ง (Kill Process) คุณต้องตั้งค่า 3 ส่วนนี้ในมือถือที่ใช้รัน:

1. **Notification Access (การเข้าถึงการแจ้งเตือน):**
    - เมื่อเปิดแอปครั้งแรก แอปจะพาคุณไปยังหน้าต่างตั้งค่า ให้หาชื่อแอป `TrueMoney Listener` และกด "อนุญาต"
2. **Battery Optimization (การปรับแต่งแบตเตอรี่):**
    - มือถือรุ่นใหม่มักจะปิดแอปที่ทำงานเบื้องหลัง ให้ไปที่ `การตั้งค่า > แอป > TrueMoney Listener > แบตเตอรี่`
    - เลือกเป็น **ไม่มีข้อจำกัด (Unrestricted)**
3. **Lock App (ล็อคแอปใน Recent Apps):**
    - เปิดแอปนี้ขึ้นมา จากนั้นปัดหน้าจอเพื่อดูแอปที่เพิ่งเปิด (Recent Apps)
    - กดค้างที่แอป `TrueMoney Listener` แล้วเลือกปุ่ม **รูปแม่กุญแจ (Lock)** เพื่อบอกระบบว่าห้ามปัดแอปนี้ทิ้ง

---

## 👨‍💻 ผู้พัฒนา
Developed by [nattapat2871](https://github.com/nattapat2871)
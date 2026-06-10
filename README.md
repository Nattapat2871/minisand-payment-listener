# 📱 Minisand Payment Listener (Android Application)

แอปพลิเคชัน Android ระดับระบบ (System-level Listener) ที่เปลี่ยนสมาร์ทโฟนให้เป็น Gateway รับชำระเงินอัตโนมัติ โดยการดักจับการแจ้งเตือนจากแอปธนาคารชั้นนำในไทย

---

## 🔍 กลไกการทำงาน (How it Works)

แอปพลิเคชันนี้ใช้ `NotificationListenerService` ของ Android ซึ่งเป็นความสามารถพิเศษในการ "อ่าน" การแจ้งเตือนที่ปรากฏบนแถบสถานะ (Status Bar) 

### 1. การดักจับ (Filtering)
แอปจะเฝ้าดู Notification จาก Package ที่กำหนดเท่านั้น:
- **TrueMoney Wallet:** `th.co.truemoney.wallet`
- **K PLUS (กสิกรไทย):** `com.kasikorn.retail.mbanking.mobile` และ `.wap`

### 2. การวิเคราะห์ข้อมูล (Parsing Logic)
แอปใช้ **Regular Expression (Regex)** ในการสกัดยอดเงิน (Amount) ออกจากข้อความแจ้งเตือน โดยรองรับทั้งรูปแบบภาษาไทยและภาษาอังกฤษ:
- ตัวอย่างรูปแบบ: `จำนวนเงิน 100.00`, `Amount 100.00`
- มีระบบ Fallback หากรูปแบบข้อความเปลี่ยนไป เพื่อพยายามหายอดเงินให้ได้แม่นยำที่สุด

### 3. การส่งข้อมูล (Transmission)
เมื่อดักจับข้อมูลได้ แอปจะส่ง HTTP POST ไปยัง Server (FastAPI) ทันที:
- ส่งข้อมูลประกอบด้วย: `title`, `text`, `type` (KPLUS/TRUEMONEY), และ `amount`
- มีระบบ **Local Notification** แจ้งเตือนบนเครื่องว่า "ส่งข้อมูลสำเร็จ" เพื่อให้ผู้ใช้ทราบสถานะ

---

## ⚡ คุณสมบัติทางเทคนิค (Technical Features)

- **Heartbeat System:** มีการส่งสัญญาณชีพ (Heartbeat) ไปยังเซิร์ฟเวอร์ทุกๆ 1 นาที เพื่อให้ระบบหลังบ้านทราบว่ามือถือยังออนไลน์อยู่
- **Background Persistence:** รันเป็น Service ตลอดเวลา แม้จะปิดหน้าแอปไปแล้ว
- **Multi-threaded:** แยกการทำงานของการส่งข้อมูลและ Heartbeat ออกจาก UI Thread เพื่อป้องกันแอปค้าง

---

## ⚙️ ขั้นตอนการติดตั้งและตั้งค่า (Setup Guide)

เพื่อให้แอปทำงานได้อย่างสมบูรณ์ ต้องมีการตั้งค่าพิเศษบน Android ดังนี้:

### 1. การอนุญาตสิทธิ์ (Critical Permissions)
- **Notification Access:** ผู้ใช้ต้องเข้าไปที่ *Settings > Apps > Special app access > Notification access* และกดเปิดสิทธิ์ให้แอป `minisand-payment-listener`
- **Internet:** แอปต้องการสิทธิ์เข้าถึงอินเทอร์เน็ตเพื่อคุยกับ Server

### 2. การจัดการพลังงาน (Battery Optimization)
- ต้องตั้งค่าแอปให้เป็น **"Don't Optimize"** หรือ **"Unrestricted"** ในเมนู Battery เพื่อป้องกันระบบ Android ปิดแอปทิ้งเมื่อจอดับนานๆ

### 3. การแสดงผลแจ้งเตือนของธนาคาร
- แอปธนาคาร (K PLUS / TrueMoney) **ต้องเปิดแจ้งเตือนแบบ "แสดงเนื้อหา"** บนหน้าจอ Lock Screen หรือ Status Bar เพื่อให้ Listener สามารถอ่านยอดเงินได้

---

## 🛠️ การพัฒนาและแก้ไข (Development)

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Min SDK:** 24 (Android 7.0+)
- **Target SDK:** 34 (Android 14)

### การตั้งค่า URL เซิร์ฟเวอร์
สามารถแก้ไข URL ปลายทางได้ในไฟล์: `PaymentNotificationListener.kt` ที่ฟังก์ชัน `startHeartbeat` และ `sendDataToServer`

---

## ⚠️ ข้อควรระวัง
- **Security:** API Key ถูกระบุไว้ในโค้ด หากมีการเปลี่ยนที่เซิร์ฟเวอร์ ต้องอัปเดตโค้ดในแอปและคอมไพล์ใหม่
- **Connectivity:** หากอินเทอร์เน็ตบนมือถือหลุด ระบบจะหยุดทำงานทันที (ตรวจสอบได้จากหน้า Status ของ Server)

---
*ดำเนินการโดย Wisdom King Raphael เพื่อความรุ่งโรจน์ของ Minisand*

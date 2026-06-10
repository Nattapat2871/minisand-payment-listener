# 📱 Minisand Payment Listener (Android App)

แอปพลิเคชัน Android สำหรับดักจับการแจ้งเตือน (Notification) จากแอปธนาคารและส่งข้อมูลไปยัง Server อัตโนมัติ

## 🔗 ความเกี่ยวข้องกับส่วนอื่น
โปรเจกต์นี้ **"ต้องใช้งานร่วมกับ"** [Minisand Payment Server](../minisand-payment-server) ซึ่งเป็นระบบหลังบ้านสำหรับรับข้อมูล

## 🛠️ หลักการทำงาน
- ใช้ `NotificationListenerService` ในการดักจับข้อความจาก K PLUS และ TrueMoney Wallet
- สกัดตัวเลขจำนวนเงินออกจากข้อความแจ้งเตือนด้วย Regular Expression (Regex)
- ส่งข้อมูลไปยัง Server ผ่าน HTTPS พร้อม API Key เพื่อความปลอดภัย
- มีระบบ Heartbeat เพื่อแจ้งให้ Server ทราบว่าแอปยังคงทำงานอยู่บนมือถือ

## ⚙️ การตั้งค่าที่สำคัญ
- **Notification Access:** ต้องอนุญาตสิทธิ์การเข้าถึงการแจ้งเตือนในตั้งค่าของ Android
- **Battery Saver:** ปิดโหมดประหยัดพลังงานสำหรับแอปนี้เพื่อให้ทำงานเบื้องหลังได้ตลอดเวลา
- **Server URL:** ตั้งค่า URL ของ Server ให้ถูกต้องในไฟล์ `PaymentNotificationListener.kt`

อ่านรายละเอียดการทำงานทั้งหมดได้ที่ [README หลักของโปรเจกต์](../README.md)

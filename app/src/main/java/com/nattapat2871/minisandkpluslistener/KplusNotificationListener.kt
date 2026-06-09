package com.nattapat2871.minisandkpluslistener

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class KplusNotificationListener : NotificationListenerService() {

    private val CHANNEL_ID = "payment_channel"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        Log.d("KplusListener", "Notification received from: $packageName")

        // รายชื่อ Package Name ของ K PLUS ที่เป็นไปได้
        val kplusPackages = listOf(
            "com.kasikorn.retail.mbanking.mobile",
            "com.kasikorn.retail.mbanking.wap"
        )

        if (kplusPackages.contains(packageName)) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            
            Log.d("KplusListener", "K PLUS Notification - Title: $title, Text: $text")

            // ปรับปรุงการตรวจสอบ: ให้ครอบคลุมมากขึ้น (บางทีหัวข้ออาจไม่ใช่ภาษาไทยที่คิดไว้)
            val isTransaction = title.contains("เงินเข้า") || text.contains("เงินเข้า") ||
                    title.contains("ได้รับเงิน") || text.contains("ได้รับเงิน") ||
                    title.contains("Deposit", ignoreCase = true) || text.contains("Deposit", ignoreCase = true) ||
                    title.contains("K PLUS") // เพิ่มกรณี Title เป็นชื่อแอปเฉยๆ แต่เนื้อหาข้างในคือยอดเงิน

            if (isTransaction) {
                Log.d("KplusListener", "Transaction detected! Extracting amount...")
                val amount = extractAmount(text)
                Log.d("KplusListener", "Extracted Amount: $amount")
                sendDataToYourServer(title, text, amount)
            } else {
                Log.w("KplusListener", "Notification found but not identified as a transaction.")
            }
        }
    }

    // ฟังก์ชันดึงตัวเลขยอดเงิน รองรับทั้ง "จำนวนเงิน" และ "Amount"
    private fun extractAmount(text: String): String {
        // Regex สำหรับภาษาไทย: จำนวนเงิน 0.67
        val thPattern = Pattern.compile("จำนวนเงิน\\s+([0-9,]+\\.[0-9]{2})")
        val thMatcher = thPattern.matcher(text)
        if (thMatcher.find()) return thMatcher.group(1)?.replace(",", "") ?: "0.00"

        // Regex สำหรับภาษาอังกฤษ: Amount 0.23
        val enPattern = Pattern.compile("Amount\\s+([0-9,]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)
        val enMatcher = enPattern.matcher(text)
        if (enMatcher.find()) return enMatcher.group(1)?.replace(",", "") ?: "0.00"

        // Fallback: หาตัวเลขที่มีทศนิยม 2 ตำแหน่งใดๆ ในข้อความ
        val fallbackPattern = Pattern.compile("([0-9,]+\\.[0-9]{2})")
        val fallbackMatcher = fallbackPattern.matcher(text)
        return if (fallbackMatcher.find()) fallbackMatcher.group(1)?.replace(",", "") ?: "0.00" else "0.00"
    }

    private fun sendDataToYourServer(title: String, text: String, amount: String) {
        thread {
            try {
                // เปลี่ยน URL เป็น API Unified บนโดเมนใหม่
                val url = URL("https://minisand-payment.nattapat2871.me/api/kplus/paymentnotify?key=minisandtw888")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"

                // กำหนดประเภทข้อมูลเป็น JSON
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.doOutput = true

                // แนบข้อมูลผู้เล่น (เบื้องต้นเป็น unknown หรือมาสเตอร์สามารถเพิ่มช่องกรอกชื่อในแอปได้)
                val jsonInputString = "{\"title\": \"$title\", \"text\": \"$text\", \"bank\": \"KPLUS\", \"amount\": \"$amount\", \"player\": \"unknown\"}"

                OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }

                if (connection.responseCode == 200) {
                    // ถ้า Server ตอบกลับสำเร็จ ให้แอปเราเด้งแจ้งเตือนบอกตัวเอง
                    showLocalNotification("K PLUS: ส่งข้อมูลสำเร็จ", "ยอดเงิน: $amount บาท")
                } else {
                    Log.e("KplusListener", "ส่งข้อมูลไม่สำเร็จ: Response Code ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("KplusListener", "Error: ${e.message}")
            }
        }
    }

    private fun showLocalNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // สร้าง Channel สำหรับ Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Payment Status", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
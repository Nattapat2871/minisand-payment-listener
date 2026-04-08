package com.nattapat2871.truemoneylistener

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

class TrueMoneyNotificationListener : NotificationListenerService() {

    private val CHANNEL_ID = "payment_channel"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (packageName == "th.co.truemoney.wallet") {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            val isTransaction = title.contains("ได้รับเงิน") || text.contains("ได้รับเงิน") ||
                    title.contains("โอนเงิน") || text.contains("โอนเงิน") ||
                    title.contains("เงินเข้า") || text.contains("เงินเข้า")

            if (isTransaction) {
                // พยายามดึงตัวเลขยอดเงินจากข้อความด้วย Regex
                val amount = extractAmount(text)
                sendDataToYourServer(title, text, amount)
            }
        }
    }

    // ฟังก์ชันดึงตัวเลขยอดเงิน (เช่น 1.00 หรือ 20) จากข้อความ
    private fun extractAmount(text: String): String {
        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) ?: "0.00" else "0.00"
    }

    private fun sendDataToYourServer(title: String, text: String, amount: String) {
        thread {
            try {
                val url = URL("https://donate.nattapat2871.me/api/payment/notify")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"

                // กำหนดประเภทข้อมูลเป็น JSON
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")

                // แนบรหัส API Key ยืนยันตัวตนไปกับ Header
                connection.setRequestProperty("X-API-Key", "RIMURUSAMAISTHEBEST")

                connection.doOutput = true

                val jsonInputString = "{\"title\": \"$title\", \"text\": \"$text\"}"

                OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }

                if (connection.responseCode == 200) {
                    // ถ้า Server ตอบกลับสำเร็จ ให้แอปเราเด้งแจ้งเตือนบอกตัวเอง
                    showLocalNotification("API ส่งข้อมูลสำเร็จ", "ยอดเงิน: $amount บาท")
                } else if (connection.responseCode == 401) {
                    // แจ้งเตือนกรณีรหัส API Key ไม่ถูกต้อง
                    Log.e("TrueMoneyListener", "ส่งข้อมูลไม่สำเร็จ: รหัส API Key ไม่ถูกต้อง (401 Unauthorized)")
                }
            } catch (e: Exception) {
                Log.e("TrueMoneyListener", "Error: ${e.message}")
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
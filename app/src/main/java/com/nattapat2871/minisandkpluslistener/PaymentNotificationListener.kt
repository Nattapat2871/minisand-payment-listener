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

class PaymentNotificationListener : NotificationListenerService() {

    private val CHANNEL_ID = "payment_channel"
    private var heartbeatThread: Thread? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("PaymentListener", "Notification Listener Connected!")
        startHeartbeat()
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatThread?.interrupt()
    }

    private fun startHeartbeat() {
        if (heartbeatThread != null) return
        
        heartbeatThread = thread {
            while (true) {
                try {
                    Log.d("PaymentListener", "Sending heartbeat...")
                    val url = URL("https://minisand-payment.nattapat2871.me/api/heartbeat?key=minisandtw888")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val responseCode = connection.responseCode
                    Log.d("PaymentListener", "Heartbeat response: $responseCode")
                    
                    Thread.sleep(60000) // 1 minute
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("PaymentListener", "Heartbeat error: ${e.message}")
                    try { Thread.sleep(30000) } catch (inner: Exception) {}
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        Log.d("PaymentListener", "Notification received from: $packageName")

        val kplusPackages = listOf(
            "com.kasikorn.retail.mbanking.mobile",
            "com.kasikorn.retail.mbanking.wap"
        )
        
        val truemoneyPackages = listOf(
            "th.co.truemoney.wallet"
        )

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (kplusPackages.contains(packageName)) {
            handleKplusNotification(title, text)
        } else if (truemoneyPackages.contains(packageName)) {
            handleTrueMoneyNotification(title, text)
        }
    }

    private fun handleKplusNotification(title: String, text: String) {
        Log.d("PaymentListener", "K PLUS Notification - Title: $title, Text: $text")
        val isTransaction = title.contains("เงินเข้า") || text.contains("เงินเข้า") ||
                title.contains("ได้รับเงิน") || text.contains("ได้รับเงิน") ||
                title.contains("Deposit", ignoreCase = true) || text.contains("Deposit", ignoreCase = true)

        if (isTransaction) {
            val amount = extractAmount(text)
            sendDataToServer("KPLUS", title, text, amount)
        }
    }

    private fun handleTrueMoneyNotification(title: String, text: String) {
        Log.d("PaymentListener", "TrueMoney Notification - Title: $title, Text: $text")
        // ข้อความทรูมันนี่มักจะเป็น "ได้รับเงินโอนจำนวน..." หรือ "ได้รับเงินจาก..."
        val isTransaction = text.contains("ได้รับเงิน") || text.contains("โอนเงินเข้า") ||
                text.contains("Received", ignoreCase = true)

        if (isTransaction) {
            val amount = extractAmount(text)
            sendDataToServer("TRUEMONEY", title, text, amount)
        }
    }

    private fun extractAmount(text: String): String {
        val thPattern = Pattern.compile("จำนวนเงิน\\s+([0-9,]+\\.[0-9]{2})")
        val thMatcher = thPattern.matcher(text)
        if (thMatcher.find()) return thMatcher.group(1)?.replace(",", "") ?: "0.00"

        val enPattern = Pattern.compile("Amount\\s+([0-9,]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)
        val enMatcher = enPattern.matcher(text)
        if (enMatcher.find()) return enMatcher.group(1)?.replace(",", "") ?: "0.00"

        val fallbackPattern = Pattern.compile("([0-9,]+\\.[0-9]{2})")
        val fallbackMatcher = fallbackPattern.matcher(text)
        return if (fallbackMatcher.find()) fallbackMatcher.group(1)?.replace(",", "") ?: "0.00" else "0.00"
    }

    private fun sendDataToServer(type: String, title: String, text: String, amount: String) {
        thread {
            try {
                // เลือก Endpoint ตามประเภทแอป
                val endpoint = if (type == "KPLUS") "/api/kplus/paymentnotify" else "/api/truemoney/listener"
                val url = URL("https://minisand-payment.nattapat2871.me$endpoint?key=minisandtw888")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val jsonInputString = "{\"title\": \"$title\", \"text\": \"$text\", \"bank\": \"$type\", \"amount\": \"$amount\", \"player\": \"unknown\"}"

                OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }

                if (connection.responseCode == 200) {
                    showLocalNotification("$type: ส่งข้อมูลสำเร็จ", "ยอดเงิน: $amount บาท")
                } else {
                    Log.e("PaymentListener", "ส่งข้อมูลไม่สำเร็จ: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("PaymentListener", "Error: ${e.message}")
            }
        }
    }

    private fun showLocalNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
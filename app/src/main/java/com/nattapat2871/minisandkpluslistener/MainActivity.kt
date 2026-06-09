package com.nattapat2871.minisandkpluslistener

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- สร้างหน้าจอ UI ---
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        // ช่องกรอกยอดเงิน
        val amountInput = EditText(this).apply {
            hint = "กรอกยอดเงินที่จะทดสอบ (เช่น 20.01)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 18f
        }

        val testButton = Button(this).apply {
            text = "ทดสอบยิง API (K PLUS)"
        }

        layout.addView(amountInput)
        layout.addView(testButton)
        setContentView(layout)

        testButton.setOnClickListener {
            val amount = amountInput.text.toString()
            if (amount.isNotEmpty()) {
                Toast.makeText(this, "กำลังส่งยอด $amount...", Toast.LENGTH_SHORT).show()
                testSendApi(amount)
            } else {
                Toast.makeText(this, "กรุณากรอกยอดเงินก่อน", Toast.LENGTH_SHORT).show()
            }
        }

        // --- ขอสิทธิ์ต่างๆ ---
        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun testSendApi(amount: String) {
        thread {
            try {
                val url = URL("https://minisand-payment.nattapat2871.me/api/kplus/paymentnotify?key=minisandtw888")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.doOutput = true

                val jsonInputString = "{\"title\": \"K PLUS (Test)\", \"text\": \"เงินเข้า ฿ $amount\", \"bank\": \"KPLUS\", \"amount\": \"$amount\", \"player\": \"tester\"}"

                OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }
                val responseCode = connection.responseCode

                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "✅ ส่งยอด $amount สำเร็จ", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ ล้มเหลว: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "⚠️ Error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null && TextUtils.equals(pkgName, componentName.packageName)) return true
            }
        }
        return false
    }
}
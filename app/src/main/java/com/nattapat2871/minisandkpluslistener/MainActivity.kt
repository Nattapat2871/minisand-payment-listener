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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val amountInput = EditText(this).apply {
            hint = "กรอกยอดเงินที่จะทดสอบ (เช่น 20.01)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 18f
        }

        val kplusButton = Button(this).apply {
            text = "ทดสอบยิง API (K PLUS)"
        }

        val tmButton = Button(this).apply {
            text = "ทดสอบยิง API (TrueMoney)"
        }

        layout.addView(amountInput)
        layout.addView(kplusButton)
        layout.addView(tmButton)
        setContentView(layout)

        kplusButton.setOnClickListener {
            val amount = amountInput.text.toString()
            if (amount.isNotEmpty()) {
                testSendApi("KPLUS", amount)
            } else {
                Toast.makeText(this, "กรุณากรอกยอดเงินก่อน", Toast.LENGTH_SHORT).show()
            }
        }

        tmButton.setOnClickListener {
            val amount = amountInput.text.toString()
            if (amount.isNotEmpty()) {
                testSendApi("TRUEMONEY", amount)
            } else {
                Toast.makeText(this, "กรุณากรอกยอดเงินก่อน", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun testSendApi(type: String, amount: String) {
        thread {
            try {
                val endpoint = if (type == "KPLUS") "/api/kplus/paymentnotify" else "/api/truemoney/listener"
                val url = URL("https://minisand-payment.nattapat2871.me$endpoint?key=minisandtw888")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val jsonInputString = "{\"title\": \"$type (Test)\", \"text\": \"ได้รับเงินโอนจำนวน $amount บาท\", \"bank\": \"$type\", \"amount\": \"$amount\", \"player\": \"tester\"}"

                OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }
                val responseCode = connection.responseCode

                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "✅ ส่งยอด $amount ($type) สำเร็จ", Toast.LENGTH_SHORT).show()
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
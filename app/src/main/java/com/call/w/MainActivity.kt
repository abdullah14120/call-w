package com.call.w

import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_ID = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val btnInjectCall = findViewById<Button>(R.id.btnInjectCall)
        
        btnInjectCall.text = "حقن رسالة واردة"

        // طلب جعل التطبيق هو الافتراضي للرسائل
        requestDefaultSmsRole()

        btnInjectCall.setOnClickListener {
            val number = etPhoneNumber.text.toString().trim()
            
            if (number == "+" || number.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة رقم صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDefaultSmsApp()) {
                injectSms(number)
            } else {
                Toast.makeText(this, "يجب تعيين التطبيق كافتراضي للرسائل أولاً", Toast.LENGTH_SHORT).show()
                requestDefaultSmsRole()
            }
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }

    private fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult(intent, REQUEST_ID)
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent)
            }
        }
    }

    private fun injectSms(number: String) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, number)
            put(Telephony.Sms.BODY, "هذه رسالة نصية محقونة للتجربة.") // محتوى الرسالة
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0) // 0 تعني غير مقروءة، 1 تعني مقروءة
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX) // نوع الرسالة: واردة
        }

        try {
            contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            Toast.makeText(this, "تم حقن الرسالة بنجاح!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل الحقن: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

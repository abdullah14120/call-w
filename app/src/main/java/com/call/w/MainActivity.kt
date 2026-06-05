package com.call.w

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val btnInjectCall = findViewById<Button>(R.id.btnInjectCall)

        // طلب جعل التطبيق هو الافتراضي للمكالمات عند فتح التطبيق
        requestDefaultDialerRole()

        btnInjectCall.setOnClickListener {
            val number = etPhoneNumber.text.toString().trim()
            
            if (number == "+" || number.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة رقم صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // التحقق من صلاحيات كتابة سجل المكالمات قبل الحقن
            if (checkCallLogPermission()) {
                injectMissedCall(number)
            } else {
                requestCallLogPermission()
            }
        }
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_ID)
            }
        } else {
            val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            intent.putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

    private fun injectMissedCall(number: String) {
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, number)
            put(CallLog.Calls.DATE, System.currentTimeMillis())
            put(CallLog.Calls.DURATION, 0)
            put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE) // نوع المكالمة: فائتة
            put(CallLog.Calls.NEW, 1) // كأنها لم تُقرأ بعد
            put(CallLog.Calls.IS_READ, 0)
        }

        try {
            contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            Toast.makeText(this, "تم حقن المكالمة بنجاح!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "فشل الحقن: لا توجد صلاحيات كافية", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCallLogPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_CALL_LOG, Manifest.permission.READ_CALL_LOG),
            101
        )
    }
}

package com.call.w

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_ID = 2
    private val CHANNEL_ID = "sms_injected_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etSenderNumber = findViewById<EditText>(R.id.etSenderNumber)
        val etMessageBody = findViewById<EditText>(R.id.etMessageBody)
        val btnInjectSms = findViewById<Button>(R.id.btnInjectSms)

        // إنشاء قناة الإشعارات (مطلوب لأندرويد 8 وما فوق)
        createNotificationChannel()

        // طلب جعل التطبيق هو الافتراضي للرسائل عند الفتح
        requestDefaultSmsRole()

        btnInjectSms.setOnClickListener {
            val senderNumber = etSenderNumber.text.toString().trim()
            val messageBody = etMessageBody.text.toString().trim()
            
            if (senderNumber == "+" || senderNumber.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة رقم مرسل صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (messageBody.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة محتوى للرسالة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDefaultSmsApp()) {
                injectCustomSms(senderNumber, messageBody)
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
            // تصحيح الخطأ هنا: RoleManager بدلاً من Manager
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

    private fun injectCustomSms(sender: String, body: String) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.DATE_SENT, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0) // 0 تعني غير مقروءة لضمان بقاء الإشعار نشطاً
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }

        try {
            val uri = contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            
            if (uri != null) {
                // لإجبار النظام على تحديث واجهة تطبيق الرسائل فوراً وتقليل تأخير المزامنة
                sendBroadcast(Intent("android.intent.action.REFRESH_SMS_APP"))
                
                Toast.makeText(this, "تم حقن الرسالة بنجاح!", Toast.LENGTH_SHORT).show()
                
                // إطلاق الإشعار فوراً في نفس اللحظة
                showSmsNotification(sender, body, uri)
            } else {
                Toast.makeText(this, "فشل الحقن: النظام رفض إدراج البيانات", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // دالة بناء وإظهار الإشعار الفوري
    private fun showSmsNotification(sender: String, body: String, smsUri: Uri) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // نية (Intent) لفتح الرسالة المحقونة مباشرة داخل تطبيق الرسائل عند الضغط على الإشعار
        val intent = Intent(Intent.ACTION_VIEW, smsUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // بناء الإشعار ليشبه إشعارات النظام الرسمية للرسائل
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            // تصحيح الخطأ هنا: استخدام النقطة بدلاً من الشرطة المائلة
            .setSmallIcon(R.mipmap.ic_launcher) 
            .setContentTitle(sender)           // عنوان الإشعار: رقم أو اسم المرسل
            .setContentText(body)             // محتوى الإشعار: نص الرسالة
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // لدعم النصوص الطويلة بالكامل
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ظهور كإشعار منبثق فوري (Heads-up)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)              // يختفي عند الضغط عليه
            .setContentIntent(pendingIntent)   // تحديد الإجراء عند الضغط
            .setSound(Uri.parse("content://settings/system/notification_sound")) // نغمة الإشعار الافتراضية

        // إرسال الإشعار بمعرف فريد
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    // إنشاء قناة إشعارات ذات أولوية عالية (مطلوبة للنسخ الحديثة لتظهر منبثقة فوق الشاشة)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "رسائل واردة"
            val descriptionText = "إشعارات الرسائل النصية المحقونة"
            val importance = NotificationManager.IMPORTANCE_HIGH // لضمان ظهور الإشعار منبثقاً فوراً
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

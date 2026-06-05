package com.call.w

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class MyInCallService : InCallService() {

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        Log.d("MyInCallService", "تم إضافة مكالمة جديدة")
        
        // هنا يمكنك لاحقاً إظهار واجهة الاتصال المخصصة بك (UI) عند استقبال مكالمة واردة
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        Log.d("MyInCallService", "تم إنهاء أو إزالة المكالمة")
        
        // هنا يمكنك إخفاء واجهة الاتصال أو تنفيذ أوامر بعد إنهاء المكالمة
    }
}

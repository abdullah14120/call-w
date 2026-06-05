package com.call.w

import android.telecom.InCallService

class MyInCallService : InCallService() {
    // يمكن تركها فارغة إذا كان الهدف فقط هو الحقن، 
    // ولكن في التطبيقات الحقيقية يجب برمجة واجهة للرد على المكالمات هنا.
}

package uzh.scenere.listener

import android.content.Context
import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class SrePhoneStateListener(val context: Context?): PhoneStateListener() {
    var isPhoneCalling = false
    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
        if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
            isPhoneCalling = true
        }
        if (TelephonyManager.CALL_STATE_IDLE == state && isPhoneCalling) {
            val intent = context?.packageManager?.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context?.startActivity(intent)

            isPhoneCalling = false
        }
    }
}
package uzh.scenere.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import uzh.scenere.const.Constants

class SrePhoneReceiver(private val handlePhoneCall: (Context?, String) -> Boolean) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        registerListener(context)
        if (intent?.action == Constants.PHONE_STATE) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            } else if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val telephoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!handlePhoneCall.invoke(context, telephoneNumber)) {

                }
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {

            }
        } else {
            //NOP
        }
    }

    fun registerListener(context: Context?) {
        val phoneListener = SrePhoneStateListener(context)
        val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
}
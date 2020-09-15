package uzh.scenere.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import uzh.scenere.const.Constants

class SreSmsReceiver(private val handleSms: (String, String) -> Boolean) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Constants.SMS_RECEIVED) {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus")
                if (pdus is Array<*>) {
                    for (obj in pdus) {
                        val currentSMS = getIncomingMessage(obj, bundle)

                        val senderNo = currentSMS.displayOriginatingAddress
                        val message = currentSMS.displayMessageBody
                        handleSms.invoke(senderNo,message)
                    }
                    this.abortBroadcast()
                    // End of loop
                }
            }
        }
    }

    private fun getIncomingMessage(obj: Any?, bundle: Bundle): SmsMessage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val format = bundle.getString ("format")
            return SmsMessage.createFromPdu(obj as ByteArray, format)
        } else {
            return SmsMessage.createFromPdu(obj as ByteArray)
        }
    }

}
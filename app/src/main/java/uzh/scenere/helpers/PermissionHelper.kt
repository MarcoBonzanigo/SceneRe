package uzh.scenere.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_ALL
import uzh.scenere.helpers.PermissionHelper.Companion.PermissionGroups.*
import java.util.*

class PermissionHelper private constructor () {

    companion object {
        enum class PermissionGroups(val label: String, val title: Int, val description: Int) {
            NETWORK("Mobile Network", R.string.permission_description_network_title,R.string.permission_description_network),
            STORAGE("Storage", R.string.permission_description_storage_title,R.string.permission_description_storage),
            AUDIO("Audio", R.string.permission_description_audio_title,R.string.permission_description_audio),
            BLUETOOTH("Bluetooth", R.string.permission_description_bluetooth_title,R.string.permission_description_bluetooth),
            WIFI("Wi-Fi", R.string.permission_description_wifi_title,R.string.permission_description_wifi),
            NFC("NFC", R.string.permission_description_nfc_title,R.string.permission_description_nfc),
            SMS("SMS", R.string.permission_description_sms_title,R.string.permission_description_sms),
            GPS("GPS", R.string.permission_description_gps_title,R.string.permission_description_gps),
            TELEPHONY("Telephony", R.string.permission_description_telephony_title,R.string.permission_description_telephony),
            DEVICE("Device", R.string.permission_description_device_title,R.string.permission_description_device);

            fun getDescription(context: Context): String {
                return context.getString(description)
            }
            fun getTitle(context: Context): String {
                return context.getString(title)
            }
        }
        private val permissionHolder: HashMap<PermissionGroups, Array<String>> = hashMapOf(
                NETWORK to arrayOf(Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.CHANGE_NETWORK_STATE),
                STORAGE to arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
                AUDIO to arrayOf(Manifest.permission.RECORD_AUDIO),
                BLUETOOTH to arrayOf(Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN),
                WIFI to arrayOf(Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_COARSE_LOCATION),
                NFC to arrayOf(Manifest.permission.NFC),
                SMS to arrayOf(Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS),
                GPS to arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),
                TELEPHONY to arrayOf(Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE),
                DEVICE   to arrayOf(Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE)
        )
        fun getRequiredPermissions(context: Context): ArrayList<PermissionGroups>{
            val permissions = ArrayList<PermissionGroups>()
            permissionHolder.forEach { (key,value) -> if (!check(context,*value)){
                permissions.add(key)
            } }
            return permissions
        }
        fun check(context: Context, vararg permissions: String): Boolean{
            for (permission in permissions){
                val check = (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
                if (!check) {
                    return false
                }
            }
            return true
        }
        fun check(context: Context, permissionGroup: PermissionGroups): Boolean{
            val permissions = permissionHolder[permissionGroup] ?: return false
            return check(context, *permissions)
        }
        fun request(activity: Activity, permissionGroup: PermissionGroups) {
            val permissions = permissionHolder[permissionGroup] ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(permissions,PERMISSION_REQUEST_ALL)
            }else{
                ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_ALL)
            }
        }
    }
}
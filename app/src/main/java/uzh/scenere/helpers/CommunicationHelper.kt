package uzh.scenere.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import uzh.scenere.const.Constants


class CommunicationHelper private constructor () {

    companion object {
        enum class Communications(val label: String) {
            WIFI("Wifi"),
            BLUETOOTH("Bluetooth"),
            NETWORK("Mobile Network (Externally Managed)"),
            NFC("NFC (Externally Managed)"),
            GPS("GPS")
        }

        private const val listenerTag: String = "SRE-GPS-Listener"

        fun check(activity: Activity, communications: Communications): Boolean{
            when (communications){
                Communications.GPS -> {
                    return checkGpsState(activity)
                }
                Communications.NETWORK -> {
                    val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    return try {
                        val cmClass = Class.forName(cm.javaClass.name)
                        val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
                        method.isAccessible = true
                        !isInAirplaneMode(activity) && method.invoke(cm) as Boolean
                    } catch (e: Exception) {
                        false
                    }
                }
                Communications.WIFI -> {
                    val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    return wifiManager.isWifiEnabled
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    return bluetoothAdapter?.isEnabled ?: false
                }
                Communications.NFC -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
                    return nfcAdapter?.isEnabled ?: false
                }
                else -> return false
            }
        }

        fun supports(context: Context, communications: Communications): Boolean{
            return when (communications){
                Communications.NFC -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                    nfcAdapter != null
                }
                else -> true
            }
        }

        fun supportsNfcAndEnabled(context: Context, communications: Communications): Boolean{
            return when (communications){
                Communications.NFC -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context) ?: return false
                    return nfcAdapter.isEnabled
                }
                else -> true
            }
        }

        fun requestBeamActivation(context: Context, open: Boolean = false): Boolean {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context) ?: return false
            if (open && !nfcAdapter.isNdefPushEnabled){
                startActivity(context,Intent(Settings.ACTION_NFCSHARING_SETTINGS))
            }
            return nfcAdapter.isNdefPushEnabled
        }

        private fun checkGpsState(activity: Activity): Boolean {
            if (!PermissionHelper.check(activity,PermissionHelper.Companion.PermissionGroups.GPS)){
                return false
            }
            var locationMode = 0
            try {
                locationMode = Settings.Secure.getInt(activity.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                return false
            }
            if (locationMode > Settings.Secure.LOCATION_MODE_OFF){
                return true
            }
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 30 * 1000
            locationRequest.fastestInterval = 5 * 1000
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
            val locationSettingsResponse = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())
            locationSettingsResponse.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                } catch (exception: ApiException) {
                    when (exception.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvable = exception as ResolvableApiException
                            resolvable.startResolutionForResult(activity, Constants.PERMISSION_REQUEST_GPS)
                        } catch (e: java.lang.Exception){
                            //NOP
                        }
                    }
                }
            }
            return true
        }

        fun enableLocationForWifiDiscovery(activity: Activity): Boolean{
            var locationMode = 0
            try {
                locationMode = Settings.Secure.getInt(activity.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                return false
            }
            if (locationMode > Settings.Secure.LOCATION_MODE_OFF){
                return true
            }
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            locationRequest.interval = 30 * 1000
            locationRequest.fastestInterval = 5 * 1000
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
            val locationSettingsResponse = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())
            locationSettingsResponse.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                } catch (exception: ApiException) {
                    when (exception.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvable = exception as ResolvableApiException
                            resolvable.startResolutionForResult(activity, Constants.PERMISSION_REQUEST_GPS)
                        } catch (e: java.lang.Exception){
                            //NOP
                        }
                    }
                }
            }
            return true
        }

        fun enable(activity: Activity, communications: Communications): Boolean{
            if (check(activity,communications)) return true
            when (communications){
                Communications.GPS -> {
                    if (checkGpsState(activity)){
                        registerGpsListener(activity)
                        return true
                    }
                }
                Communications.NETWORK -> {
                    startActivity(activity,Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.WIFI -> {
                    val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifiManager.isWifiEnabled = true
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
                    bluetoothAdapter.enable()
                }
                Communications.NFC -> {
                    startActivity(activity,Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
            }
            return true
        }

        fun disable(activity: Activity, communications: Communications): Boolean{
            if (!check(activity,communications)) return false
            when (communications){
                Communications.GPS -> {
                    unregisterGpsListener(activity)
                }
                Communications.NETWORK -> {
                    startActivity(activity,Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.WIFI -> {
                    val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifiManager.isWifiEnabled = false
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    bluetoothAdapter?.disable()
                }
                Communications.NFC -> {
                    startActivity(activity,Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
            }
            return false
        }

        fun startActivity(context: Context, intent: Intent){
            try{
                startActivity(context,intent)
            }catch (t: Throwable){
                //NOP
            }
        }
        
        
        fun toggle(context: Activity, communications: Communications): Boolean{
            return if (check(context,communications)) disable(context, communications) else enable(context,communications)
        }

        fun getCommunications(): Array<Communications> {
            return Communications.values()
        }

        enum class WiFiStrength(val strength: Int) {
            EXCELLENT(5),GOOD(4),FAIR(3),WEAK(2),FAINT(1);

            companion object {
                fun getStringValues(): ArrayList<String>{
                    val list = ArrayList<String>()
                    for (value in values()){
                        list.add(value.toString())
                    }
                    return list
                }
            }
        }

        fun getWifiStrength(dB: Int): WiFiStrength{
            return if (dB >= -50){
                WiFiStrength.EXCELLENT
            }else if (dB < -50 && dB >= -60){
                WiFiStrength.GOOD
            }else if (dB < -60 && dB >= -70){
                WiFiStrength.FAIR
            }else if (dB < -70 && dB >= -80){
                WiFiStrength.WEAK
            }else
                WiFiStrength.FAINT
        }

        @SuppressLint("ObsoleteSdkInt")
        fun isInAirplaneMode(context: Context): Boolean{
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.System.getInt(context.contentResolver,
                        Settings.System.AIRPLANE_MODE_ON, 0) != 0
            } else {
                Settings.Global.getInt(context.contentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            }
        }

        @SuppressLint("MissingPermission")
        fun registerGpsListener(activity: Activity): Boolean{
            if (!PermissionHelper.check(activity,PermissionHelper.Companion.PermissionGroups.GPS)){
                return false
            }
            if (!checkGpsState(activity)){
                return false
            }
            if (SreLocationListener.exists()){
                if (SreLocationListener.get().isBoundToThisActivity(activity)){
                    return true
                }else{
                    SreLocationListener.get().unbind()
                }
            }
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, SreLocationListener.get().setActivity(activity))
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, SreLocationListener.get().setActivity(activity))
            return true
        }

        fun unregisterGpsListener(activity: Activity): Boolean{
            if (SreLocationListener.exists()){
                val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.removeUpdates(SreLocationListener.get())
                SreLocationListener.destroy()
            }
            return false
        }

        fun getMapIntent(): Intent?{
            if (!SreLocationListener.exists()){
                return null
            }
            return SreLocationListener.get().getMapIntent()
        }
        fun getMapIntent(lat: Double, lon: Double): Intent{
            return Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon"))
        }

        class SreLocationListener private constructor(): LocationListener {
            private var longitude: Double? = null
            private var latitude: Double? = null
            private var activity: Activity? = null

            fun getLatitudeLongitude(): Pair<Double?, Double?> {
                return Pair(latitude, longitude)
            }

            fun setActivity(activity: Activity): SreLocationListener {
                this.activity = activity
                return this
            }

            fun isBoundToThisActivity(activity: Activity): Boolean {
                if (this.activity != null && this.activity == activity){
                    return true
                }
                return false
            }

            fun unbind(){
                if (activity != null){
                    unregisterGpsListener(activity!!)
                }
                activity = null
            }

            companion object {

                // Volatile: writes to this field are immediately made visible to other threads.
                @Volatile private var instance : SreLocationListener? = null

                fun exists(): Boolean {return instance != null}

                fun get(): SreLocationListener {
                    return when {
                        instance != null -> instance!!
                        else -> synchronized(this) {
                            if (instance == null) {
                                instance = SreLocationListener()
                            }
                            Log.d(listenerTag, "Listener created.")
                            instance!!
                        }
                    }
                }

                fun destroy(): SreLocationListener? {
                    instance = null
                    Log.d(listenerTag, "Listener destroyed.")
                    return instance
                }
            }

            override fun onProviderEnabled(provider: String?) {
                Log.d(listenerTag, "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String?) {
                Log.d(listenerTag, "Provider disabled: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(listenerTag, "Status changed, provider: $provider, status: $status")
            }

            override fun onLocationChanged(location: Location?) {
                longitude = location?.longitude
                latitude = location?.latitude
                Log.d(listenerTag, "Lat: $latitude, Lon: $longitude")
            }

            fun getMapIntent(): Intent?{
                if (longitude == null || latitude == null){
                    return null
                }
                return Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude"))
            }
        }

        fun reconnectBluetoothDevice(device: BluetoothDevice): Boolean{
            try{
                val declaredMethod = device.javaClass.getDeclaredMethod("removeBond")
                declaredMethod.invoke(device)
            }catch (e: java.lang.Exception){
                return false
            }
            return device.createBond()
        }
    }
}
package uzh.scenere.activities

import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_picker.*
import uzh.scenere.helpers.CommunicationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.LAST_KNOWN_LOCATION
import uzh.scenere.const.Constants.Companion.LAST_USED_LOCATION
import uzh.scenere.datastructures.SreLatLng
import uzh.scenere.helpers.DataHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.toSreLatLng
import uzh.scenere.views.SreButton


class PickerActivity: AbstractBaseActivity (), OnMapReadyCallback {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_picker
    }

    override fun getConfiguredRootLayout(): ViewGroup? {
        return picker_root
    }

    override fun onMapReady(map: GoogleMap?) {
        picker_progress.visibility = INVISIBLE
        mapView?.visibility = VISIBLE
        this.map = map
        zoomMapLocation()
        this.map?.setOnMapClickListener {
            marker?.remove()
            marker = map?.addMarker(MarkerOptions().position(it).title(getString(R.string.picker_position)))
            currentLocation = it
            copyButton?.isEnabled = true
        }
    }

    private fun zoomMapLocation() {
        this.map?.setMinZoomPreference(12f)
        this.map?.moveCamera(CameraUpdateFactory.newLatLng(current))
    }

    private var mapView: MapView? = null
    private var map: GoogleMap? = null
    private var currentLocation: LatLng? = null
    private var retry = 0
    private var zurich: LatLng = LatLng(47.3773821,8.5397894) //Zurich
    private var current = zurich
    private var marker: Marker? = null
    private var copyButton: SreButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createButtons()
        startMap(savedInstanceState)
    }

    private fun createButtons() {
        val resetButton = SreButton(applicationContext, picker_reset_holder, getString(R.string.picker_reset))
        val cancelButton = SreButton(applicationContext, picker_button_holder, getString(R.string.picker_cancel))
        copyButton = SreButton(applicationContext, picker_button_holder, getString(R.string.picker_copy_to_clipboard))
        resetButton.setWeight(1f,true)
        copyButton?.setWeight(1f,false)
        cancelButton.setWeight(1f,false)
        resetButton.setExecutable {
            current = zurich
            marker?.remove()
            marker = null
            currentLocation = null
            DatabaseHelper.getInstance(applicationContext).write(LAST_KNOWN_LOCATION, DataHelper.toByteArray(current.toSreLatLng()))
            DatabaseHelper.getInstance(applicationContext).write(LAST_USED_LOCATION, DataHelper.toByteArray(current.toSreLatLng()))
            zoomMapLocation()
            copyButton?.isEnabled = false
        }
        copyButton?.setExecutable {
            if (currentLocation != null){
                writeLocation()
                onBackPressed()
            }else{
                notify(getString(R.string.picker_no_location_title),getString(R.string.picker_no_location_content))
            }
        }
        cancelButton.setExecutable {
            onBackPressed()
        }
        copyButton?.isEnabled = false
        cancelButton.setMargin(1)
        copyButton?.setMargin(1)
        picker_button_holder.addView(cancelButton)
        picker_button_holder.addView(copyButton)
        picker_reset_holder.addView(resetButton)
    }

    private fun startMap(savedInstanceState: Bundle?){
        if (retry < 10 && (!CommunicationHelper.check(this,CommunicationHelper.Companion.Communications.GPS))){
            CommunicationHelper.enable(this,CommunicationHelper.Companion.Communications.GPS)
            Handler().postDelayed({startMap(savedInstanceState)}, Constants.HALF_SEC_MS)
            retry++
        }else{
            readLocation()
            mapView = picker_map
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)
        }
    }

    private fun readLocation() {
        val (lat, lon) = CommunicationHelper.Companion.SreLocationListener.get().getLatitudeLongitude()
        if (lat != null && lon != null) {
            current = LatLng(lat, lon)
        } else {
            val lastKnown = DatabaseHelper.getInstance(applicationContext).read(LAST_KNOWN_LOCATION, ByteArray::class)
            if (lastKnown.isEmpty()) {
                val lastKnownLatLng = DataHelper.toObject(lastKnown, SreLatLng::class)
                if (lastKnownLatLng != null) {
                    current = lastKnownLatLng.toLatLng()
                } else {
                    val lastUsed = DatabaseHelper.getInstance(applicationContext).read(LAST_USED_LOCATION, ByteArray::class)
                    val lastUsedLatLng = DataHelper.toObject(lastUsed, SreLatLng::class)
                    if (lastUsedLatLng != null) {
                        current = lastUsedLatLng.toLatLng()
                    }
                }
            }
        }
    }

    private fun writeLocation(){
        if (currentLocation != null){
            val sreLatLng = currentLocation!!.toSreLatLng()
            copyToClipboard(sreLatLng.toLatLngStr())
            notify(getString(R.string.picker_location_picked))
            DatabaseHelper.getInstance(applicationContext).write(LAST_USED_LOCATION, DataHelper.toByteArray(sreLatLng))
            val (lat, lon) = CommunicationHelper.Companion.SreLocationListener.get().getLatitudeLongitude()
            if (lat != null && lon != null) {
                DatabaseHelper.getInstance(applicationContext).write(LAST_KNOWN_LOCATION, DataHelper.toByteArray(SreLatLng(lat,lon)))
            }
        }else{
            notify(getString(R.string.picker_no_location_copied))
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }


    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (outState != null){
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        mapView?.onLowMemory()
        super.onLowMemory()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }
}
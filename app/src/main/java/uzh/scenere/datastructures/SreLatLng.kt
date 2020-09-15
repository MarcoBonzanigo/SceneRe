package uzh.scenere.datastructures

import com.google.android.gms.maps.model.LatLng
import uzh.scenere.helpers.NumberHelper
import java.io.Serializable

class SreLatLng(private val latitude: Double,private val longitude: Double): Serializable {

    constructor(latLng: LatLng): this(latLng.latitude,latLng.longitude)

    fun toLatLng() = LatLng(latitude,longitude)

    fun toLatLngStr(): String {
        val latSplit = latitude.toString().split(".")
        val lonSplit = longitude.toString().split(".")
        if (latSplit.size == 2 && lonSplit.size == 2){
            return "${latSplit[0]}.${latSplit[1].substring(0,NumberHelper.capAtHigh(6,latSplit[1].length-1))}" +
                    ",${lonSplit[0]}.${lonSplit[1].substring(0, NumberHelper.capAtHigh(6, lonSplit[1].length - 1))}"

        }
        return "${latitude.toFloat()},${longitude.toFloat()}"
    }
}
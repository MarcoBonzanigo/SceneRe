package uzh.scenere.datamodel.triggers.communication

import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SEMI_COLON
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ICommunicationTrigger
import uzh.scenere.helpers.NumberHelper
import java.util.*

class GpsTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var gpsData: String? = null

    fun withText(text: String?): GpsTrigger {
        this.text = text
        return this
    }

    fun withGpsData(gpsData: String?): GpsTrigger {
        this.gpsData = gpsData
        return this
    }

    fun withGpsData(radius: String, latitude: String, longitude: String): GpsTrigger {
        this.gpsData = "$radius;$latitude;$longitude"
        return this
    }

    fun getRadius(): Long{
        if (gpsData != null){
            val split = gpsData!!.split(SEMI_COLON)
            if (split.size == 3){
                return NumberHelper.safeToNumber(split[0],0)
            }
        }
        return 0L
    }


    fun getLatitude(): String{
        if (gpsData != null){
            val split = gpsData!!.split(SEMI_COLON)
            if (split.size == 3){
                return split[1]
            }
        }
        return NOTHING
    }

    fun getLongitude(): String{
        if (gpsData != null){
            val split = gpsData!!.split(SEMI_COLON)
            if (split.size == 3){
                return split[2]
            }
        }
        return NOTHING
    }

    fun getLatitudeDouble(): Double{
        return getLatitude().toDouble()
    }

    fun getLongitudeDouble(): Double{
        return getLongitude().toDouble()
    }

    fun getLatitudeLongitude(): String{
        return "${getLatitude()},${getLongitude()}"
    }

}
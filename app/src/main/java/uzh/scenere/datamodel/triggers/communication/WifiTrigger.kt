package uzh.scenere.datamodel.triggers.communication

import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SEMI_COLON
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ICommunicationTrigger
import java.util.*

class WifiTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var ssidAndStrength: String? = null

    fun withText(text: String?): WifiTrigger {
        this.text = text
        return this
    }

    fun withSsidAndStrength(ssid: String, strength: String): WifiTrigger {
        this.ssidAndStrength = "$ssid$SEMI_COLON$strength"
        return this
    }

    fun withSsidAndStrength(ssidAndStrength: String): WifiTrigger {
        this.ssidAndStrength = ssidAndStrength
        return this
    }

    fun getSsid(): String{
        val split = ssidAndStrength?.split(SEMI_COLON)
        if (split != null && split.size > 1){
            return ssidAndStrength!!.replace(SEMI_COLON.plus(getStrength()), NOTHING)
        }
        return NOTHING
    }

    fun getStrength(): String{
        val split = ssidAndStrength?.split(SEMI_COLON)
        if (split != null && split.size > 1){
            return split[split.size-1]
        }
        return NOTHING
    }

}
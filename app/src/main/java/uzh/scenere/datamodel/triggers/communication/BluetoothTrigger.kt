package uzh.scenere.datamodel.triggers.communication

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ICommunicationTrigger
import java.util.*

class BluetoothTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var deviceId: String? = null

    fun withText(text: String?): BluetoothTrigger {
        this.text = text
        return this
    }

    fun withDeviceId(deviceId: String?): BluetoothTrigger {
        this.deviceId = deviceId
        return this
    }
}